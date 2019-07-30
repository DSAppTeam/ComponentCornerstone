package com.plugin.module


import com.plugin.module.alone.AloneExtension
import com.plugin.module.alone.AlonePlugin
import com.plugin.module.mis.extension.Dependencies
import com.plugin.module.mis.extension.MisExtension
import com.plugin.module.mis.extension.OnMisExtensionListener
import com.plugin.module.mis.extension.Publication
import com.plugin.module.mis.extension.PublicationManager
import com.plugin.module.mis.utils.MisUtil
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project

class ModulePlugin implements Plugin<Project> {

    static File misDir
    static MisExtension misExtension
    static AloneExtension alone
    static String androidJarPath
    static PublicationManager publicationManager

    @Override
    void apply(Project project) {

        boolean isRootProject = project == project.rootProject
        if (isRootProject) {
            initPlugin(project)
        } else {
            new AlonePlugin().apply(project)

            if (!MisUtil.hasAndroidPlugin(project)) {
                throw new GradleException("The android or android-library plugin must be applied to the project.")
            }

            project.dependencies.metaClass.misPublication { Object value ->
                String[] gav = MisUtil.filterGAV(value)
                return getPublication(gav[0], gav[1])
            }

            List<Publication> publications = publicationManager.getPublicationByProject(project)
            project.dependencies {
                publications.each {
                    implementation getPublication(it.groupId, it.artifactId)
                }
            }
            if (project.gradle.startParameter.taskNames.isEmpty()) {
                publications.each {
                    addPublicationDependencies(it)
                }
            }

            project.afterEvaluate {
                MisUtil.addMisSourceSets(project)
                List<Publication> publicationList = publicationManager.getPublicationByProject(project)
                List<Publication> publicationPublishList = new ArrayList<>()
                publicationList.each {
                    if (it.version != null) {
                        publicationPublishList.add(it)
                    }
                }

                if (publicationPublishList.size() > 0) {
                    project.plugins.apply('maven-publish')
                    def publishing = project.extensions.getByName('publishing')
                    if (misExtension.configure != null) {
                        publishing.repositories misExtension.configure
                    }

                    publicationPublishList.each {
                        createPublishTask(it)
                    }
                }
            }

        }

    }

    private void initPlugin(Project project) {
        misDir = new File(project.projectDir, '.gradle/misExtension')
        if (!misDir.exists()) {
            misDir.mkdirs()
        }
        project.gradle.getStartParameter().taskNames.each {
            if (it == 'clean') {
                if (!misDir.deleteDir()) {
                    throw new RuntimeException("unable to delete dir " + misDir.absolutePath)
                }
                misDir.mkdirs()
            }
        }
        project.repositories {
            flatDir {
                dirs misDir.absolutePath
            }
        }
        publicationManager = PublicationManager.getInstance()
        publicationManager.loadManifest(project, misDir)

        /**
         * 构造器参数
         */
        misExtension = project.getExtensions().create("mis", MisExtension, new OnMisExtensionListener() {
            @Override
            void onPublicationAdded(Project childProject, Publication publication) {
                initPublication(childProject, publication)
                publicationManager.addDependencyGraph(publication)
            }
        })
        alone = project.getExtensions().create("runalone", AloneExtension)

        project.allprojects.each {
            if (it == project) return
            Project childProject = it
            childProject.repositories {
                flatDir {
                    dirs misDir.absolutePath

                }
            }
        }

        //评估完成之后
        project.afterEvaluate {
            androidJarPath = MisUtil.getAndroidJarPath(project, misExtension.compileSdkVersion)

            //扩展misPublication
            Dependencies.metaClass.misPublication { String value ->
                String[] gav = MisUtil.filterGAV(value)
                return 'misExtension-' + gav[0] + ':' + gav[1] + ':' + gav[2]
            }

            //如果存在module.gradle，则引用
            project.allprojects.each {
                if (it == project) return
                Project childProject = it
                def misScript = new File(childProject.projectDir, 'module.gradle')
                if (misScript.exists()) {
                    misExtension.childProject = childProject
                    project.apply from: misScript
                }
            }

            List<String> topSort = publicationManager.dependencyGraph.topSort()
            Collections.reverse(topSort)
            topSort.each {
                Publication publication = publicationManager.publicationDependencies.get(it)
                if (publication == null) {
                    return
                }

                Project childProject = project.findProject(publication.project)

                filterPublicationDependencies(publication)

                if (publication.version != null) {
                    handleMavenJar(childProject, publication)
                } else {
                    handleLocalJar(childProject, publication)
                }
                publicationManager.hitPublication(publication)
            }

        }
    }


    def filterPublicationDependencies(Publication publication) {
        if (publication.dependencies != null) {
            if (publication.dependencies.compileOnly != null) {
                List<Object> compileOnly = new ArrayList<>()
                publication.dependencies.compileOnly.each {
                    if (it instanceof String && it.startsWith('misExtension-')) {
                        String[] gav = MisUtil.filterGAV(it.replace('misExtension-', ''))
                        Publication existPublication = publicationManager.getPublicationByKey(gav[0] + '-' + gav[1])
                        if (existPublication != null) {
                            if (existPublication.useLocal) {
                                compileOnly.add(':misExtension-' + existPublication.groupId + '-' + existPublication.artifactId + ':')
                            } else {
                                compileOnly.add(existPublication.groupId + ':' + existPublication.artifactId + ':' + existPublication.version)
                            }
                        }
                    } else {
                        compileOnly.add(it)
                    }
                }
                publication.dependencies.compileOnly = compileOnly
            }
            if (publication.dependencies.implementation != null) {
                List<Object> implementation = new ArrayList<>()
                publication.dependencies.implementation.each {
                    if (it instanceof String && it.startsWith('misExtension-')) {
                        String[] gav = MisUtil.filterGAV(it.replace('misExtension-', ''))
                        Publication existPublication = publicationManager.getPublicationByKey(gav[0] + '-' + gav[1])
                        if (existPublication != null) {
                            if (existPublication.useLocal) {
                                implementation.add(':misExtension-' + existPublication.groupId + '-' + existPublication.artifactId + ':')
                            } else {
                                implementation.add(existPublication.groupId + ':' + existPublication.artifactId + ':' + existPublication.version)
                            }
                        }
                    } else {
                        implementation.add(it)
                    }
                }
                publication.dependencies.implementation = implementation
            }
        }
    }

    def handleLocalJar(Project project, Publication publication) {
        File target = new File(misDir, 'misExtension-' + publication.groupId + '-' + publication.artifactId + '.jar')

        if (publication.invalid) {
            publicationManager.addPublication(publication)
            if (target.exists()) {
                target.delete()
            }
            return
        }

        if (target.exists()) {
            boolean hasModifiedSource = publicationManager.hasModified(publication)
            if (!hasModifiedSource) {
                publication.invalid = false
                publication.useLocal = true
                publicationManager.addPublication(publication)
                return
            }
        }

        File releaseJar = JarUtil.packJavaSourceJar(project, publication, androidJarPath, misExtension.compileOptions, true)
        if (releaseJar == null) {
            publication.invalid = true
            publicationManager.addPublication(publication)
            if (target.exists()) {
                target.delete()
            }
            return
        }

        MisUtil.copyFile(releaseJar, target)
        publication.invalid = false
        publication.useLocal = true
        publicationManager.addPublication(publication)
    }

    def handleMavenJar(Project project, Publication publication) {
        File target = new File(misDir, 'misExtension-' + publication.groupId + '-' + publication.artifactId + '.jar')
        if (publication.invalid) {
            publicationManager.addPublication(publication)
            if (target.exists()) {
                target.delete()
            }
            return
        }

        boolean hasModifiedSource = publicationManager.hasModified(publication)

        if (target.exists()) {
            if (hasModifiedSource) {
                def releaseJar = JarUtil.packJavaSourceJar(project, publication, androidJarPath, misExtension.compileOptions, true)
                if (releaseJar == null) {
                    publication.invalid = true
                    publicationManager.addPublication(publication)
                    if (target.exists()) {
                        target.delete()
                    }
                    return
                }
                MisUtil.copyFile(releaseJar, target)
            }
            publication.invalid = false
            publication.useLocal = true
            publicationManager.addPublication(publication)
        } else if (!hasModifiedSource) {
            Publication lastPublication = publicationManager.getPublication(publication.groupId, publication.artifactId)
            if (lastPublication.version != publication.version) {
                publication.versionNew = publication.version
                publication.version = lastPublication.version
            }
            publication.invalid = false
            publication.useLocal = false
            publicationManager.addPublication(publication)
            return
        } else {
            def releaseJar = JarUtil.packJavaSourceJar(project, publication, androidJarPath, misExtension.compileOptions, false)
            if (releaseJar == null) {
                publication.invalid = true
                publicationManager.addPublication(publication)
                if (target.exists()) {
                    target.delete()
                }
                return
            }

            Publication lastPublication = publicationManager.getPublication(publication.groupId, publication.artifactId)
            if (lastPublication == null) {
                lastPublication = publication
            }
            boolean equals = JarUtil.compareMavenJar(project, lastPublication, releaseJar.absolutePath)
            if (equals) {
                if (target.exists()) {
                    target.delete()
                }
                publication.useLocal = false
            } else {
                releaseJar = JarUtil.packJavaSourceJar(project, publication, androidJarPath, misExtension.compileOptions, true)
                MisUtil.copyFile(releaseJar, target)
                publication.useLocal = true
            }
            publication.invalid = false
            publicationManager.addPublication(publication)
        }
    }

}
