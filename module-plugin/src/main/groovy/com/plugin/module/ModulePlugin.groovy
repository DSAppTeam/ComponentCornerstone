package com.plugin.module

import com.plugin.module.extension.AloneExtension
import com.plugin.module.extension.module.Dependencies
import com.plugin.module.extension.MisExtension
import com.plugin.module.listener.OnMisExtensionListener
import com.plugin.module.publication.Publication
import com.plugin.module.publication.PublicationManager
import com.plugin.module.utils.MisUtil
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.maven.MavenPublication

/**
 *   ./gradlew --no-daemon ModulePlugin  -Dorg.gradle.debug=true
 */
class ModulePlugin implements Plugin<Project> {

    static File misDir
    static MisExtension misExtension
    static AloneExtension alone
    static String androidJarPath
    static PublicationManager publicationManager

    @Override
    void apply(Project project) {
        if (project == project.rootProject) {
            handleRootProject(project)
        } else {
            handleProject(project)
        }
    }

    /**
     * 处理非rootproject
     * @param project
     */
    private void handleProject(Project project) {

        new AlonePlugin().apply(project)

        if (!MisUtil.hasAndroidPlugin(project)) {
            throw new GradleException("The android or android-library plugin must be applied to the project.")
        }

        //解析 misPublication(xxxx) 中的字符串转化为 maven 格式
        project.dependencies.metaClass.misPublication { Object value ->
            String[] gav = MisUtil.filterGAV(value)
            return getPublication(gav[0], gav[1])
        }

        //获取当前project 可能依赖scope 使用的 misPublication(xxxx)，转化为 implementation
        List<Publication> publications = publicationManager.getPublicationByProject(project)
        project.dependencies {
            publications.each {
                implementation getPublication(it.groupId, it.artifactId)
            }
        }

        //添加每一个 publication 的依赖
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

    /**
     * 处理root peoject
     * @param project
     */
    private void handleRootProject(Project project) {
        //重置 .gradle/mis 文件
        misDir = new File(project.projectDir, '.gradle/mis')
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

        //读取 manifest
        publicationManager = PublicationManager.getInstance()
        publicationManager.loadManifest(project, misDir)
        misExtension = project.getExtensions().create("mis", MisExtension, new OnMisExtensionListener() {
            @Override
            void onPublicationAdded(Project childProject, Publication publication) {
                initPublication(childProject, publication)
                publicationManager.addDependencyGraph(publication)
            }
        })
        alone = project.getExtensions().create("runalone", AloneExtension)


        //如果子project不存在module.gradle,则报错
        project.allprojects.each {
            if (it == project) return
            Project childProject = it
            if(childProject.pluginManager.hasPlugin('com.android.module')){
                def misScript = new File(childProject.projectDir, 'module.gradle')
                if (misScript.exists()) {
                    misExtension.childProject = childProject
                    project.apply from: misScript
                    //添加依赖路径
                    childProject.repositories {
                        flatDir {
                            dirs misDir.absolutePath
                        }
                    }
                } else {
                    throw new RuntimeException("找不到 " + project.name + "下的module.gradle ！")
                }
            }
        }


        //评估完成之后
        project.afterEvaluate {
            androidJarPath = MisUtil.getAndroidJarPath(project, misExtension.compileSdkVersion)

            //扩展misPublication
            Dependencies.metaClass.misPublication { String value ->
                String[] gav = MisUtil.filterGAV(value)
                return 'mis-' + gav[0] + ':' + gav[1] + ':' + gav[2]
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

        FileUtil.copyFile(releaseJar, target)
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
                FileUtil.copyFile(releaseJar, target)
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
                FileUtil.copyFile(releaseJar, target)
                publication.useLocal = true
            }
            publication.invalid = false
            publicationManager.addPublication(publication)
        }
    }

    def getPublication(String groupId, String artifactId) {
        Publication publication = publicationManager.getPublication(groupId, artifactId)
        if (publication != null) {
            if (publication.invalid) {
                return []
            } else if (publication.useLocal) {
                return ':mis-' + publication.groupId + '-' + publication.artifactId + ':'
            } else {
                return publication.groupId + ':' + publication.artifactId + ':' + publication.version
            }
        } else {
            return []
        }
    }

    void addPublicationDependencies(Publication publication) {
        if (publication.dependencies == null) return
        project.dependencies {
            if (publication.dependencies.compileOnly != null) {
                publication.dependencies.compileOnly.each {
                    compileOnly it
                }
            }
            if (publication.dependencies.implementation != null) {
                publication.dependencies.implementation.each {
                    implementation it
                }
            }
        }
    }


    void createPublishTask(Publication publication) {
        def taskName = 'compileMis[' + publication.artifactId + ']Source'
        def compileTask = project.getTasks().findByName(taskName)
        if (compileTask == null) {
            compileTask = project.getTasks().create(taskName, CompileMisTask.class)
            compileTask.publication = publication
            compileTask.dependsOn 'clean'
        }

        def publicationName = 'Mis[' + publication.artifactId + ']'
        String publishTaskNamePrefix = "publish${publicationName}PublicationTo"
        project.tasks.whenTaskAdded {
            if (it.name.startsWith(publishTaskNamePrefix)) {
                it.dependsOn compileTask
                it.doLast {
                    new File(misDir, 'mis-' + publication.groupId + '-' + publication.artifactId + '.jar').delete()
                }
            }
        }
        createPublishingPublication(publication, publicationName)
    }

    void createPublishingPublication(Publication publication, String publicationName) {
        def publishing = project.extensions.getByName('publishing')
        MavenPublication mavenPublication = publishing.publications.maybeCreate(publicationName, MavenPublication)
        mavenPublication.groupId = publication.groupId
        mavenPublication.artifactId = publication.artifactId
        mavenPublication.version = publication.versionNew != null ? publication.versionNew : publication.version
        mavenPublication.pom.packaging = 'jar'

        def outputsDir = new File(publication.buildDir, "outputs")
        mavenPublication.artifact source: new File(outputsDir, "classes.jar")
        mavenPublication.artifact source: new File(outputsDir, "classes-source.jar"), classifier: 'sources'

        if (publication.dependencies != null) {
            mavenPublication.pom.withXml {
                def dependenciesNode = asNode().appendNode('dependencies')
                if (publication.dependencies.implementation != null) {
                    publication.dependencies.implementation.each {
                        def gav = it.split(":")
                        if (gav[1].startsWith('mis-')) {
                            Publication dependencyPublication = publicationManager.getPublicationByKey(gav[1].replace('mis-', ''))
                            if (dependencyPublication.useLocal) {
                                throw new RuntimeException("mis publication [$dependencyPublication.groupId:$dependencyPublication.artifactId] has not publish yet.")
                            }
                        }
                        def dependencyNode = dependenciesNode.appendNode('dependency')
                        dependencyNode.appendNode('groupId', gav[0])
                        dependencyNode.appendNode('artifactId', gav[1])
                        dependencyNode.appendNode('version', gav[2])
                        dependencyNode.appendNode('scope', 'implementation')
                    }
                }
            }
        }

    }

}
