package com.plugin.component.task


import com.plugin.component.Runtimes
import com.plugin.component.extension.PublicationManager
import com.plugin.component.extension.option.sdk.PublicationOption
import com.plugin.component.utils.JarUtil
import com.plugin.component.utils.PublicationUtil
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
        File target = new File(Runtimes.sSdkDir, PublicationUtil.getJarName(publication))
        if (!target.exists()) {
            def releaseJar = JarUtil.packJavaSourceJar(project, publication, Runtimes.getAndroidJarPath(), Runtimes.getCompileOption(), false)
            if (releaseJar == null) {
                throw new RuntimeException("nothing to push.")
            }
        }
        JarUtil.packJavaDocSourceJar(publication)
        PublicationManager publicationManager = PublicationManager.getInstance()
        if (publication.versionNew != null) {
            publication.sdkVersion = publication.versionNew
        }
        publicationManager.addPublication(publication)
    }
}
