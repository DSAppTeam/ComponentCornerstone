package com.plugin.component

import com.plugin.component.extension.ComponentExtension
import com.plugin.component.extension.module.ProjectInfo
import com.plugin.component.extension.option.CompileOptions
import com.plugin.component.extension.option.addition.AdditionOption
import com.plugin.component.extension.option.publication.PublicationOption
import com.plugin.component.extension.option.debug.DebugOption
import com.plugin.component.utils.ProjectUtil
import org.gradle.api.Project

class Runtimes {

    //sdk 发布信息
    private static Map<String, PublicationOption> sSdkPublicationMap = new HashMap<>()
    //impl 发布信息
    private static Map<String, PublicationOption> sImplPublicationMap = new HashMap<>()
    //模块信息
    private static Map<String, ProjectInfo> sProjectInfoMap = new HashMap<>()

    //本地 android jar 路径
    public static File sSdkDir
    public static File sImplDir

    public static String sAndroidJarPath
    public static String sMainModuleName
    public static CompileOptions sCompileOption
    private static AdditionOption sAdditionOption
    public static DebugOption sDebugOption
    public static int sCompileSdkVersion
    public static Set<String> sValidComponents

    static initRuntimeConfiguration(Project root,ComponentExtension componentExtension){
        
        sAndroidJarPath = ProjectUtil.getAndroidJarPath(root, componentExtension.compileSdkVersion)
        sMainModuleName = componentExtension.mainModuleName
        sCompileSdkVersion = componentExtension.compileSdkVersion
        sAdditionOption = componentExtension.additionOption
        sCompileOption = componentExtension.compileOption
        sDebugOption = componentExtension.debugOption
        Set<String> includeModules = ProjectUtil.getModuleName(componentExtension.includes)
        Set<String> excludeModules = ProjectUtil.getModuleName(componentExtension.excludes)
        boolean includeModel = !includeModules.isEmpty()
        sValidComponents = getValidComponents(root, includeModules, excludeModules, includeModel)


        root.extensions.add("targetDebugName", sDebugOption.targetDebugName)
        Logger.buildOutput("AndroidJarPath", sAndroidJarPath)
        Logger.buildOutput("mainModuleName", getMainModuleName())
        Logger.buildOutput("compileSdkVersion", sCompileSdkVersion)
        Logger.buildOutput("CompileOptions", sCompileOption.toString())
        Logger.buildOutput("includes", componentExtension.includes)
        Logger.buildOutput("excludes", componentExtension.excludes)
        Logger.buildOutput("Select module by " + (includeModel ? "include" : "exclude"))
        Logger.buildOutput("生效模块", sValidComponents.toList().toString())
        Logger.buildOutput("调试信息", sDebugOption.toString())
        Logger.buildOutput("扩展信息", sAdditionOption.toString())
    }

    static boolean shouldApplyComponentPlugin(Project project){
        return sValidComponents.contains(ProjectUtil.getProjectName(project))
    }

    static String getMainModuleName() {
        if (sMainModuleName == null || sMainModuleName.isEmpty()) {
            sMainModuleName = Constants.DEFAULT_MAIN_MODULE_NAME
        }
        return sMainModuleName
    }

    static String getDebugModuleName(){
        return sDebugOption != null && sDebugOption.targetModuleName
    }

    static String getDebugTargetName(){
        return sDebugOption != null && sDebugOption.targetDebugName
    }

    static String getDebugConfigurations(){
        return sDebugOption != null && sDebugOption.configurationList
    }

    static boolean enbaleMethodCost(){
        return sAdditionOption != null && sAdditionOption.methodCostOption.enable
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

    private static Set<String> getValidComponents(Project root, Set<String> includeModules, Set<String> excludeModules, boolean includeModel) {
        Set<String> result = new HashSet<>()
        root.allprojects.each {
            if (includeModel) {
                if (includeModules.contains(ProjectUtil.getProjectName(it))) {
                    result.add(it.name)
                }
            } else {
                if (!excludeModules.contains(ProjectUtil.getProjectName(it))) {
                    result.add(it.name)
                }
            }
        }
        return result
    }
}
