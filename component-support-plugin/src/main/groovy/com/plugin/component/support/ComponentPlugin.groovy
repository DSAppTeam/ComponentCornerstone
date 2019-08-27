package com.plugin.component

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 *  组件插件入口
 *  created by yummylau 2019/08/09
 */
class ComponentPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        boolean isRoot = project == project.rootProject
        if (!isRoot) {

        }
    }
}
