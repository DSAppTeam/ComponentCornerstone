package com.plugin.component.support

import com.android.build.gradle.AppPlugin
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.LibraryPlugin
import com.plugin.component.support.extension.ComponentSupportExtension
import com.plugin.component.support.transform.MethodCostTransform
import com.plugin.component.support.utils.ProjectUtil
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 *  组件插件入口
 *  created by yummylau 2019/08/09
 */
class ComponentSupportPlugin implements Plugin<Project> {

    static ComponentSupportExtension extension
    static Set<String> includeModules
    static Set<String> excludeModules
    static boolean includeModel

    @Override
    void apply(Project project) {

        boolean isRoot = project == project.rootProject
        if (isRoot) {
            extension = project.getExtensions().create(Constants.COMPONENT_SUPPORT, ComponentSupportExtension.class)
            Logger.buildOutput("")
            Logger.buildOutput("ComponentSupportPlugin >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>")
            project.afterEvaluate {
                Logger.buildOutput("methodCostEnable", extension.methodCostEnable)
                Logger.buildOutput("includes", extension.includes)
                Logger.buildOutput("excludes", extension.excludes)
                includeModules = ProjectUtil.getModuleName(extension.includes)
                excludeModules = ProjectUtil.getModuleName(extension.excludes)
                includeModel = !includeModules.isEmpty()
                Logger.buildOutput("select module by " + (includeModel ? "include" : "exclude"))
                project.allprojects.each {
                    if (it == project) return
                    if (!isValidPluginModule(it)) return
                    Project childProject = it

                    if (childProject.pluginManager.hasPlugin('com.android.application')
                            || childProject.pluginManager.hasPlugin('com.android.library')) {
                        addPluginToProject(childProject)
                    } else {
                        childProject.plugins.whenObjectAdded {
                            if (it instanceof AppPlugin || it instanceof LibraryPlugin) {
                                addPluginToProject(childProject)
                            }
                        }
                    }
                }
            }
        } else {
            if (!isValidPluginModule(project)) return
            if (project.plugins.hasPlugin('com.android.application')) {
                if (extension.methodCostEnable) {
                    Logger.buildOutput("project[" + project.name + "] enable methodCost")
                    project.extensions.findByType(BaseExtension.class).registerTransform(new MethodCostTransform(project))
                }
            }
        }
    }

    void addPluginToProject(Project project) {
        project.apply plugin: Constants.SUPPORT_PLUGIN
        project.dependencies {
            implementation Constants.SUPPORT_DEPENDENCY
        }
        Logger.buildOutput("project[" + project.name + "] implementation " + Constants.SUPPORT_DEPENDENCY)
        Logger.buildOutput("project[" + project.name + "] apply plugin: " + Constants.SUPPORT_PLUGIN)
        Logger.buildOutput("")
    }

    private boolean isValidPluginModule(Project project) {
        if (includeModel) {
            return includeModules.contains(project.name)
        } else {
            return !excludeModules.contains(project.name)
        }
    }
}
