package com.plugin.component

import com.android.build.gradle.AppPlugin
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.LibraryPlugin
import com.plugin.component.extension.ComponentExtension
import com.plugin.component.extension.PublicationManager
import com.plugin.component.extension.module.ProjectInfo
import com.plugin.component.extension.option.DebugOption
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

import javax.print.attribute.standard.PrinterURI

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
            ProjectUtil.modifySourceSets(projectInfo)

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
            void onPublicationImplOptionAdded(Project childProject, PublicationOption publicationOption) {
                if (!publication.isSdk) {
                    //预留后续逻辑
                }
            }

            void onPublicationSdkOptionAdded(Project childProject, PublicationOption publication) {
                if (publication.isSdk) {
                    PublicationUtil.initPublication(childProject, publication)
                    Runtimes.addSdkPublication(childProject.name, publication)
                    PublicationManager.getInstance().addDependencyGraph(childProject.name, publication)
                }
            }

            @Override
            void onDebugOptionAdded(Project childProject, DebugOption debugOption) {
                Runtimes.addDebugInfo(debugOption.name, debugOption)
            }
        })

        DependenciesOption.metaClass.component { String value ->
            return Constants.COMPONENT_PRE + value
        }

        project.afterEvaluate {

            Logger.buildOutput("ComponentPlugin >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>")
            Runtimes.sAndroidJarPath = ProjectUtil.getAndroidJarPath(project, mComponentExtension.compileSdkVersion)
            Runtimes.sMainModuleName = mComponentExtension.mainModuleName
            Runtimes.sDebugModuleName = mComponentExtension.debugModuleName
            Runtimes.sCompileSdkVersion = mComponentExtension.compileSdkVersion
            Runtimes.sCompileOption = mComponentExtension.compileOptions

            Logger.buildOutput("Extension-AndroidJarPath", Runtimes.sAndroidJarPath)
            Logger.buildOutput("Extension-mainModuleName", Runtimes.sMainModuleName)
            Logger.buildOutput("Extension-compileSdkVersion", Runtimes.sCompileSdkVersion)
            Logger.buildOutput("Extension-CompileOption", "sourceCompatibility[" + Runtimes.sCompileOption.sourceCompatibility
                    + "] targetCompatibility[" + Runtimes.sCompileOption.targetCompatibility + "]")
            Logger.buildOutput("Extension-includes", mComponentExtension.includes)
            Logger.buildOutput("Extension-excludes", mComponentExtension.excludes)
            Set<String> includeModules = ProjectUtil.getModuleName(mComponentExtension.includes)
            Set<String> excludeModules = ProjectUtil.getModuleName(mComponentExtension.excludes)
            boolean includeModel = !includeModules.isEmpty()
            Logger.buildOutput("Select module by " + (includeModel ? "include" : "exclude"))
            Logger.buildOutput("Start apply from sub project' component.gradle")
            project.allprojects.each {
                if (it == project) return
                if (!isValidPluginModule(it, includeModules, excludeModules, includeModel)) return
                Project childProject = it
                File moduleScript = new File(childProject.projectDir, Constants.COMPONENT_SCRIPT)
                if (moduleScript.exists()) {
                    mComponentExtension.currentChildProject = childProject
                    project.apply from: moduleScript
                    Logger.buildOutput("apply from " + moduleScript.path)
                }
            }

            Logger.buildOutput("start package sdk jar")
            List<String> topSort = PublicationManager.getInstance().dependencyGraph.topSort()
            Collections.reverse(topSort)
            topSort.each {
                PublicationOption publication = PublicationManager.getInstance().publicationDependencies.get(it)
                if (publication == null) {
                    return
                }
                Project childProject = project.findProject(publication.project)
                PublicationUtil.filterPublicationDependencies(publication)
                if (publication.version != null) {
                    JarUtil.handleMavenJar(childProject, publication)
                    Logger.buildOutput("Handle Maven jar " + PublicationUtil.getJarName(publication))
                } else {
                    JarUtil.handleLocalJar(childProject, publication)
                    Logger.buildOutput("Handle Local jar " + PublicationUtil.getJarName(publication))
                }
                PublicationManager.getInstance().hitPublication(publication)
            }

            project.allprojects.each {
                if (it == project) return
                if (!isValidPluginModule(it, includeModules, excludeModules, includeModel)) return
                Project childProject = it
                Logger.buildOutput("")
                Logger.buildOutput("project[" + childProject.name + "] >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>")
                ProjectInfo projectInfo = new ProjectInfo(childProject)
                if (projectInfo.isVailModulePluginTarget) {

                    childProject.repositories {
                        flatDir {
                            dirs Runtimes.sSdkDir.absolutePath
                            Logger.buildOutput("project[" + childProject.name + "]" + "-flatDir Dir[" + Runtimes.sSdkDir.absolutePath + "]")
                            dirs Runtimes.sImplDir.absolutePath
                            Logger.buildOutput("project[" + childProject.name + "]" + "-flatDir Dir[" + Runtimes.sImplDir.absolutePath + "]")
                        }
                    }
                    Runtimes.addProjectInfo(childProject.name, projectInfo)
                    Logger.buildOutput("project[" + childProject.name + "]" + "compileModuleName", projectInfo.compileModuleName)
                    Logger.buildOutput("project[" + childProject.name + "]" + "taskNames", projectInfo.taskNames)
                    Logger.buildOutput("project[" + childProject.name + "]" + "moduleName", projectInfo.currentModuleName)
                    Logger.buildOutput("project[" + childProject.name + "]" + "isSyncTask", projectInfo.isSync())
                    Logger.buildOutput("project[" + childProject.name + "]" + "aloneEnable", projectInfo.aloneEnable)

                    childProject.plugins.whenObjectAdded {
                        if (it instanceof AppPlugin || it instanceof LibraryPlugin) {
                            Logger.buildOutput("project[" + childProject.name + "]" + "whenObjectAdded(" + it + ")")
                            Logger.buildOutput("project[" + childProject.name + "]" + "apply plugin: com.android.component")
                            childProject.pluginManager.apply(Constants.PLUGIN_COMPONENT)
                            childProject.dependencies {
                                implementation Constants.CORE_DEPENDENCY
                            }

                            if (it instanceof AppPlugin) {
                                if (projectInfo.isDebugModule() || projectInfo.isMainModule()) {
                                    Logger.buildOutput("project[" + childProject.name + "] registerTransform => ScanCodeTransform")
                                    Logger.buildOutput("project[" + childProject.name + "] registerTransform => InjectCodeTransform")
                                    childProject.extensions.findByType(BaseExtension.class).registerTransform(new ScanCodeTransform(childProject))
                                    childProject.extensions.findByType(BaseExtension.class).registerTransform(new InjectCodeTransform(childProject))
                                }
                            }

                            if (projectInfo.isDebugModule()) {
                                ProjectUtil.modifyDebugSets(projectInfo)
                            }
                        }
                    }
                } else {
                    Logger.buildOutput("project[" + childProject.name + "]" + "can't apply component plugin")
                }
            }
        }

        project.getGradle().projectsEvaluated {
            project.allprojects.each {
                if (it == project) return
                ProjectInfo projectInfo = Runtimes.getProjectInfo(Runtimes.sDebugModuleName)
                if (projectInfo != null && projectInfo.isDebugModule()) {
                    ProjectUtil.modifyDebugSets(projectInfo)
                }
            }
        }
    }

    private boolean isValidPluginModule(Project project, Set<String> includeModules, Set<String> excludeModules, boolean includeModel) {
        if (includeModel) {
            return includeModules.contains(project.name)
        } else {
            return !excludeModules.contains(project.name)
        }
    }
}
