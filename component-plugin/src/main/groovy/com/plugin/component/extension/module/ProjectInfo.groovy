package com.plugin.component.extension.module

import com.plugin.component.utils.ProjectUtil
import com.plugin.component.Constants
import org.gradle.api.Project

/**
 * 插件运行时收集的项目信息
 * 在根root评估前判断子project插件是否存在AppPlugin或者LibraryPlugin
 * created by yummylau 2019/08/09
 */
class ProjectInfo {

    public static final String BUILD_GRADLE = "build.gradle"

    public Project project

    public boolean isVailModulePluginTarget = false
    public File buildGradleOriginFile
    public String buildGradleOriginContent = ""

    public String currentProjectName
    public String currentModuleName
    public boolean debugEnable

    //入口任务信息
    public String taskNames
    public List<String> modules = new ArrayList<>()         //包含的模块
    public boolean isAssemble = false                       //是否是asAssemble
    public boolean isDebug = false                          //是否是debug
    public String compileModuleName                         //入口模块名字

    ProjectInfo(Project project) {
        this.project = project
        this.currentModuleName = ProjectUtil.getModuleName(project)
        this.currentProjectName = project.name
        beforeEvaluateHandlerBuildScript()
        parseEnterTaskInfo()
        initRunAlone()
    }

    /**
     * 解析运行任务的信息
     */
    void parseEnterTaskInfo() {
        taskNames = ProjectUtil.getTaskName(project)
        List<String> taskNames = ProjectUtil.getTasks(project)
        if (!taskNames.isEmpty()) {
            for (String task : taskNames) {
                if (task.toUpperCase().contains("ASSEMBLE")
                        || task.contains("aR")
                        || task.contains("asR")
                        || task.contains("asD")
                        || task.toUpperCase().contains("TINKER")
                        || task.toUpperCase().contains("INSTALL")
                        || task.toUpperCase().contains("RESGUARD")) {
                    isAssemble = true
                    if (task.toUpperCase().contains("DEBUG")) {
                        isDebug = true
                    }
                    String[] strings = task.split(":")
                    modules.add(strings.length > 1 ? strings[strings.length - 2] : "all")
                    break
                }
            }
        }
        compileModuleName = Constants.DEFAULT_MAIN_MODULE_NAME
        if (isAssemble) {
            if (!modules.isEmpty() && modules.get(0) != null
                    && modules.get(0).trim().length() > 0
                    && !modules.get(0).equals("all")) {
                compileModuleName = modules.get(0)
            } else {
                compileModuleName = Runtimes.sModuleExtension.mainModuleName
            }
            if (compileModuleName == null || compileModuleName.trim().isEmpty()) {
                compileModuleName = Constants.DEFAULT_MAIN_MODULE_NAME
            }
        }
    }

    /**
     * 修改build.gradle 脚本
     */
    void beforeEvaluateHandlerBuildScript() {
        buildGradleOriginFile = new File(project.projectDir, BUILD_GRADLE)
        if (buildGradleOriginFile != null && buildGradleOriginFile.exists()) {
            List<String> lines = buildGradleOriginFile.readLines()
            buildGradleOriginContent = buildGradleOriginFile.text
            StringBuilder stringBuilder = new StringBuilder()
            for (String line : lines) {
                if (!ProjectUtil.containValidPluginDefine(line)) {
                    stringBuilder.append(line + "\n")
                }
            }
            buildGradleOriginFile.text = stringBuilder.toString()
//            Logger.buildOutput(">>>>>> 评估前(" + project.name + ")build.gradle >>>>>>>")
//            Logger.buildOutput("\n" + buildGradleOriginFile.text)
            isVailModulePluginTarget = true
        } else {
            isVailModulePluginTarget = false
        }
    }

    /**
     * 还原build.gradle 脚本
     */
    void afterEvaluateHandlerBuildScript() {
        if (isVailModulePluginTarget) {
            buildGradleOriginFile.text = buildGradleOriginContent
//            Logger.buildOutput(">>>>>> 评估后(" + project.name + ")build.gradle >>>>>>>")
//            Logger.buildOutput("\n" + buildGradleOriginFile.text)
        }
    }

    /**
     * 决定是否打开单独运行
     */
    void initRunAlone() {
        this.debugEnable = ProjectUtil.isRunAlone(project)
        if (debugEnable && !isSync()) {
            //当前编译的模块才需要设置为true
            if (currentModuleName == compileModuleName) {
                debugEnable = true
            } else {
                //如果当前模块不是编译模块且不是main模块，需要更改为false
                if (!isMainModule()) {
                    debugEnable = false
                }
            }
        }
    }

    boolean debugEnableAndNoSync() {
        return debugEnable && !isSync()
    }

    boolean isMainModule() {
        return currentModuleName == ProjectUtil.getMainModuleName()
    }

    boolean isSync() {
        return project.gradle.startParameter.taskNames.isEmpty()
    }
}
