package com.plugin.component.extension.module

import com.plugin.component.Constants
import com.plugin.component.Runtimes
import com.plugin.component.utils.ProjectUtil
import org.gradle.api.Project

/**
 * 插件运行时收集的项目信息
 * 在根root评估前判断子project插件是否存在AppPlugin或者LibraryPlugin
 * created by yummylau 2019/08/09
 */
class ProjectInfo {


    public Project project

    public boolean isVailModulePluginTarget = false
    public File buildGradleOriginFile
    public String buildGradleOriginContent = ""

    public String currentModuleName
    public boolean aloneEnable

    //入口任务信息
    public String taskNames
    public List<String> modules = new ArrayList<>()         //包含的模块
    public boolean isAssemble = false                       //是否是asAssemble
    public boolean isDebug = false                          //是否是debug
    public String compileModuleName                         //入口模块名字

    ProjectInfo(Project project) {
        this.project = project
        this.currentModuleName = ProjectUtil.getModuleName(project)
        parseEnterTaskInfo()
        initRunAlone()
        isVailModulePluginTarget = Runtimes.addBuildGradleFile(this)
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
                compileModuleName = Runtimes.sMainModuleName
            }
            if (compileModuleName == null || compileModuleName.trim().isEmpty()) {
                compileModuleName = Constants.DEFAULT_MAIN_MODULE_NAME
            }
        }
    }

    /**
     * 决定是否打开单独运行
     */
    void initRunAlone() {
        this.aloneEnable = ProjectUtil.isRunAlone(project)
        if (aloneEnable && !isSync()) {
            //当前编译的模块才需要设置为true
            if (currentModuleName == compileModuleName) {
                aloneEnable = true
            } else {
                //如果当前模块不是编译模块且不是main模块，需要更改为false
                if (!isMainModule()) {
                    aloneEnable = false
                }
            }
        }
    }


    boolean aloneEnableAndNoSync() {
        return aloneEnable && !isSync()
    }

    boolean isMainModule() {
        return currentModuleName == ProjectUtil.getMainModuleName()
    }

    boolean shouldMofifyAloneSourceSet() {
        return ProjectUtil.isRunAlone(project) && !isMainModule()
    }

    boolean isSync() {
        return project.gradle.startParameter.taskNames.isEmpty()
    }
}
