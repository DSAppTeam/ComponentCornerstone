package com.plugin.module.utils

import com.plugin.module.Constants
import com.plugin.module.Logger
import com.plugin.module.extension.ModuleRuntime
import com.plugin.module.extension.module.AssembleTask
import org.gradle.api.Project

import javax.annotation.Nonnull

class ProjectUtil {

    static String getMainModuleName() {
        String name = ModuleRuntime.sModuleExtension.mainModuleName
        if (name == null || name.isEmpty()) {
            return Constants.DEFAULT_MAIN_MODULE_NAME
        }
        return name
    }

    static boolean isRunalone(Project project) {
        if ((getModuleName(project)) == getMainModuleName()) {
            return true
        }
        if (ModuleRuntime.aloneRunMap.get(project.name) == null) {
            return false
        }
        return ModuleRuntime.aloneRunMap.get(project.name).runAlone
    }

    static List<String> getTasks(Project project) {
        return project.gradle.getStartParameter().taskNames
    }

    static getTaskName(Project project) {
        return project.gradle.startParameter.taskNames.toString()
    }

    static getModuleName(Project project) {
        return project.path.replace(":", "")
    }

    static AssembleTask parseTaskInfo(@Nonnull List<String> taskNames) {
        AssembleTask assembleTask = new AssembleTask()
        if (!taskNames.isEmpty()) {
            for (String task : taskNames) {
                if (task.toUpperCase().contains("ASSEMBLE")
                        || task.contains("aR")
                        || task.contains("asR")
                        || task.contains("asD")
                        || task.toUpperCase().contains("TINKER")
                        || task.toUpperCase().contains("INSTALL")
                        || task.toUpperCase().contains("RESGUARD")) {
                    if (task.toUpperCase().contains("DEBUG")) {
                        assembleTask.isDebug = true
                    }
                    Logger.buildOutput("task is debug (" + assembleTask.isDebug + ")")
                    assembleTask.isAssemble = true
                    String[] strings = task.split(":")
                    assembleTask.modules.add(strings.length > 1 ? strings[strings.length - 2] : "all")
                    break
                }
            }
        }
        return assembleTask
    }

    /**
     * 根据当前的task，获取要运行的组件，规则如下：
     * assembleRelease ---app
     * app:assembleRelease :app:assembleRelease ---app
     * sharecomponent:assembleRelease :sharecomponent:assembleRelease ---sharecomponent
     *
     * @param assembleTask
     * @param project
     * @param assembleTask
     * @return
     */
    static String parseMainModuleName(@Nonnull Project project, @Nonnull AssembleTask assembleTask) {
        String compileModule = Constants.DEFAULT_MAIN_MODULE_NAME
        if (assembleTask.modules.size() > 0 && assembleTask.modules.get(0) != null
                && assembleTask.modules.get(0).trim().length() > 0
                && !assembleTask.modules.get(0).equals("all")) {
            compileModule = assembleTask.modules.get(0)
        } else {
            compileModule = ModuleRuntime.sModuleExtension.mainModuleName
        }
        if (compileModule == null || compileModule.trim().length() <= 0) {
            compileModule = Constants.DEFAULT_MAIN_MODULE_NAME
        }
        return compileModule
    }

    static boolean containValidPluginDefine(String string) {
        return string.contains("apply") &&
                string.contains("plugin") && (string.contains("com.android.library") || string.contains("com.android.application"))
    }

}
