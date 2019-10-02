package com.plugin.component

import com.plugin.component.extension.module.ProjectInfo
import com.plugin.component.extension.option.CompileOption
import com.plugin.component.extension.option.DebugOption
import com.plugin.component.extension.option.PublicationOption

class Runtimes {

    //调试信息
    private static Set<DebugOption> sDebugOptions = new HashSet<>()
    //sdk 发布信息
    private static Map<String, PublicationOption> sSdkPublicationMap = new HashMap<>()
    //impl 发布信息
    private static Map<String, PublicationOption> sImplPublicationMap = new HashMap<>()
    //模块信息
    private static Map<String, ProjectInfo> sProjectInfoMap = new HashMap<>()

    //基本公用配信息
    public static String sAndroidJarPath
    public static String sMainModuleName
    public static String sDebugModuleName
    public static String sDebugComponentName = "debug"
    public static int sCompileSdkVersion
    public static CompileOption sCompileOption
    public static Set<String> sValidComponents

    //本地 android jar 路径
    public static File sSdkDir
    public static File sImplDir


    static void setMainModuleName(String sMainModuleName) {
        Runtimes.sMainModuleName = sMainModuleName
    }

    static String getMainModuleName() {
        if (sMainModuleName == null || sMainModuleName.isEmpty()) {
            sMainModuleName = Constants.DEFAULT_MAIN_MODULE_NAME
        }
        return sMainModuleName
    }

    static void addDebugOptions(DebugOption debugOption){
        if(debugOption != null){
            sDebugOptions.add(debugOption)
        }
    }

    static boolean hasDebugOptions(){
        return !sDebugOptions.isEmpty()
    }

    static Set<DebugOption> getDebugOptions(){
        return sDebugOptions
    }

    static void addImplPublication(String projectName, PublicationOption publicationOption) {
        sImplPublicationMap.put(projectName, publicationOption)
    }

    static PublicationOption getImplPublication(String projectName) {
        return sImplPublicationMap.get(projectName)
    }

    static void addSdkPublication(String projectName, PublicationOption publicationOption) {
        sSdkPublicationMap.put(projectName, publicationOption)
    }

    static PublicationOption getSdkPublication(String projectName) {
        return sSdkPublicationMap.get(projectName)
    }

    static void addProjectInfo(String projectName, ProjectInfo projectInfo) {
        sProjectInfoMap.put(projectName, projectInfo)
    }

    static ProjectInfo getProjectInfo(String projectName) {
        return sProjectInfoMap.get(projectName)
    }
}
