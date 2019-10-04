package com.plugin.component.task


import com.plugin.component.Runtimes
import com.plugin.component.extension.PublicationManager
import com.plugin.component.extension.option.publication.PublicationOption
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

/**
 * 编译 sdk task
 * created by yummylau 2019/08/09
 */
class CompileSdkTask extends DefaultTask {

    PublicationOption publication

    @TaskAction
    void compileSource() {
        def project = getProject()
        def releaseJar = JarUtil.packJavaSourceJar(project, publication, Runtimes.sAndroidJarPath, Runtimes.sCompileOption, false)
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
