package com.plugin.module.utils

import com.android.build.gradle.AppPlugin
import com.android.build.gradle.LibraryPlugin
import com.plugin.module.Constants
import com.plugin.module.ModuleRuntime
import org.gradle.api.Project

class ProjectUtil {

    /**
     * 是否有效注入的对象，只有实现application
     * @param project
     * @return
     */
    static boolean isModulePluginTarget(Project project) {
        return project.plugins.contains(AppPlugin.class) || project.plugins.contains(LibraryPlugin.class);
    }

    /**
     * 获取项目工程的主模块名，默认 'app'
     * @return
     */
    static String getMainModuleName() {
        String name = ModuleRuntime.sModuleExtension.mainModuleName
        if (name == null || name.isEmpty()) {
            return Constants.DEFAULT_MAIN_MODULE_NAME
        }
        return name
    }

    /**
     * 如果当前就是工程主模块，则默认 ture
     * 否则则判断是否有配置脚本
     * @param project
     * @return
     */
    static boolean isRunAlone(Project project) {
        if ((getModuleName(project)) == getMainModuleName()) {
            return true
        }
        if(ModuleRuntime.aloneRunMap.get(project.name) == null){
            return false
        }
        return ModuleRuntime.aloneRunMap.get(project.name).runAlone
    }

    static List<String> getTasks(Project project) {
        return project.gradle.getStartParameter().taskNames
    }

    static String getTaskName(Project project) {
        return project.gradle.startParameter.taskNames.toString()
    }

    static String getModuleName(Project project) {
        return project.path.replace(":", "")
    }

    static boolean containValidPluginDefine(String string) {
        return string.contains("apply") &&
                string.contains("plugin") && (string.contains("com.android.library") || string.contains("com.android.application"))
    }
}
