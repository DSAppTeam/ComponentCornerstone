package com.plugin.component

import com.android.build.gradle.AppPlugin
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.LibraryPlugin
import com.plugin.component.extension.ComponentExtension
import com.plugin.component.extension.PublicationManager
import com.plugin.component.extension.module.ProjectInfo
import com.plugin.component.extension.option.DependenciesOption
import com.plugin.component.extension.option.PublicationOption
import com.plugin.component.listener.OnModuleExtensionListener
import com.plugin.component.transform.InjectCodeTransform
import com.plugin.component.transform.ScanCodeTransform
import com.plugin.component.utils.JarUtil
import com.plugin.component.utils.ProjectUtil
import com.plugin.component.utils.PublicationUtil
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 *   ./gradlew --no-daemon ComponentPlugin  -Dorg.gradle.debug=true
 *   ./gradlew --no-daemon [clean, :app:generateDebugSources, :library:generateDebugSources, :module_lib:generateDebugSources]  -Dorg.gradle.debug=true
 *    ./gradlew --no-daemon :app:assemble  -Dorg.gradle.debug=true
 *  组件插件入口
 *  created by yummylau 2019/08/09
 */
class ComponentPlugin implements Plugin<Project> {

    private ComponentExtension mComponentExtension

    @Override
    void apply(Project project) {
        boolean isRoot = project == project.rootProject
        if (isRoot) {
            initPlugin(project)
            handleRootProject(project)
        } else {
            handleProject(project)
        }
    }

    private void handleProject(Project project) {

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
         * Syncing Gradle will evaluate the build files by comparing the current files to the project state that Gradle and Android Studio maintain.
         * If it finds any changes it will execute just those specific tasks.
         */
        //project 需要依赖 sdk 中声明的依赖，意味着同步的时候，component.gradle 中 sdk { dependencies { <xxxxx> }} 内容需要同步迁移到 project
        if (projectInfo.isSync()) {
            Logger.buildOutput("Syncing gradle...")
            publications.each {
                PublicationUtil.addPublicationDependencies(projectInfo, it)
            }
        }


        project.afterEvaluate {
            Logger.buildOutput("project[" + projectInfo.name + "] is modifying sdk SourceSet.")
            ProjectUtil.modifySourceSets(projectInfo)
            if (ProjectUtil.isProjectSame(projectInfo.name, Runtimes.sDebugModuleName)) {
                Logger.buildOutput("project[" + projectInfo.name + "] is debugModel,modifying DebugSets...")
                ProjectUtil.modifyDebugSets(projectInfo.project.rootProject, projectInfo)
            }

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
    }

    private initPlugin(Project project) {
        Runtimes.sSdkDir = new File(project.projectDir, Constants.SDK_DIR)
        Runtimes.sImplDir = new File(project.projectDir, Constants.IMPL_DIR)

        if (!Runtimes.sSdkDir.exists()) {
            Runtimes.sSdkDir.mkdirs()
            Logger.buildOutput("create File[" + Runtimes.sSdkDir.name + "]")
        }

        if (!Runtimes.sImplDir.exists()) {
            Runtimes.sImplDir.mkdirs()
            Logger.buildOutput("create File[" + Runtimes.sImplDir.name + "]")
        }

        ProjectUtil.getTasks(project).each {
            if (it == Constants.CLEAN) {
                if (!Runtimes.sSdkDir.deleteDir()) {
                    throw new RuntimeException("unable to delete dir " + Runtimes.sSdkDir.absolutePath)
                }
                if (!Runtimes.sImplDir.deleteDir()) {
                    throw new RuntimeException("unable to delete dir " + Runtimes.sImplDir.absolutePath)
                }
                Runtimes.sSdkDir.mkdirs()
                Logger.buildOutput("reset File[" + Runtimes.sSdkDir.name + "]")

                Runtimes.sImplDir.mkdirs()
                Logger.buildOutput("reset File[" + Runtimes.sImplDir.name + "]")
            }
        }
        project.repositories {
            flatDir {
                dirs Runtimes.sSdkDir.absolutePath
                Logger.buildOutput("flatDir Dir[" + Runtimes.sSdkDir.absolutePath + "]")

                dirs Runtimes.sImplDir.absolutePath
                Logger.buildOutput("flatDir Dir[" + Runtimes.sImplDir.absolutePath + "]")
            }
        }

    }


    private void handleRootProject(Project project) {
        PublicationManager.getInstance().loadManifest(project)
        mComponentExtension = project.getExtensions().create(Constants.COMPONENT, ComponentExtension, project, new OnModuleExtensionListener() {

            @Override
            void onPublicationOptionAdd(PublicationOption publication) {
                Project childProject = ProjectUtil.getProject(project, publication.name)
                if (childProject == null) {
                    Logger.buildOutput("publication's target[" + publication.name + "] does not exist!")
                } else {
                    if (publication.isSdk) {
                        Logger.buildOutput("publication's sdk[" + publication.name + "] is " + publication.groupId + ":" + publication.artifactId)
                        PublicationUtil.initPublication(childProject, publication)
                        PublicationManager.getInstance().addDependencyGraph(childProject.name, publication)
                        Runtimes.addSdkPublication(childProject.name, publication)
                    } else {
                        Logger.buildOutput("publication's impl[" + publication.name + "] is " + publication.groupId + ":" + publication.artifactId)
                        //todo 预留后续逻辑
                    }
                }
            }
        })

        //todo sdk中依赖sdk，需要特别区分，预留后续逻辑
        DependenciesOption.metaClass.component { String value ->
            return Constants.COMPONENT_PRE + value
        }

        project.afterEvaluate {

            Logger.buildOutput("")
            Logger.buildOutput("ComponentPlugin >>>>>>>>>> root#afterEvaluate")
            Runtimes.sAndroidJarPath = ProjectUtil.getAndroidJarPath(project, mComponentExtension.compileSdkVersion)
            Runtimes.setMainModuleName(mComponentExtension.mainModuleName)
            Runtimes.sDebugModuleName = mComponentExtension.debugModuleName
            Runtimes.sCompileSdkVersion = mComponentExtension.compileSdkVersion
            Runtimes.sCompileOption = mComponentExtension.compileOptions
            Runtimes.sDebugComponentName = mComponentExtension.debugComponentName
            project.extensions.add("debugComponentName",Runtimes.sDebugComponentName)

            Logger.buildOutput("component.gradle 配置信息 -->")
            Logger.buildOutput("AndroidJarPath", Runtimes.sAndroidJarPath)
            Logger.buildOutput("mainModuleName", Runtimes.getMainModuleName())
            Logger.buildOutput("debugModuleName", Runtimes.sDebugModuleName)
            Logger.buildOutput("debugComponentName", Runtimes.sDebugComponentName)
            Logger.buildOutput("compileSdkVersion", Runtimes.sCompileSdkVersion)
            Logger.buildOutput("CompileOption", "sourceCompatibility[" + Runtimes.sCompileOption.sourceCompatibility
                    + "] targetCompatibility[" + Runtimes.sCompileOption.targetCompatibility + "]")
            Logger.buildOutput("includes", mComponentExtension.includes)
            Logger.buildOutput("excludes", mComponentExtension.excludes)
            Set<String> includeModules = ProjectUtil.getModuleName(mComponentExtension.includes)
            Set<String> excludeModules = ProjectUtil.getModuleName(mComponentExtension.excludes)
            boolean includeModel = !includeModules.isEmpty()
            Logger.buildOutput("Select module by " + (includeModel ? "include" : "exclude"))
            Set<String> validModules = getValidComponents(project, includeModules, excludeModules, includeModel)
            Runtimes.sValidComponents = validModules
            Logger.buildOutput("生效模块", validModules.toList().toString())


            List<String> topSort = PublicationManager.getInstance().dependencyGraph.topSort()
            Collections.reverse(topSort)
            Logger.buildOutput("开始处理 jar...")
            topSort.each {
                PublicationOption publication = PublicationManager.getInstance().publicationDependencies.get(it)
                if (publication == null) {
                    return
                }
                Project childProject = project.findProject(publication.project)
                PublicationUtil.filterPublicationDependencies(publication)
                long startTime = System.nanoTime()
                if (publication.version != null) {
                    JarUtil.handleMavenJar(childProject, publication)
                    long currentTime = System.nanoTime()
                    Logger.buildOutput("Handle Maven jar " + PublicationUtil.getJarName(publication) + " cost " + (currentTime - startTime) + "ns")
                } else {
                    long currentTime = System.nanoTime()
                    JarUtil.handleLocalJar(childProject, publication)
                    Logger.buildOutput("Handle Local jar " + PublicationUtil.getJarName(publication) + " cost " + (currentTime - startTime) + "ns")
                }
                PublicationManager.getInstance().hitPublication(publication)
            }

            project.allprojects.each {
                if (it == project) return
                if (!validModules.contains(ProjectUtil.getProjectName(it))) return
                Project childProject = it
                Logger.buildOutput("")
                Logger.buildOutput("project[" + childProject.name + "] 配置信息 -->")
                ProjectInfo projectInfo = new ProjectInfo(childProject)
                childProject.repositories {
                    flatDir {
                        dirs Runtimes.sSdkDir.absolutePath
                        Logger.buildOutput("add flatDir Dir[" + Runtimes.sSdkDir.absolutePath + "]")
                        dirs Runtimes.sImplDir.absolutePath
                        Logger.buildOutput("add flatDir Dir[" + Runtimes.sImplDir.absolutePath + "]")
                    }
                }
                Runtimes.addProjectInfo(childProject.name, projectInfo)
                Logger.buildOutput("compileModuleName", projectInfo.compileModuleName)
                Logger.buildOutput("projectName", projectInfo.name)
                Logger.buildOutput("isDebugModule", projectInfo.isDebugModule())
                Logger.buildOutput("isMainModule", projectInfo.isMainModule())
                Logger.buildOutput("taskNames", projectInfo.taskNames)
                Logger.buildOutput("isSyncTask", projectInfo.isSync())
                Logger.buildOutput("isAssemble", projectInfo.isAssemble)
                Logger.buildOutput("isDebug", projectInfo.isDebug)


                childProject.plugins.whenObjectAdded {
                    if (it instanceof AppPlugin || it instanceof LibraryPlugin) {
                        Logger.buildOutput("project[" + childProject.name + "] whenObjectAdded AppPlugin or LibraryPlugin-->")
                        Logger.buildOutput("apply plugin: com.android.component")
                        childProject.pluginManager.apply(Constants.PLUGIN_COMPONENT)
                        childProject.dependencies {
                            Logger.buildOutput("add dependency: " + Constants.CORE_DEPENDENCY)
                            implementation Constants.CORE_DEPENDENCY
                        }
                        if (it instanceof AppPlugin) {
                            if (projectInfo.isDebugModule() || projectInfo.isMainModule()) {
                                Logger.buildOutput("plugin is AppPlugin and isDebugModule or isMainModule")
                                Logger.buildOutput("registerTransform", "ScanCodeTransform")
                                Logger.buildOutput("registerTransform", "InjectCodeTransform")
                                childProject.extensions.findByType(BaseExtension.class).registerTransform(new ScanCodeTransform(childProject))
                                childProject.extensions.findByType(BaseExtension.class).registerTransform(new InjectCodeTransform(childProject))
                            }
                        }
                    }
                }
            }
        }
    }

    private Set<String> getValidComponents(Project root, Set<String> includeModules, Set<String> excludeModules, boolean includeModel) {
        Set<String> result = new HashSet<>()
        root.allprojects.each {
            if (includeModel) {
                if (includeModules.contains(ProjectUtil.getProjectName(it))) {
                    result.add(it.name)
                }
            } else {
                if (!excludeModules.contains(ProjectUtil.getProjectName(it))) {
                    result.add(it.name)
                }
            }
        }
        return result
    }
}
