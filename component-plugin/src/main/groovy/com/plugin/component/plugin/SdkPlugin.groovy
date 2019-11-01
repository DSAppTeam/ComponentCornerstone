package com.plugin.component.plugin

import com.plugin.component.log.Logger
import com.plugin.component.Runtimes
import com.plugin.component.extension.PublicationManager
import com.plugin.component.extension.module.ProjectInfo
import com.plugin.component.extension.option.sdk.PublicationOption
import com.plugin.component.log.MutLineLog
import com.plugin.component.utils.ProjectUtil
import com.plugin.component.utils.PublicationUtil
import org.gradle.api.Project

class SdkPlugin extends BasePlugin {

    @Override
    void evaluateAfterAndroidPlugin(Project project) {
        ProjectInfo projectInfo = Runtimes.getProjectInfo(project.name)

        //解析： component
        //example ：component(':library')
        //规则：
        // 1.  component 对象必须为实现 component 插件的project
        // 2.  component(<project>) project 逻辑上被划分为 impl / debug / sdk，其中 sdk 通过 api 暴露给上层，impl 直接打包
        project.dependencies.metaClass.component { String value ->
            return PublicationUtil.parseComponent(projectInfo, value)
        }

        //project 需要使用 api 依赖并暴露 sdk，解决 project 被依赖的时候，依赖者可以引用到 project sdk
        List<PublicationOption> publications = PublicationManager.getInstance().getPublicationByProject(project)
        project.dependencies {
            publications.each {
                api PublicationUtil.getPublication(it)
            }
        }

        /**
         * Syncing Gradle will evaluateAfterAndroidPlugin the build files by comparing the current files to the project state that Gradle and Android Studio maintain.
         * If it finds any changes it will execute just those specific tasks.
         */
        //project 需要依赖 sdk 中声明的依赖，意味着同步的时候，component.gradle 中 sdk { dependencies { <xxxxx> }} 内容需要同步迁移到 project
        if (projectInfo.isSync()) {
            Logger.buildOutput("Syncing gradle...")
            publications.each {
                PublicationUtil.addPublicationDependencies(projectInfo, it)
            }
        }
    }


    @Override
    void afterEvaluateAfterAndroidPlugin(Project project) {
        ProjectInfo projectInfo = Runtimes.getProjectInfo(project.name)
        ProjectUtil.modifySourceSets(projectInfo)

        //todo 发布
//            List<PublicationOption> publicationList = PublicationManager.getInstance().getPublicationByProject(project)
//            List<PublicationOption> publicationPublishList = new ArrayList<>()
//            publicationList.each {
//                if (it.version != null) {
//                    publicationPublishList.add(it)
//                }
//            }
//
//            if (publicationPublishList.size() > 0) {
//                project.plugins.apply(Constants.PLUGIN_MAVEN_PUBLISH)
//                def publishing = project.extensions.getByName(Constants.PUBLISHING)
//                if (mComponentExtension != null && mComponentExtension.configure != null) {
//                    publishing.repositories mComponentExtension.configure
//                }
//
//                publicationPublishList.each {
//                    PublicationUtil.createPublishTask(project, it)
//                }
//            }
    }


    @Override
    void afterAllEvaluate(Project root) {
        ProjectInfo compileProject = Runtimes.getCompileProjectWhenAssemble()
        if (compileProject != null) {

            MutLineLog mutLineLog = new MutLineLog()
            if (Runtimes.sAssembleModules.size() > 1) {
                mutLineLog.build4("task has one more assemble module,skip transforming [component] to [project]")
            } else {
                mutLineLog.build4("assemble project = " + compileProject.name)
                Set<String> hasResolve = new HashSet<>()
                Set<String> currentDependencies = new HashSet<>()
                Set<String> nextDependencies = new HashSet<>()
                currentDependencies.addAll(compileProject.componentDependencies)
                mutLineLog.build4("project[" + compileProject.name + "] component 依赖 = " + compileProject.getComponentDependenciesString())

                while (!currentDependencies.isEmpty()) {
                    for (String string : currentDependencies) {
                        ProjectInfo projectInfo = Runtimes.getProjectInfo(string)
                        if (projectInfo == null) {
                            continue
                        }
                        String name = projectInfo.name
                        if (!hasResolve.contains(name)) {
                            hasResolve.add(name)
                            nextDependencies.addAll(projectInfo.componentDependencies)
                            mutLineLog.build4("project[" + projectInfo.name + "] component 依赖 = " + projectInfo.getComponentDependenciesString())
                        }
                    }
                    currentDependencies.clear()
                    currentDependencies.addAll(nextDependencies)
                    nextDependencies.clear()
                }

                if (!hasResolve.isEmpty()) {
                    StringBuilder stringBuilder = new StringBuilder()
                    for (String realDependency : hasResolve) {
                        stringBuilder.append(" :project(")
                        stringBuilder.append(realDependency)
                        stringBuilder.append(")")
                        compileProject.project.dependencies {
                            implementation compileProject.project.project(":" + realDependency)
                        }
                    }
                    mutLineLog.build4("application[" + compileProject.name + "] component 合并依赖 = " + stringBuilder.toString())
                }
            }
            Logger.buildBlockLog("单 ASSEMBLE 场景处理循环依赖", mutLineLog)
        }
    }
}
