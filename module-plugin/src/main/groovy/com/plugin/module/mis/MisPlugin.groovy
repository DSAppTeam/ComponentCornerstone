package com.plugin.module.mis

import com.android.build.gradle.AppPlugin
import com.android.build.gradle.LibraryPlugin
import com.plugin.module.mis.extension.Dependencies
import com.plugin.module.mis.extension.MisExtension
import com.plugin.module.mis.extension.OnMisExtensionListener
import com.plugin.module.mis.extension.Publication
import com.plugin.module.mis.extension.PublicationManager
import com.plugin.module.mis.utils.MisUtil
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

            //{project}/.gradle/mis
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

            //{project}/.gradle/mis/publicationManifest.xml
            publicationManager = PublicationManager.getInstance()
            publicationManager.loadManifest(project, misDir)

            misExtension = project.extensions.create('mis', MisExtension, new OnMisExtensionListener() {
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
                        childProject.pluginManager.apply('mis')
                    }
                }
            }

            project.afterEvaluate {

                //获取android jar目录
                androidJarPath = MisUtil.getAndroidJarPath(project, misExtension.compileSdkVersion)

                //扩展misPublication
                Dependencies.metaClass.misPublication { String value ->
                    String[] gav = MisUtil.filterGAV(value)
                    return 'mis-' + gav[0] + ':' + gav[1] + ':' + gav[2]
                }

                //检索每个子project目录下的mis.gradle并apply
                project.allprojects.each {
                    if (it == project) return
                    Project childProject = it
                    def misScript = new File(childProject.projectDir, 'mis.gradle')
                    if (misScript.exists()) {
                        misExtension.childProject = childProject
                        project.apply from: misScript
                    }
                }

                //
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
            return
        }

    }
}
