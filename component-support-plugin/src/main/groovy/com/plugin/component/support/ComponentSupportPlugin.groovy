package com.plugin.component.support

import com.plugin.component.support.extension.ComponentSupportExtension
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
        if (!isRoot) {
            return
        }

        extension = project.getExtensions().create(Constants.COMPONENT_SUPPORT, ComponentSupportExtension.class)
        project.afterEvaluate {


            Set<String> includeModules = ProjectUtil.getModuleName(extension.includes)
            Set<String> excludeModules = ProjectUtil.getModuleName(extension.excludes)
            boolean includeModel = !includeModules.isEmpty()

            project.allprojects.each {
                if (it == project) return
                Project childProject = it
                String projectName = ProjectUtil.getModuleName(childProject)
                if (includeModel && includeModules.contains(projectName)) {
                    childProject.apply plugin: 'com.android.component.support'
                    childProject.dependencies {
                        implementation 'com.effective.android:component-support-core:1.0.0'
                    }
                } else if (!excludeModules.contains(projectName)) {
                    childProject.apply plugin: 'com.android.component.support'
                    childProject.dependencies {
                        implementation 'com.effective.android:component-support-core:1.0.0'
                    }
                }
            }
        }
    }
}
