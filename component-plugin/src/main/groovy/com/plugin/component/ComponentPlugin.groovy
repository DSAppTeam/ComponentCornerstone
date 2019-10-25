package com.plugin.component

import com.plugin.component.extension.ComponentExtension
import com.plugin.component.plugin.AbsPlugin
import com.plugin.component.plugin.DebugPlugin
import com.plugin.component.plugin.PinPlugin
import com.plugin.component.plugin.SdkPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 *   ./gradlew --no-daemon ComponentPlugin  -Dorg.gradle.debug=true
 *   ./gradlew --no-daemon [clean, :app:generateDebugSources, :library:generateDebugSources, :module_lib:generateDebugSources]  -Dorg.gradle.debug=true
 *    ./gradlew --no-daemon :app:assemble  -Dorg.gradle.debug=true
 *    ./gradlew --no-daemon [:library:assembleDebug, :libraryKotlin:assembleDebug, :debugModule:assembleDebug, :app:assembleDebug]  -Dorg.gradle.debug=true
 *  组件插件入口
 *  created by yummylau 2019/08/09
 */
class ComponentPlugin extends AbsPlugin implements Plugin<Project> {

    private AbsPlugin sdk = new SdkPlugin()
    private AbsPlugin debug = new DebugPlugin()
    private AbsPlugin pins = new PinPlugin()
    private ComponentExtension componentExtension

    @Override
    void apply(Project project) {
        if (project == project.rootProject) {
            componentExtension = project.getExtensions().create(Constants.COMPONENT, ComponentExtension, project)
            initExtension(componentExtension)
            evaluate(project, true)
            project.afterEvaluate {
                afterEvaluate(project, true)
            }
            project.gradle.projectsEvaluated {
                afterAllEvaluate()
            }
        } else {
            evaluate(project, false)
            project.afterEvaluate {
                afterEvaluate(project, false)
            }
        }
    }

    @Override
    void evaluate(Project project, boolean isRoot) {
        sdk.evaluate(project, isRoot)
        debug.evaluate(project, isRoot)
        pins.evaluate(project, isRoot)
    }

    @Override
    void afterEvaluate(Project project, boolean isRoot) {
        sdk.afterEvaluate(project, isRoot)
        debug.afterEvaluate(project, isRoot)
        pins.afterEvaluate(project, isRoot)
    }

    @Override
    void initExtension(ComponentExtension componentExtension) {
        sdk.initExtension(componentExtension)
        debug.initExtension(componentExtension)
        pins.initExtension(componentExtension)
    }


    @Override
    void afterAllEvaluate() {
        sdk.afterAllEvaluate()
        debug.afterAllEvaluate()
        pins.afterAllEvaluate()
    }
}
