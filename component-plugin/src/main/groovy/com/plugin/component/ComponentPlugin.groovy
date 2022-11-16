package com.plugin.component

import com.plugin.component.extension.ComponentExtension
import com.plugin.component.log.Logger
import com.plugin.component.plugin.AbsPlugin
import com.plugin.component.plugin.DebugPlugin
import com.plugin.component.plugin.PinPlugin
import com.plugin.component.plugin.SdkPlugin
import com.plugin.component.utils.ProjectUtil
import org.gradle.BuildListener
import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.internal.dispatch.MethodInvocation
import org.gradle.internal.event.BroadcastDispatch
import org.gradle.internal.event.ListenerBroadcast
import org.gradle.invocation.DefaultGradle
import org.gradle.listener.ClosureBackedMethodInvocationDispatch

import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Modifier

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
    private boolean isFirst = true
    static Project androidProject
    static Project rootProject

    @Override
    void apply(Project project) {
        if (project == project.rootProject) {
            rootProject = project.rootProject
            componentExtension = project.getExtensions().create(Constants.COMPONENT, ComponentExtension, project)
            initExtension(componentExtension)
            Runtimes.initRuntimeConfigurationOnEvaluate(project)
            project.afterEvaluate {
                androidProject = ProjectUtil.getProject(project, componentExtension.appModule)
                Runtimes.initRuntimeConfigurationAfterEvaluate(project, componentExtension)
                Runtimes.hookAfterApplyingAndroidPlugin(project, androidProject, this)
                Runtimes.injectComponentPlugin(project)
                Runtimes.registerPublishTask(project)
            }
//            def projectsEvaluatedList = Runtimes.hookProjectsEvaluatedAction(project)
            project.gradle.projectsEvaluated {
                if (isFirst) {
                    isFirst = false
                    //修改引用
                    afterAllEvaluate(project)
//                    //后执行移除的监听（主要调整执行顺序，重依赖才能生效和不报错，可能有AGP 版本兼容问题）
//                    Class clazz = Class.forName("org.gradle.api.invocation.Gradle")
//                    Method method = clazz.getDeclaredMethod("projectsEvaluated", Action.class)
//                    Object[] objects = [it]
//                    MethodInvocation mMethodInvocation = new MethodInvocation(method, objects)
//                    projectsEvaluatedList.forEach {
//                        it.dispatch(mMethodInvocation)
//                    }
                }
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
    void afterEvaluateAfterAndroidPlugin(Project project, Project androidProject) {
        sdk.afterEvaluateAfterAndroidPlugin(project, androidProject)
        debug.afterEvaluateAfterAndroidPlugin(project, androidProject)
    }

    @Override
    void afterAllEvaluate(Project root) {
        sdk.afterAllEvaluate(root)
        debug.afterAllEvaluate(root)
        pins.afterAllEvaluate(root)
    }
}
