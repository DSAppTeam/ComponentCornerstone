package com.plugin.component

import com.android.build.gradle.BaseExtension
import com.plugin.component.support.transform.MethodCostTransform
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 *  组件插件入口
 *  created by yummylau 2019/08/09
 */
class ComponentSupportPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        if (project != project.rootProject) {
            project.extensions.findByType(BaseExtension.class).registerTransform(new MethodCostTransform(childProject))
        }
    }
}
