package com.plugin.module.mis

import com.android.build.gradle.AppPlugin
import com.android.build.gradle.LibraryPlugin
import com.plugin.module.mis.extension.Dependencies
import com.plugin.module.mis.extension.MisExtension
import com.plugin.module.mis.extension.OnMisExtensionListener
import com.plugin.module.mis.extension.Publication
import com.plugin.module.mis.extension.PublicationManager
import com.plugin.module.mis.utils.JarUtil
import com.plugin.module.mis.utils.MisUtil
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project

class MisPlugin implements Plugin<Project> {

    static File misDir
    static MisExtension misExtension
    static String androidJarPath
    static PublicationManager publicationManager
    Project project


    @Override
    void apply(Project project) {

        this.project = project


        //如果是根项目
        if (project == project.rootProject) {

            //{project}/.gradle/misExtension
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

            //{project}/.gradle/misExtension/publicationManifest.xml
            publicationManager = PublicationManager.getInstance()
            publicationManager.loadManifest(project, misDir)

            //读取跟节点信息添加监听
            misExtension = project.extensions.create('misExtension', MisExtension, new OnMisExtensionListener() {
                @Override
                void onPublicationAdded(Project childProject, Publication publication) {
                    initPublication(childProject, publication)
                    publicationManager.addDependencyGraph(publication)
                }
            })

            project.allprojects.each {
                if (it == project) return
                Project childProject = it
                childProject.repositories {
                    flatDir {
                        dirs misDir.absolutePath
                    }
                }

                childProject.plugins.whenObjectAdded {
                    if (it instanceof AppPlugin || it instanceof LibraryPlugin) {
                        childProject.pluginManager.apply('misExtension')
                    }
                }
            }

            project.afterEvaluate {

                //获取android jar目录
                androidJarPath = MisUtil.getAndroidJarPath(project, misExtension.compileSdkVersion)

                //扩展misPublication
                Dependencies.metaClass.misPublication { String value ->
                    String[] gav = MisUtil.filterGAV(value)
                    return 'misExtension-' + gav[0] + ':' + gav[1] + ':' + gav[2]
                }

                //检索每个子project目录下的mis.gradle并apply
                project.allprojects.each {
                    if (it == project) return
                    Project childProject = it
                    def misScript = new File(childProject.projectDir, 'misExtension.gradle')
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

                    //搜索包含mis子项目
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
            return
        }

        //子project
        if (!MisUtil.hasAndroidPlugin(project)) {
            throw new GradleException("The android or android-library plugin must be applied to the project.")
        }

        project.dependencies.metaClass.misPublication { Object value ->
            String[] gav = MisUtil.filterGAV(value)
            return getPublication(gav[0], gav[1])
        }


        //获取子project publication
        List<Publication> publications = publicationManager.getPublicationByProject(project)
        project.dependencies {
            publications.each {
                implementation getPublication(it.groupId, it.artifactId)
            }
        }

        //解析子项目 publication 并为子项目添加依赖
        if (project.gradle.startParameter.taskNames.isEmpty()) {
            publications.each {
                addPublicationDependencies(it)
            }
        }


        //发布到maven
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
     * 获取依赖
     * @param groupId
     * @param artifactId
     * @return
     */
    def getPublication(String groupId, String artifactId) {
        Publication publication = publicationManager.getPublication(groupId, artifactId)
        if (publication != null) {
            if (publication.invalid) {
                return []
            } else if (publication.useLocal) {
                return ':misExtension-' + publication.groupId + '-' + publication.artifactId + ':'
            } else {
                return publication.groupId + ':' + publication.artifactId + ':' + publication.version
            }
        } else {
            return []
        }
    }

    /**
     * 添加真实依赖
     * @param publication
     */
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

    /**
     * 处理本地jar
     * 1. 如果 publication 无效，则保证清除本地 jar
     * 2. 如果 jar 存在且资源没有发生修改，则默认 jar 可用且使用本地
     * 3. 重新打包 jar，如果打包失败则 publication 无效，保证清除本地旧jar
     *                如果打包成功则新jar覆盖旧jar，默 jar 可用且使用本地
     * @param project
     * @param publication
     * @return
     */
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

        //重新打包jar
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

    /**
     * 处理moven jar
     * 1. 如果 publication 无效，则保证清除本地 jar
     * 2. 如果jar 存在
     *      发生修改则重打包，处理打包成功失败结果
     *    如果jar不存在且没有发生修改，则使用 moven 依赖
     *    如果jar不存在且发生修改，则使用 moven 依赖
     * @param project
     * @param publication
     * @return
     */
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
