package com.plugin.component

import com.plugin.component.extension.ComponentExtension
import com.plugin.component.extension.module.PinInfo
import com.plugin.component.extension.module.ProjectInfo
import com.plugin.component.extension.option.pin.PinConfiguration
import com.plugin.component.extension.option.pin.PinOption
import com.plugin.component.extension.option.sdk.CompileOptions
import com.plugin.component.extension.option.debug.DebugConfiguration
import com.plugin.component.extension.option.sdk.PublicationOption
import com.plugin.component.extension.option.debug.DebugOption
import com.plugin.component.extension.option.sdk.SdkOption
import com.plugin.component.utils.ProjectUtil
import org.gradle.api.Project

class Runtimes {

    //sdk 发布信息
    private static Map<String, PublicationOption> sSdkPublicationMap = new HashMap<>()
    //impl 发布信息
    private static Map<String, PublicationOption> sImplPublicationMap = new HashMap<>()
    //模块信息
    private static Map<String, ProjectInfo> sProjectInfoMap = new HashMap<>()

    private static Map<String, PinConfiguration> sPinConfigurations = new HashMap<>()

    private static String sAndroidJarPath
    public static DebugOption sDebugOption
    public static SdkOption sSdkOption
    public static PinOption sPinOption
    public static ComponentExtension sExtension
    public static List<String> sAssembleModules = new ArrayList<>()
    public static File sSdkDir
    public static File sImplDir

    static initRuntimeConfiguration(Project root, ComponentExtension componentExtension) {
        sExtension = componentExtension
        sDebugOption = sExtension.debugOption
        sSdkOption = sExtension.sdkOption
        sPinOption = sExtension.pinOption
        root.extensions.add("targetDebugName", sDebugOption.targetDebugName)
        sAndroidJarPath = ProjectUtil.getAndroidJarPath(root, componentExtension.sdkOption.compileSdkVersion)
        Logger.buildOutput("")
        Logger.buildOutput(" =====> component.gradle配置信息 <===== ")
        Logger.buildOutput("")
        Logger.buildOutput("    全局配置")
        Logger.buildOutput(componentExtension.toString())
        Logger.buildOutput("    SDK配置")
        Logger.buildOutput(componentExtension.sdkOption.toString())
        Logger.buildOutput("    PIN配置")
        Logger.buildOutput(componentExtension.pinOption.toString())
        Logger.buildOutput("    DEBUG配置")
        Logger.buildOutput(componentExtension.debugOption.toString())
        Logger.buildOutput(" =====> component.gradle配置信息 <===== ")
        Logger.buildOutput("")

        //初始化pins
        root.allprojects.each {
            for (PinConfiguration pinConfiguration : sPinOption.configurationList) {
                if (ProjectUtil.isProjectSame(it.name, pinConfiguration.name)) {
                    sPinConfigurations.put(it.name, pinConfiguration)
                    pinConfiguration.initMainPin(it)
                }
            }
        }
    }

    static Map<String, PinConfiguration> getPinConfigurations() {
        return sPinConfigurations
    }

    static boolean hasPinModule() {
        return !sPinConfigurations.isEmpty()
    }

    static PinConfiguration getPinConfiguration(String name) {
        return sPinConfigurations.get(name)
    }

    static CompileOptions getCompileOption() {
        return sSdkOption.compileOption
    }

    static getAndroidJarPath() {
        return sAndroidJarPath
    }

    static boolean shouldApplyComponentPlugin(Project project) {
        return sExtension.shouldApplyComponentPlugin(project)
    }

    static String getDebugModuleName() {
        return sDebugOption.targetModuleName
    }

    static String getDebugTargetName() {
        return sDebugOption.targetDebugName
    }

    static List<DebugConfiguration> getDebugConfigurations() {
        return sDebugOption.configurationList
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

    static Map<String, PublicationOption> getSdkPublicationMap() {
        return sSdkPublicationMap
    }

    static void addProjectInfo(String projectName, ProjectInfo projectInfo) {
        sProjectInfoMap.put(projectName, projectInfo)
    }

    static ProjectInfo getProjectInfo(String projectName) {
        return sProjectInfoMap.get(projectName)
    }

    static ProjectInfo getCompileProjectWhenAssemble() {
        Set<String> keys = sProjectInfoMap.keySet()
        for (String key : keys) {
            ProjectInfo projectInfo = sProjectInfoMap.get(key)
            if (projectInfo.isCompileModuleAndAssemble()) {
                return projectInfo
            }
        }
        return null
    }
}
