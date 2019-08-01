package com.plugin.module

import com.android.build.gradle.BaseExtension
import com.plugin.module.extension.module.AloneConfiguration
import com.plugin.module.extension.module.AssembleTask
import com.plugin.module.transform.CodeTransform
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * 编写 AlonePlugin 插件
 *  ./gradlew --no-daemon checkGradleDependencies  -Dorg.gradle.debug=true
 */
class AlonePlugin implements Plugin<Project> {

    String compileModule = Constants.DEFAULT_APP_NAME

    void apply(Project project) {

        project.getExtensions().create("runalone", AloneConfiguration)

        def misScript = new File(project.projectDir, 'module.gradle')
        if (misScript.exists()) {
            project.apply from: misScript
        } else {
            throw new RuntimeException("找不到 " + project.name + "下的module.gradle ！")
        }

        //获取运行task名称
        String taskNames = project.gradle.startParameter.taskNames.toString()
        Logger.buildOutput("taskNames is " + taskNames)

        //获取运行模块名称
        String module = project.path.replace(":", "")
        Logger.buildOutput("current module is " + module)

        //解析AssembleTask
        Logger.buildOutput("startParameter.taskNames is " + project.gradle.startParameter.taskNames)
        AssembleTask assembleTask = com.plugin.module.utils.Utils.parseTaskInfo(project.gradle.startParameter.taskNames)
        if (assembleTask.isAssemble) {
            compileModule = com.plugin.module.utils.Utils.parseMainModuleName(project, assembleTask)
            Logger.buildOutput("compile module is : " + compileModule)
        }

        //需要在特定的模块中声明 isRunAlone，用于判断是否单独运行
        if (!project.hasProperty(Constants.PROPERTIES_ISRUNALONE)) {
            throw new RuntimeException("you should set isRunAlone in " + module + "'s gradle.properties")
        }

//        boolean isRunAlone = extension.runAlone


        boolean isRunAlone = Boolean.parseBoolean((project.properties.get("isRunAlone")))
        String mainModuleName = project.rootProject.property(Constants.PROPERTIES_MAIN_MODULE_NAME)


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
        Logger.buildOutput("setProperty isRunAlone(" + isRunAlone + ")")

        //根据配置添加各种组件依赖，并且自动化生成组件加载代码
        if (isRunAlone) {
            project.apply plugin: Constants.PLUGIN_APPLICATION
            Logger.buildOutput("project.apply plugin: com.android.application")

            //对于组件，则需要读取alone目录进行运行
            if (!module.equals(mainModuleName)) {

                project.android.sourceSets {
                    main {
                        manifest.srcFile Constants.AFTER_MANIFEST_PATH
                        java.srcDirs = [Constants.JAVA_PATH, Constants.AFTER_JAVA_PATH, Constants.MIS_JAVA_PATH]
                        res.srcDirs = [Constants.RES_PATH, Constants.AFTER_RES_PATH]
                        assets.srcDirs = [Constants.ASSETS_PATH, Constants.AFTER_ASSETS_PATH]
                        jniLibs.srcDirs = [Constants.JNILIBS_PATH, Constants.AFTER_JNILIBS_PATH]
                    }
                }

            }
            if (assembleTask.isAssemble && module.equals(compileModule)) {
                com.plugin.module.utils.Utils.compileComponents(project, assembleTask)
                //参考https://github.com/luojilab/DDComponentForAndroid/issues/122
                project.extensions.findByType(BaseExtension.class).registerTransform(new CodeTransform(project));
//                project.android.registerTransform(new CodeTransform(project))
            }
        } else {
            project.apply plugin: Constants.PLUGIN_LIBRARY
            Logger.buildOutput("project.apply plugin: com.android.library")
        }
    }
}