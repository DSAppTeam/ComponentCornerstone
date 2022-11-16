package com.plugin.component.task

import com.plugin.component.Runtimes
import com.plugin.component.extension.option.sdk.PublicationOption
import com.plugin.component.log.Logger
import com.plugin.component.utils.FileUtil
import com.plugin.component.utils.PublicationUtil
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

/**
 * 复制aar到Runtime#sImplDir路径下
 */
class AarLocalPublishTask extends DefaultTask {

    @Internal
    String inputPath
    @Internal
    File inputFile

    @Internal
    File outputDir
    @Internal
    PublicationOption publication

    @TaskAction
    void uploadLocalMaven() {
        String aarName = PublicationUtil.getAarName(publication)
        inputPath = FileUtil.findAarPath(project)
        outputDir = Runtimes.sImplDir
        if (inputPath != null) {
            inputFile = new File(inputPath)
            File outputFile = new File(outputDir, aarName)
            if (outputFile.exists()) {
                outputFile.delete()
            }
            FileUtil.copyFile(inputFile, outputFile)
        } else {
            Logger.buildOutput("project:${project.name} aar not found")
        }
    }
}