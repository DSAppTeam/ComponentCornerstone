package com.plugin.component

import com.plugin.component.extension.module.BuildGradleInfo
import com.plugin.component.extension.module.ProjectInfo
import com.plugin.component.extension.option.CompileOption
import com.plugin.component.extension.option.DebugOption
import com.plugin.component.extension.option.PublicationOption
import org.gradle.api.Project

class Runtimes {

    //sdk 发布信息
    private static Map<String, PublicationOption> sSdkPublicationMap = new HashMap<>()
    //impl 发布信息
    private static Map<String, PublicationOption> sImplPublicationMap = new HashMap<>()
    //模块独立debug 信息
    private static Map<String, DebugOption> sDebugMap = new HashMap<>()
    //模块信息
    private static Map<String, ProjectInfo> sProjectInfoMap = new HashMap<>()
    //模块 build.gradle 脚本信息
    private static Map<String, BuildGradleInfo> sBuildGradleFile = new HashMap<>()

    //基本公用配信息
    public static String sAndroidJarPath
    public static String sMainModuleName
    public static String sCompileModuleName
    public static int sCompileSdkVersion
    public static CompileOption sCompileOption

    //本地 android jar 路径
    public static File sSdkDir
    public static File sImplDir


    static boolean addBuildGradleFile(ProjectInfo projectInfo) {
        Project project = projectInfo.project
        File file = new File(project.projectDir, Constants.BUILD_GRADLE)
        if (file != null && file.exists()) {
            List<String> lines = file.readLines()
            StringBuilder stringBuilder = new StringBuilder()
            Logger.buildOutput("project[" + project.name + "]" + "modify build.gradle start")
            long currentTime = System.currentTimeMillis()
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i)
                String content = line
                if (!line.trim().startsWith("#") && line.contains("apply plugin: 'com.android.library'")) {
                    if (projectInfo.aloneEnable) {
                        content = line.replaceAll("apply plugin: 'com.android.library'", "apply plugin: 'com.android.application'")
                        BuildGradleInfo buildGradleInfo = new BuildGradleInfo()
                        buildGradleInfo.modifyLine = i
                        buildGradleInfo.originLineContent = line
                        buildGradleInfo.mofifyLineContent = content
                        buildGradleInfo.file = file
                        sBuildGradleFile.put(project.name, buildGradleInfo)
                        Logger.buildOutput("project[" + project.name + "]" + "filePath", projectInfo.project.projectDir)
                        Logger.buildOutput("project[" + project.name + "]" + "modifyLine", buildGradleInfo.modifyLine)
                        Logger.buildOutput("project[" + project.name + "]" + "originLineContent", buildGradleInfo.originLineContent)
                        Logger.buildOutput("project[" + project.name + "]" + "mofifyLineContent", buildGradleInfo.mofifyLineContent)
                    }
                }
                stringBuilder.append(content + "\n")
            }
            file.text = stringBuilder.toString()
            Logger.buildOutput("project[" + project.name + "]" + "modify build gradle cost " + (System.currentTimeMillis() - currentTime) + "ms")
            Logger.buildOutput("project[" + project.name + "]" + "modify build gradle end")
            return true
        }
        return false
    }

    static void resetBuildGradleFile() {
        Set<String> keys = sBuildGradleFile.keySet()
        for (String key : keys) {
            BuildGradleInfo buildGradleInfo = sBuildGradleFile.get(key)
            if (buildGradleInfo.file != null && buildGradleInfo.file.exists()) {
                List<String> lines = buildGradleInfo.file.readLines()
                StringBuilder stringBuilder = new StringBuilder()
                for (int i = 0; i < lines.size(); i++) {
                    String line = lines.get(i)
                    if (i == buildGradleInfo.modifyLine && buildGradleInfo.mofifyLineContent == line) {
                        stringBuilder.append(buildGradleInfo.originLineContent + "\n")
                    } else {
                        stringBuilder.append(line + "\n")
                    }
                }
                buildGradleInfo.file.text = stringBuilder.toString()
                Logger.buildOutput("reset build.gradle[" + buildGradleInfo.file.path + "]")
            }
        }
    }

    static void addImplPublication(String projectName, PublicationOption publicationOption) {
        sImplPublicationMap.put(projectName, publicationOption)
    }

    static void addSdkPublication(String projectName, PublicationOption publicationOption) {
        sSdkPublicationMap.put(projectName, publicationOption)
    }

    static PublicationOption getSdkPublication(String projectName) {
        return sSdkPublicationMap.get(projectName)
    }

    static void addDebugInfo(String projectName, DebugOption debugOption) {
        sDebugMap.put(projectName, debugOption)
    }

    static DebugOption getDebugInfo(String projectName) {
        return sDebugMap.get(projectName)
    }

    static Set<String> getProjectNames() {
        return sProjectInfoMap.keySet()
    }

    static void addProjectInfo(String projectName, ProjectInfo projectInfo) {
        sProjectInfoMap.put(projectName, projectInfo)
    }

    static ProjectInfo getProjectInfo(String projectName) {
        return sProjectInfoMap.get(projectName)
    }
}
