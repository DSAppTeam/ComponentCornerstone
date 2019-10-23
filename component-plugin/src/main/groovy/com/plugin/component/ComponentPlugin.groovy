package com.plugin.component

import com.plugin.component.extension.ComponentExtension
import com.plugin.component.plugin.BasePlugin
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
class ComponentPlugin implements Plugin<Project>,BasePlugin{

    private BasePlugin sdk = new SdkPlugin()
    private BasePlugin debug = new DebugPlugin()
    private BasePlugin pins = new PinPlugin()

    @Override
    void apply(Project project) {

        if (project == project.rootProject) {
            ComponentExtension componentExtension = project.getExtensions().create(Constants.COMPONENT, ComponentExtension, project)
            initExtension(componentExtension)
            evaluateRoot(project)
            project.afterEvaluate {
                afterEvaluateRoot(project)
            }
            project.gradle.projectsEvaluated {
                afterAllEvaluate()
            }
        } else {
            evaluateChild(project)
            project.afterEvaluate {
                afterEvaluateChild(project)
            }
        }
    }

    @Override
    void initExtension(ComponentExtension componentExtension) {
        sdk.initExtension(componentExtension)
        debug.initExtension(componentExtension)
        pins.initExtension(componentExtension)
    }

    @Override
    void evaluateChild(Project child) {
        sdk.evaluateChild(child)
        debug.evaluateChild(child)
        pins.evaluateChild(child)
    }

    @Override
    void afterEvaluateChild(Project child) {
        sdk.afterEvaluateChild(child)
        debug.afterEvaluateChild(child)
        pins.afterEvaluateChild(child)
    }

    @Override
    void evaluateRoot(Project root) {
        sdk.evaluateRoot(root)
        debug.evaluateRoot(root)
        pins.evaluateRoot(root)
    }

    @Override
    void afterEvaluateRoot(Project root) {
        sdk.afterEvaluateRoot(root)
        debug.afterEvaluateRoot(root)
        pins.afterEvaluateRoot(root)
    }

    @Override
    void afterAllEvaluate() {
        sdk.afterAllEvaluate()
        debug.afterAllEvaluate()
        pins.afterAllEvaluate()
    }
}
