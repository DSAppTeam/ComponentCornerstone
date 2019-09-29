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
    public static Map<String, DebugOption> sDebugMap = new HashMap<>()
    //模块信息
    private static Map<String, ProjectInfo> sProjectInfoMap = new HashMap<>()
    //模块 build.gradle 脚本信息
    private static Map<String, BuildGradleInfo> sBuildGradleFile = new HashMap<>()

    //基本公用配信息
    public static String sAndroidJarPath
    public static String sMainModuleName
    public static String sDebugModuleName
    public static int sCompileSdkVersion
    public static CompileOption sCompileOption

    //本地 android jar 路径
    public static File sSdkDir
    public static File sImplDir


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

    static void addProjectInfo(String projectName, ProjectInfo projectInfo) {
        sProjectInfoMap.put(projectName, projectInfo)
    }

    static ProjectInfo getProjectInfo(String projectName) {
        return sProjectInfoMap.get(projectName)
    }
}
