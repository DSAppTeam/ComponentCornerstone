package com.plugin.module.task


import com.plugin.module.extension.PublicationManager
import com.plugin.module.extension.option.PublicationOption
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

/**
 * 编译 mis source task
 */
class CompileMisTask extends DefaultTask {

    PublicationOption publication

    @TaskAction
    void compileSource() {
        def project = getProject()
        def releaseJar = JarUtil.packJavaSourceJar(project, publication, MisPlugin.androidJarPath, MisPlugin.sModuleExtension.compileOptions, false)
        if (releaseJar == null) {
            throw new RuntimeException("nothing to push.")
        }
        JarUtil.packJavaDocSourceJar(publication)

        PublicationManager publicationManager = PublicationManager.getInstance()
        if (publication.versionNew != null) {
            publication.version = publication.versionNew
        }
        publicationManager.addPublication(publication)
    }
}
