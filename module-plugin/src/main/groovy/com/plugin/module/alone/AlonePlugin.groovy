package com.plugin.module.alone

import com.android.build.gradle.BaseExtension
import com.plugin.module.Constants
import com.plugin.module.Utils
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * 编写 AlonePlugin 插件
 */
class AlonePlugin implements Plugin<Project> {

    String compileModule = Constants.DEFAULT_APP_NAME

    void apply(Project project) {

        //解析ComExtension属性
        Extension extension = project.extensions.create(Constants.EXTENSION_NAME, Extension)

        //获取运行task名称
        String taskNames = project.gradle.startParameter.taskNames.toString()
        Utils.buildOutput("taskNames is " + taskNames)

        //获取运行模块名称
        String module = project.path.replace(":", "")
        Utils.buildOutput("current module is " + module)

        //解析AssembleTask
        Utils.buildOutput("startParameter.taskNames is " + project.gradle.startParameter.taskNames)
        AssembleTask assembleTask = Utils.parseTaskInfo(project.gradle.startParameter.taskNames)
        if (assembleTask.isAssemble) {
            compileModule = Utils.parseMainModuleName(project, assembleTask)
            Utils.buildOutput("compile module is : " + compileModule)
        }

        //需要在特定的模块中声明 isRunAlone，用于判断是否单独运行
        if (!project.hasProperty(Constants.PROPERTIES_ISRUNALONE)) {
            throw new RuntimeException("you should set isRunAlone in " + module + "'s gradle.properties")
        }

        boolean isRunAlone = extension.runAlone



//        String mainModuleName = project.rootProject.property(Constants.PROPERTIES_MAIN_MODULE_NAME)


        //当且仅当 isRunAlone 为ture需要判断
        if (isRunAlone && assembleTask.isAssemble) {
            //如果运行的模块就是app模块，或者当前运行的模块就是我们配置的mainmodulename，则默认需要单独运行，其他组件强制修改为false
            if (module.equals(compileModule)) {
                isRunAlone = true
            } else {
                isRunAlone = false
            }
        }
        project.setProperty("isRunAlone", isRunAlone)
        Utils.buildOutput("setProperty isRunAlone(" + isRunAlone + ")")

        //根据配置添加各种组件依赖，并且自动化生成组件加载代码
        if (isRunAlone) {
            project.apply plugin: Constants.PLUGIN_APPLICATION
            Utils.buildOutput("project.apply plugin: com.android.application")

            //对于组件，则需要读取alone目录进行运行
//            if (!module.equals(mainModuleName)) {

//            }

            project.android.sourceSets {
                main {
                    manifest.srcFile Constants.AFTER_MANIFEST_PATH
                    java.srcDirs = [Constants.JAVA_PATH, Constants.AFTER_JAVA_PATH]
                    res.srcDirs = [Constants.RES_PATH, Constants.AFTER_RES_PATH]
                    assets.srcDirs = [Constants.ASSETS_PATH, Constants.AFTER_ASSETS_PATH]
                    jniLibs.srcDirs = [Constants.JNILIBS_PATH, Constants.AFTER_JNILIBS_PATH]
                }
            }
            if (assembleTask.isAssemble && module.equals(compileModule)) {
                Utils.compileComponents(project, assembleTask)
                //参考https://github.com/luojilab/DDComponentForAndroid/issues/122
                project.extensions.findByType(BaseExtension.class).registerTransform(new CodeTransform(project));
//                project.android.registerTransform(new CodeTransform(project))
            }
        } else {
            project.apply plugin: Constants.PLUGIN_LIBRARY
            Utils.buildOutput("project.apply plugin: com.android.library")
        }
    }
}