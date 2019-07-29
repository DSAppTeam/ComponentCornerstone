package com.plugin.module.mis.task

import com.plugin.module.mis.extension.Publication
import com.plugin.module.mis.extension.PublicationManager
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

class CompileMisTask extends DefaultTask{

    Publication publication

    @TaskAction
    void compileSource() {
        def project = getProject()
        def releaseJar = JarUtil.packJavaSourceJar(project, publication, MisPlugin.androidJarPath, MisPlugin.misExtension.compileOptions, false)
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
