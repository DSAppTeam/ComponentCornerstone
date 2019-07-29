package com.plugin.module

import com.plugin.module.extention.ModuleExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

class ModulePlugin implements Plugin<Project> {

    static File misDir

    @Override
    void apply(Project project) {

        if (project == project.rootProject) {
            project.getExtensions().create("module", ModuleExtension, project.objects)
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
        }
    }

}
