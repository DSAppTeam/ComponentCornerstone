package com.plugin.component.plugin


import com.plugin.component.ComponentPlugin
import com.plugin.component.Runtimes
import com.plugin.component.extension.PublicationManager
import com.plugin.component.extension.module.ProjectInfo
import com.plugin.component.extension.option.sdk.PublicationOption
import com.plugin.component.log.Logger
import com.plugin.component.log.MutLineLog
import com.plugin.component.utils.AarUtil
import com.plugin.component.utils.ProjectUtil
import com.plugin.component.utils.PublicationUtil
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.internal.artifacts.dependencies.DefaultProjectDependency
import org.gradle.api.internal.artifacts.dependencies.DefaultSelfResolvingDependency
import org.gradle.api.internal.file.collections.DefaultConfigurableFileCollection
import org.gradle.api.internal.file.collections.DefaultConfigurableFileTree

class SdkPlugin extends BasePlugin {

    @Override
    void evaluateAfterAndroidPlugin(Project project) {
        //针对每个module
        ProjectInfo projectInfo = Runtimes.getProjectInfo(project.name)

        //解析： component
        //example ：将component(':library') 转换为 ':com.effective.android-librarySdk:'
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
                Logger.buildOutput("添加依赖名字： " + PublicationUtil.getPublication(it))
                api PublicationUtil.getPublication(it)
            }
        }

        Logger.buildOutput("Syncing gradle...")
        /**
         * Syncing Gradle will evaluateAfterAndroidPlugin the build files by comparing the current files to the project state that Gradle and Android Studio maintain.
         * If it finds any changes it will execute just those specific tasks.
         */
        //project 需要依赖 sdk 中声明的依赖，意味着同步的时候，component.gradle 中 sdk { dependencies { <xxxxx> }} 内容需要同步迁移到 project
//        if (projectInfo.isSync()) {
//            Logger.buildOutput("Syncing gradle...")
        publications.each {
            PublicationUtil.addPublicationDependencies(projectInfo, it)
        }
//        }
    }


    @Override
    void afterEvaluateAfterAndroidPlugin(Project project, Project androidProject) {
        //整理资源路径
        ProjectInfo projectInfo = Runtimes.getProjectInfo(project.name)
        ProjectUtil.modifySourceSets(projectInfo)

    }


    @Override
    void afterAllEvaluate(Project root) {
//        handleImpAar(root)
        List<String> topSort = PublicationManager.getInstance().dependencyGraph.topSort()
        Collections.reverse(topSort)
        boolean isAssemble = false
        boolean isPublish = false
        topSort.each {
            PublicationOption publication = PublicationManager.getInstance().publicationDependencies.get(it)
            if (publication == null) {
                return
            }
            Project childProject = root.findProject(publication.project)
            ProjectInfo projectInfo = Runtimes.getProjectInfo(childProject.name)
            if (projectInfo.isAssemble) {
                isAssemble = true
                if (Runtimes.sSdkOption.isPublishMode) {
                    Runtimes.checkPublishEnable(childProject)
                }
                PublicationUtil.handleSdkPublish(childProject, ComponentPlugin.androidProject, publication, null)
                handleImpAar(childProject, publication)
            } else if (projectInfo.isPublish) {
                isPublish = true
                publication.impNeedPublish = true
                publication.sdkNeedPublish = true
                def publishTask = null
                if (projectInfo.isDebug) {
                    publishTask = root.tasks.named("ComponentPublishDebug")
                } else {
                    publishTask = root.tasks.named("ComponentPublishRelease")
                }
                PublicationUtil.handleSdkPublish(childProject, ComponentPlugin.androidProject, publication, publishTask.get())
                PublicationUtil.handleImplPublish(childProject, publication, projectInfo.isDebug, publishTask.get())
                PublicationManager.getInstance().addImpPublication(publication)
            }
        }
        if (isAssemble) {
            dealAssemble(root)
        } else if (isPublish) {
            dealPublish(root)
        } else {
            dealAssemble(root)
        }
    }

    private dealPublish(Project root) {
        MutLineLog mutLineLog = new MutLineLog()
        mutLineLog.build4("开始处理Publish场景")
        //收集所有模块的引用配置
        ArrayList<Configuration> allConfigList = new ArrayList<>()
        root.allprojects.each { project ->
            if (project == root) return
            if (!Runtimes.shouldApplyComponentPlugin(project)) return
            project.configurations.each { config ->
                if (config.name == "api" || config.name == "implementation"
                        || config.name == "compileOnly"
                        || config.name == "debugImplementation"
                        || config.name == "debugApi"
                        || config.name == "debugCompileOnly") {
                    allConfigList.add(config)
                }
            }
        }
        //收集project之间的引用依赖关系 收集被依赖的project，需要参与编译
        HashMap<Project, ArrayList<Configuration>> needCompileProjectConfigMap = new HashMap<>()
        HashMap<String, Map<String, String>> aarList = new HashMap<>()
        root.allprojects.each { project ->
            if (project == root) return
            allConfigList.each { config ->
                for (Dependency dependency : config.dependencies) {
                    if (dependency instanceof DefaultProjectDependency && dependency.dependencyProject.name == project.name) {
                        ArrayList<Configuration> configs = needCompileProjectConfigMap.get(project)
                        if (configs == null) {
                            configs = new ArrayList<>()
                            needCompileProjectConfigMap.put(project, configs)
                        }
                        configs.add(config)
                        //一个config只需要添加一次就好了
                        return
                    }
                }
            }
            PublicationOption publication = Runtimes.getImplPublication(project.name)
            if (publication != null && !publication.impNeedPublish) {
                ProjectInfo projectInfo = Runtimes.getProjectInfo(project.name)
                Map<String, String> map = new LinkedHashMap<>()
                map.put("path", PublicationUtil.getImpMavenGAV(publication, !projectInfo.isDebug))
                aarList.put(project.name, map)
            }
        }

        if (!aarList.isEmpty()) {
            root.allprojects.each { project ->
                if (project == root) return
                // 替换成aar文件
                aarList.each { map ->
                    //剔除所有配置中的不需要编译模块的依赖
                    project.configurations.each { config ->
                        config.dependencies.removeAll { dependency ->
                            dependency instanceof DefaultProjectDependency && dependency.dependencyProject.name == map.key
                        }
                    }
                    project.dependencies {
                        api map.value.get("path")
                    }
                }
            }
            aarList.each { map ->
                allConfigList.each { config ->
                    config.dependencies.removeAll { dependency ->
                        dependency instanceof DefaultProjectDependency && dependency.dependencyProject.name == map.key
                    }
                }
            }
            List<Map<String, Object>> needAndDependency = new ArrayList<>()
            //分离所有dependency 进行预处理
            allConfigList.each { config ->
                config.dependencies.each { dependency ->
                    if (dependency instanceof DefaultProjectDependency) {
                        def dependencyClone = dependency.copy()
                        dependencyClone.targetConfiguration = null
                        Map<String, Object> map = new HashMap<>()
                        map.put(config.name, dependencyClone)
                        needAndDependency.add(map)
                    } else {
                        if (dependency instanceof DefaultSelfResolvingDependency && (dependency.files instanceof DefaultConfigurableFileCollection || dependency.files instanceof DefaultConfigurableFileTree)) {
                            // 这里的依赖是以下两种： 无需添加在 编译project ，因为 jar 包直接进入 自身的 aar 中的libs 文件夹
                            //    implementation rootProject.files("libs/*.jar")
                            //    implementation fileTree(dir: "libs", include: ["*.jar"])

                        } else {
                            Map<String, Object> map = new HashMap<>()
                            map.put(config.name, dependency)
                            needAndDependency.add(map)
                        }
                    }

                }
            }
            //遍历所有需要编译的project 将引用传递至需要编译的project中，module引用只需要传递api方式引用的
            needCompileProjectConfigMap.each { entry ->
                if (entry.key == root) return
                needAndDependency.each { map ->
                    Map.Entry<String, Object> config = map.entrySet().first()
                    Dependency dependency = config.value
                    if (config.key == "api") {
                        if (dependency instanceof DefaultProjectDependency && dependency.dependencyProject.name == entry.key.name) {
                            return
                        }
                        entry.key.dependencies.add(config.key, config.value)
                    } else {
                        //不传递implement project
                        if (dependency instanceof DefaultProjectDependency) {
                            return
                        }
                        entry.key.dependencies.add(config.key, config.value)
                    }
                }
            }
        }
        Logger.buildBlockLog("处理Publish场景", mutLineLog)
    }

    private static void handleImpAar(Project project, PublicationOption publication) {
        ProjectInfo projectInfo = Runtimes.getProjectInfo(project.name)
        if (projectInfo == null) {
            return
        }
        if (publication != null && !publication.invalid && ComponentPlugin.androidProject != null) {
            boolean hasModify = PublicationManager.getInstance().hasImpModified(publication)
            boolean hasGitDiff = PublicationManager.getInstance().hasModifyWithGitDiff(publication.impSourceSet)
            File impFile = new File(Runtimes.sImplDir, PublicationUtil.getAarName(publication))
            if (!publication.forceLocal && publication.implVersion != null) {
                long startTime = System.currentTimeMillis()
                boolean isAarExists = AarUtil.isArrExits(project, publication, !projectInfo.isDebug)
                long cost = System.currentTimeMillis() - startTime
                if (isAarExists && !hasGitDiff) {
                    publication.impNeedPack = false
                    publication.useLocalImp = false
                    PublicationManager.getInstance().addImpPublication(publication)
                    Logger.buildOutput("Handle Maven Imp ${publication.name} cost ${cost}ms. aar exists.No need to pack")
                } else if (!hasModify && impFile.exists()) {
                    publication.impNeedPack = false
                    publication.useLocalImp = true
                    PublicationManager.getInstance().addImpPublication(publication)
                    Logger.buildOutput("Handle Local Imp ${publication.name} cost ${cost}ms. aar not exists but not modify and local file exists.No need to pack")
                } else {
                    publication.impNeedPack = true
                    publication.useLocalImp = true
                    if (projectInfo.isDebug) {
                        //注册打包arr task
                        AarUtil.packImpAar(project, ComponentPlugin.androidProject, publication, null)
                    }
                    Logger.buildOutput("Handle Local Imp ${publication.name} cost ${cost}ms. Need pack")
                }
            } else {
                Logger.buildOutput("Handle Local Imp ${publication.name}.")
                if (publication.impNeedPublish) {
                    boolean isAarExists = AarUtil.isArrExits(project, publication, !projectInfo.isDebug)
                    publication.useLocalImp = !isAarExists
                    publication.impNeedPack = hasModify || !isAarExists
                    if (!isAarExists) {
                        PublicationUtil.handleImplPublish(project, publication, projectInfo.isDebug, null)
                    } else {
                        publication.impNeedPublish = false
                    }
                    PublicationManager.getInstance().addImpPublication(publication)
                } else {
                    if (projectInfo.isDebug) {
                        publication.useLocalImp = true
                        if (hasModify) {
                            publication.impNeedPack = true
                            //注册打包arr task
                            AarUtil.packImpAar(project, ComponentPlugin.androidProject, publication, null)
                        } else {
                            publication.impNeedPack = false
                        }
                        PublicationManager.getInstance().addImpPublication(publication)
                    }
                }
            }
        }
    }

    private void dealAssemble(Project root) {
        //处理循环依赖
        ProjectInfo compileProject = Runtimes.getCompileProjectWhenAssemble()
        MutLineLog mutLineLog = new MutLineLog()
        mutLineLog.build4("开始处理Assemble场景 compileProject:${compileProject}")
        if (compileProject != null) {
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
                hasResolve.each {
                    Logger.buildOutput("resolve project:${compileProject.name} realDependency:${it}")
                }
                if (!hasResolve.isEmpty()) {
                    handleCompileModuleDependency(compileProject, root, hasResolve, mutLineLog)
                }
            }
        }
        Logger.buildBlockLog("单 ASSEMBLE 场景处理循环依赖", mutLineLog)
    }

    private void handleCompileModuleDependency(ProjectInfo compileProject, Project root, HashSet<String> hasResolve, MutLineLog mutLineLog) {

        //收集所有模块的引用配置
        ArrayList<Configuration> allConfigList = new ArrayList<>()
        root.allprojects.each { project ->
            if (project == root) return
            if (!Runtimes.shouldApplyComponentPlugin(project)) return
            project.configurations.each { config ->
                if (config.name == "api" || config.name == "implementation"
                        || config.name == "compileOnly"
                        || config.name == "debugImplementation"
                        || config.name == "debugApi"
                        || config.name == "debugCompileOnly") {
                    allConfigList.add(config)
                }
            }
        }
        //收集project之间的引用依赖关系 收集被依赖的project，需要参与编译
        HashMap<Project, ArrayList<Configuration>> needCompileProjectConfigMap = new HashMap<>()
        root.allprojects.each { project ->
            if (project == root) return
            allConfigList.each { config ->
                for (Dependency dependency : config.dependencies) {
                    if (dependency instanceof DefaultProjectDependency && dependency.dependencyProject.name == project.name) {
                        ArrayList<Configuration> configs = needCompileProjectConfigMap.get(project)
                        if (configs == null) {
                            configs = new ArrayList<>()
                            needCompileProjectConfigMap.put(project, configs)
                        }
                        configs.add(config)
                        //一个config只需要添加一次就好了
                        return
                    }
                }
            }
        }
        //剔除被收集的编译工程的引用
        allConfigList.clear()
        root.allprojects.each { project ->
            if (project == root) return
            if (!Runtimes.shouldApplyComponentPlugin(project)) return
            project.configurations.each { config ->
                if (config.name == "api" || config.name == "implementation"
                        || config.name == "compileOnly"
                        || config.name == "debugImplementation"
                        || config.name == "debugApi"
                        || config.name == "debugCompileOnly") {
                    if (compileProject.project != project)
                        allConfigList.add(config)
                }
            }
        }
        HashMap<String, Map<String, String>> aarList = new HashMap<>()
        StringBuilder stringBuilder = new StringBuilder()
        for (String realDependency : hasResolve) {
            PublicationOption publication = Runtimes.getImplPublication(realDependency)
            Logger.buildOutput("resolve publication ${publication}")
            if (publication != null && compileProject.isDebug) {
                if (publication.impNeedPack) {
                    stringBuilder.append(" :project(")
                    stringBuilder.append(realDependency)
                    stringBuilder.append(")")
                    compileProject.project.dependencies {
                        implementation compileProject.project.project(":" + realDependency)
                    }
                    needCompileProjectConfigMap.put(Runtimes.getProjectInfo(realDependency).project, new ArrayList<Configuration>())
                } else {
                    Map<String, String> map = new LinkedHashMap<>()
                    if (publication.useLocalImp) {
                        stringBuilder.append("${PublicationUtil.getAarName(publication)} ")
                        map.put("name", publication.groupId + '-' + publication.artifactId + '-imp')
                        map.put("ext", "aar")
                    } else {
                        stringBuilder.append("${PublicationUtil.getImpMavenGAV(publication, !compileProject.isDebug)} ")
                        map.put("path", PublicationUtil.getImpMavenGAV(publication, !compileProject.isDebug))
                    }
                    aarList.put(realDependency, map)
                }
                PublicationManager.getInstance().addImpPublication(publication)
            } else {
                stringBuilder.append(" :project(")
                stringBuilder.append(realDependency)
                stringBuilder.append(")")
                compileProject.project.dependencies {
                    implementation compileProject.project.project(":" + realDependency)
                }
                needCompileProjectConfigMap.put(Runtimes.getProjectInfo(realDependency).project, new ArrayList<Configuration>())
            }
        }
        mutLineLog.build4("application[" + compileProject.name + "] component 合并依赖 = " + stringBuilder.toString())
        if (compileProject.isDebug && !aarList.isEmpty()) {
            root.allprojects.each { project ->
                if (project == root) return
                Logger.buildOutput("replace aar：project: $project.name")
                // 替换成aar文件
                aarList.each { map ->
                    //剔除所有配置中的不需要编译模块的依赖
                    project.configurations.each { config ->
                        config.dependencies.removeAll { dependency ->
                            dependency instanceof DefaultProjectDependency && dependency.dependencyProject.name == map.key
                        }
                    }
                    project.dependencies {
                        if (map.getValue().size() > 1) {
                            api map.value
                        } else {
                            api map.value.get("path")
                        }
                    }
                }
            }
            aarList.each { map ->
                allConfigList.each { config ->
                    config.dependencies.removeAll { dependency ->
                        dependency instanceof DefaultProjectDependency && dependency.dependencyProject.name == map.key
                    }
                }
            }
            List<Map<String, Object>> needAndDependency = new ArrayList<>()
            //分离所有dependency 进行预处理
            allConfigList.each { config ->
                config.dependencies.each { dependency ->
                    if (dependency instanceof DefaultProjectDependency) {
                        def dependencyClone = dependency.copy()
                        dependencyClone.targetConfiguration = null
                        Map<String, Object> map = new HashMap<>()
                        map.put(config.name, dependencyClone)
                        needAndDependency.add(map)
                    } else {
                        if (dependency instanceof DefaultSelfResolvingDependency && (dependency.files instanceof DefaultConfigurableFileCollection || dependency.files instanceof DefaultConfigurableFileTree)) {
                            // 这里的依赖是以下两种： 无需添加在 编译project ，因为 jar 包直接进入 自身的 aar 中的libs 文件夹
                            //    implementation rootProject.files("libs/*.jar")
                            //    implementation fileTree(dir: "libs", include: ["*.jar"])

                        } else {
                            Map<String, Object> map = new HashMap<>()
                            map.put(config.name, dependency)
                            needAndDependency.add(map)
                        }
                    }

                }
            }
            //遍历所有需要编译的project 将引用传递至需要编译的project中，module引用只需要传递api方式引用的
            needCompileProjectConfigMap.each { entry ->
                if (entry.key == root) return
                needAndDependency.each { map ->
                    Map.Entry<String, Object> config = map.entrySet().first()
                    Dependency dependency = config.value
                    if (config.key == "api") {
                        if (dependency instanceof DefaultProjectDependency && dependency.dependencyProject.name == entry.key.name) {
                            return
                        }
                        compileProject.project.dependencies.add(config.key, config.value)
                        entry.key.dependencies.add(config.key, config.value)
                    } else {
                        //不传递implement project
                        if (dependency instanceof DefaultProjectDependency) {
                            return
                        }
                        compileProject.project.dependencies.add(config.key, config.value)
//                                    entry.key.dependencies.add(config.key, config.value)
                    }
                }
            }
        }

    }
}
