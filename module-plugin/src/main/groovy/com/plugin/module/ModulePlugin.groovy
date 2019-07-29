package com.plugin.module

import com.android.build.gradle.AppPlugin
import com.android.build.gradle.LibraryPlugin
import com.plugin.module.alone.AloneExtension
import com.plugin.module.mis.extension.MisExtension
import com.plugin.module.mis.extension.OnMisExtensionListener
import com.plugin.module.mis.extension.Publication
import com.plugin.module.mis.extension.PublicationManager
import org.gradle.api.Plugin
import org.gradle.api.Project

class ModulePlugin implements Plugin<Project> {

    static File misDir
    static MisExtension mis
    static AloneExtension alone
    static PublicationManager publicationManager

    @Override
    void apply(Project project) {

        boolean isRootProject = project == project.rootProject

        if (isRootProject) {
            initPlugin(project)
        }

    }

    private void initPlugin(Project project) {
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
        publicationManager = PublicationManager.getInstance()
        publicationManager.loadManifest(project, misDir)

        mis = project.getExtensions().create("mis", MisExtension, new OnMisExtensionListener() {
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


                childProject.plugins.whenObjectAdded {
                    if (it instanceof AppPlugin || it instanceof LibraryPlugin) {
//                    childProject.pluginManager.apply('mis')
                        def del = childProject.plugins
                    }
                }
            }
        }
    }

}
