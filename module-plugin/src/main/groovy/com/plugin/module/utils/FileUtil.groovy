package com.plugin.module.utils

import com.plugin.module.Constants
import com.plugin.module.Logger
import com.plugin.module.extension.ModuleRuntime
import org.gradle.api.Project

class FileUtil {


    static void beforeEvaluateHandlerBuildScript(Project project, File file) {
        if (file == null || !file.exists()) {
            return
        }
        List<String> lines = file.readLines()
        ModuleRuntime.beforeEvaluateText.put(project.name, file.text)
        ModuleRuntime.buildScripts.put(project.name, file)
        StringBuilder stringBuilder = new StringBuilder()
        for (String line : lines) {
            if (!ProjectUtil.containValidPluginDefine(line)) {
                stringBuilder.append(line + "\n")
            }
        }
        file.text = stringBuilder.toString()
        Logger.buildOutput(">>>>>> 评估前(" + project.name + ")build.gradle >>>>>>>")
        Logger.buildOutput("\n" + file.text)
    }

    static void afterEvaluateHandlerBuildScript(Project project) {
        ModuleRuntime.buildScripts.get(project.name).text = ModuleRuntime.beforeEvaluateText.get(project.name)
        Logger.buildOutput(">>>>>> 评估后(" + project.name + ")build.gradle >>>>>>>")
        Logger.buildOutput("\n" + ModuleRuntime.buildScripts.get(project.name).text)
    }

    /**
     * 是否是有效目标文件
     * @param file
     * @return
     */
    static boolean isValidPackSource(File file) {
        return file != null &&
                (file.name.endsWith(Constants.JAVA_FILE_END) || file.name.endsWith(Constants.KOTLIN_FILE_END))
    }

    /**
     * 复制文件
     *
     * @param source
     * @param target
     */
    static void copyFile(File source, File target) {
        try {
            InputStream input = new FileInputStream(source)
            OutputStream output = new FileOutputStream(target)
            byte[] buf = new byte[1024]
            int bytesRead
            while ((bytesRead = input.read(buf)) > 0) {
                output.write(buf, 0, bytesRead)
            }
            input.close()
            output.close()
        } catch (IOException e) {
            e.printStackTrace()
        }
    }
}
