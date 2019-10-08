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
    public String name

    //入口任务信息
    public String taskNames
    public List<String> modules = new ArrayList<>()         //包含的模块
    public boolean isAssemble = false                       //是否是asAssemble
    public boolean isDebug = false                          //是否是debug
    public String compileModuleName                         //入口模块名字
    public Set<String> componentDependencies = new HashSet<>()   //模块依赖的component

    ProjectInfo(Project project) {
        this.project = project
        this.name = ProjectUtil.getProjectName(project)
        parseEnterTaskInfo()
    }

    String getComponentDependenciesString() {
        StringBuilder stringBuilder = new StringBuilder()
        for (String string : componentDependencies) {
            stringBuilder.append(" :project(")
            stringBuilder.append(string)
            stringBuilder.append(")")
        }
        return stringBuilder.toString()
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

    boolean isCompileModuleAndAssemble() {
        return ProjectUtil.isProjectSame(project.name, compileModuleName) && isAssemble
    }

    boolean isDebugModule() {
        return ProjectUtil.isProjectSame(project.name, Runtimes.getDebugModuleName())
    }


    boolean isMainModule() {
        return ProjectUtil.isProjectSame(project.name, Runtimes.getMainModuleName())
    }

    boolean isSync() {
        return project.gradle.startParameter.taskNames.isEmpty()
    }
}
