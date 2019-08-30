package com.plugin.component.support

import com.android.build.gradle.BaseExtension
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

    ComponentSupportExtension extension

    @Override
    void apply(Project project) {

        boolean isRoot = project == project.rootProject
        extension = project.getExtensions().create(Constants.COMPONENT_SUPPORT, ComponentSupportExtension.class)
        if (isRoot) {
            Logger.buildOutput("")
            Logger.buildOutput("ComponentSupportPlugin >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>")
            project.afterEvaluate {
                Logger.buildOutput("methodCostEnable", extension.methodCostEnable)
                Logger.buildOutput("includes", extension.includes)
                Logger.buildOutput("excludes", extension.excludes)
                Set<String> includeModules = ProjectUtil.getModuleName(extension.includes)
                Set<String> excludeModules = ProjectUtil.getModuleName(extension.excludes)
                boolean includeModel = !includeModules.isEmpty()
                Logger.buildOutput("select module by " + (includeModel ? "include" : "exclude"))
                project.allprojects.each {
                    if (it == project) return
                    Project childProject = it
                    String projectName = ProjectUtil.getModuleName(childProject)
                    if (includeModel) {
                        if (includeModules.contains(projectName)) {
                            addPluginToProject(childProject)
                        }
                    } else {
                        if (!excludeModules.contains(projectName)) {
                            addPluginToProject(childProject)
                        }
                    }
                }
            }
        } else {
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
        Logger.buildOutput("project[" + project.name + "] apply plugin: " + Constants.SUPPORT_PLUGIN)
        Logger.buildOutput("project[" + project.name + "] implementation " + Constants.SUPPORT_DEPENDENCY)
        Logger.buildOutput("")
    }
}
