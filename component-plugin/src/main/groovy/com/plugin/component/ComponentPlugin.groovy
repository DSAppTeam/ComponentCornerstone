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
 *  插件统一在root模块配置收集：所有配置信息在root评估之后统一初始化完成
 *  pin功能需要在android插件apply之前
 *  debug/sdk功能需要在android插件apply之后
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
            Runtimes.initRuntimeConfigurationOnEvaluate(project)
            project.afterEvaluate {
                Runtimes.initRuntimeConfigurationAfterEvaluate(project, componentExtension)
                Runtimes.hookAfterApplyingAndroidPlugin(project, this)
                Runtimes.injectComponentPlugin(project)
            }
            project.gradle.projectsEvaluated {
                afterAllEvaluate(project)
            }
        } else {
            evaluateBeforeAndroidPlugin(project)
            project.afterEvaluate {
                afterEvaluateBeforeAndroidPlugin(project)
            }
        }
    }

    @Override
    void initExtension(ComponentExtension componentExtension) {
        this.componentExtension = componentExtension
        sdk.initExtension(componentExtension)
        debug.initExtension(componentExtension)
        pins.initExtension(componentExtension)
    }

    @Override
    void evaluateBeforeAndroidPlugin(Project project) {
        pins.evaluateBeforeAndroidPlugin(project)
    }

    @Override
    void afterEvaluateBeforeAndroidPlugin(Project project) {
        pins.afterEvaluateBeforeAndroidPlugin(project)
    }

    @Override
    void evaluateAfterAndroidPlugin(Project project) {
        sdk.evaluateAfterAndroidPlugin(project)
        debug.evaluateAfterAndroidPlugin(project)
    }

    @Override
    void afterEvaluateAfterAndroidPlugin(Project project) {
        sdk.afterEvaluateAfterAndroidPlugin(project)
        debug.afterEvaluateAfterAndroidPlugin(project)
    }

    @Override
    void afterAllEvaluate(Project root) {
        sdk.afterAllEvaluate(root)
        debug.afterAllEvaluate(root)
        pins.afterAllEvaluate(root)
    }
}
