package com.plugin.component

import com.android.build.gradle.AppPlugin
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.LibraryPlugin
import com.plugin.component.extension.option.DependenciesOption
import com.plugin.component.extension.module.ProjectInfo
import com.plugin.component.extension.option.PublicationOption
import com.plugin.component.extension.option.DebugOption
import com.plugin.component.listener.OnModuleExtensionListener

import com.plugin.component.transform.ComponentTransform
import com.plugin.component.utils.JarUtil
import com.plugin.component.extension.PublicationManager
import com.plugin.component.extension.ComponentExtension

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

    @Override
    void apply(Project project) {
        boolean isRoot = project == project.rootProject
        if (isRoot) {
            handleRootProject(project)
        } else {
            handleProject(project)
        }
    }

    private void handleProject(Project project) {

        ProjectInfo projectInfo = PluginRuntime.sProjectInfoMap.get(project.name)

        //解析 component
        //比如 A 声明  component(':library') 且 A是可运行的
        //则A需要导入 library 中 sdk 和 impl 模块，其中 sdk 为模块内可见 implementation，impl 为纯 impl，其依赖的 sdk 需要模块哇可见 api
        project.dependencies.metaClass.component { String value ->
            return PublicationUtil.parseComponent(projectInfo, value)
        }

        //独立模块内 依赖sdk为api，由于该模块可能被依赖，所以sdk需要模块外暴露
        List<PublicationOption> publications = PluginRuntime.sPublicationManager.getPublicationByProject(project)
        project.dependencies {
            publications.each {
                api PublicationUtil.getPublication(it)
            }
        }

        //sdk 模块，则当前project需要依赖当前声明
        if (projectInfo.isSync()) {
            publications.each {
                PublicationUtil.addPublicationDependencies(project, it)
            }
        }

        project.afterEvaluate {

            ProjectUtil.addSdkSourceSets(project)
            List<PublicationOption> publicationList = PluginRuntime.sPublicationManager.getPublicationByProject(project)
            List<PublicationOption> publicationPublishList = new ArrayList<>()
            publicationList.each {
                if (it.version != null) {
                    publicationPublishList.add(it)
                }
            }

            if (publicationPublishList.size() > 0) {
                project.plugins.apply(Constants.PLUGIN_MAVEN_PUBLISH)
                def publishing = project.extensions.getByName(Constants.PUBLISHING)
                if (PluginRuntime.sModuleExtension.configure != null) {
                    publishing.repositories PluginRuntime.sModuleExtension.configure
                }

                publicationPublishList.each {
                    PublicationUtil.createPublishTask(project, it)
                }
            }
        }
    }

    private resetDir(Project project) {
        Logger.buildOutput("\n\n======> " + project.name + " <======\n")
        PluginRuntime.sSdkDir = new File(project.projectDir, Constants.SDK_DIR)
        PluginRuntime.sImplDir = new File(project.projectDir, Constants.IMPL_DIR)

        if (!PluginRuntime.sSdkDir.exists()) {
            PluginRuntime.sSdkDir.mkdirs()
            Logger.buildOutput("create File[" + PluginRuntime.sSdkDir.name + "]")
        }

        if (!PluginRuntime.sImplDir.exists()) {
            PluginRuntime.sImplDir.mkdirs()
            Logger.buildOutput("create File[" + PluginRuntime.sImplDir.name + "]")
        }

        ProjectUtil.getTasks(project).each {
            if (it == Constants.CLEAN) {
                if (!PluginRuntime.sSdkDir.deleteDir()) {
                    throw new RuntimeException("unable to delete dir " + PluginRuntime.sSdkDir.absolutePath)
                }
                if (!PluginRuntime.sImplDir.deleteDir()) {
                    throw new RuntimeException("unable to delete dir " + PluginRuntime.sImplDir.absolutePath)
                }
                PluginRuntime.sSdkDir.mkdirs()
                Logger.buildOutput("reset File[" + PluginRuntime.sSdkDir.name + "]")

                PluginRuntime.sImplDir.mkdirs()
                Logger.buildOutput("reset File[" + PluginRuntime.sImplDir.name + "]")
            }
        }
        project.repositories {
            flatDir {
                dirs PluginRuntime.sSdkDir.absolutePath
                Logger.buildOutput(project.name + "-flatDir Dir[" + PluginRuntime.sSdkDir.absolutePath + "]")

                dirs PluginRuntime.sImplDir.absolutePath
                Logger.buildOutput(project.name + "-flatDir Dir[" + PluginRuntime.sImplDir.absolutePath + "]")
            }
        }

    }


    private void handleRootProject(Project project) {

        resetDir(project)
        PluginRuntime.sPublicationManager = PublicationManager.getInstance()
        PluginRuntime.sPublicationManager.loadManifest(project)
        PluginRuntime.sModuleExtension = project.getExtensions().create(Constants.COMPONENT, ComponentExtension, new OnModuleExtensionListener() {

            @Override
            void onPublicationOptionAdded(Project childProject, PublicationOption publication) {
                if (publication.isSdk) {
                    PublicationUtil.initPublication(childProject, publication)
                    PluginRuntime.sSdkPublicationMap.put(childProject.name, publication)
                    PluginRuntime.sPublicationManager.addDependencyGraph(childProject.name, publication)
                }
            }

            @Override
            void onDebugOptionAdded(Project childProject, DebugOption aloneConfiguration) {
                PluginRuntime.sDebugMap.put(childProject.name, aloneConfiguration)
            }
        })

        DependenciesOption.metaClass.component { String value ->
            return Constants.COMPONENT_PRE + value
        }

        project.afterEvaluate {

            PluginRuntime.sAndroidJarPath = ProjectUtil.getAndroidJarPath(project, PluginRuntime.sModuleExtension.compileSdkVersion)
            Logger.buildOutput("sAndroidJarPath", PluginRuntime.sAndroidJarPath)

            project.allprojects.each {
                if (it == project) return
                Project childProject = it
                def moduleScript = new File(childProject.projectDir, Constants.COMPONENT_SCRIPT)
                if (moduleScript.exists()) {
                    PluginRuntime.sModuleExtension.currentChildProject = childProject
                    project.apply from: moduleScript
                }
            }

            List<String> topSort = PluginRuntime.sPublicationManager.dependencyGraph.topSort()
            Collections.reverse(topSort)
            topSort.each {
                PublicationOption publication = PluginRuntime.sPublicationManager.publicationDependencies.get(it)
                if (publication == null) {
                    return
                }

                Project childProject = project.findProject(publication.project)
                PublicationUtil.filterPublicationDependencies(publication)
                if (publication.version != null) {
                    JarUtil.handleMavenJar(childProject, publication)
                } else {
                    JarUtil.handleLocalJar(childProject, publication)
                }
                PluginRuntime.sPublicationManager.hitPublication(publication)
            }

            project.allprojects.each {
                if (it == project) return
                Project childProject = it

                Logger.buildOutput("\n\n======> " + childProject.name + " <======\n")

                ProjectInfo projectInfo = new ProjectInfo(childProject)
                if (projectInfo.isVailModulePluginTarget) {
                    childProject.repositories {
                        flatDir {
                            dirs PluginRuntime.sSdkDir.absolutePath
                            Logger.buildOutput(childProject.name + "-flatDir Dir[" + PluginRuntime.sSdkDir.absolutePath + "]")
                            dirs PluginRuntime.sImplDir.absolutePath
                            Logger.buildOutput(childProject.name + "-flatDir Dir[" + PluginRuntime.sImplDir.absolutePath + "]")
                        }
                    }
                    PluginRuntime.sProjectInfoMap.put(childProject.name, projectInfo)

                    Logger.buildOutput("compileModuleName", projectInfo.compileModuleName)
                    Logger.buildOutput("taskNames", projectInfo.taskNames)
                    Logger.buildOutput("moduleName", projectInfo.currentModuleName)
                    Logger.buildOutput("isSyncTask", projectInfo.isSync())
                    Logger.buildOutput("debugEnable", projectInfo.debugEnable)

                    childProject.plugins.whenObjectAdded {
                        if (it instanceof AppPlugin || it instanceof LibraryPlugin) {
                            childProject.pluginManager.apply(Constants.PLUGIN_COMPONENT)
                            Logger.buildOutput("project.apply plugin: com.android.component")
                        }
                    }

                    if (projectInfo.debugEnable) {
                        childProject.apply plugin: Constants.PLUGIN_APPLICATION
                        Logger.buildOutput("project.apply plugin: com.android.application")

                        if (!projectInfo.isMainModule()) {
                            childProject.android.sourceSets {
                                main {
                                    manifest.srcFile Constants.DEBUG_MANIFEST_PATH
                                    java.srcDirs = [Constants.JAVA_PATH, Constants.DEBUG_JAVA_PATH]
                                    res.srcDirs = [Constants.RES_PATH, Constants.DEBUG_RES_PATH]
                                    assets.srcDirs = [Constants.ASSETS_PATH, Constants.DEBUG_ASSETS_PATH]
                                    jniLibs.srcDirs = [Constants.JNILIBS_PATH, Constants.DEBUG_JNILIBS_PATH]
                                }
                            }
                        }
                        childProject.extensions.findByType(BaseExtension.class).registerTransform(new ComponentTransform())
                    } else {
                        childProject.apply plugin: Constants.PLUGIN_LIBRARY
                        Logger.buildOutput("project.apply plugin: com.android.library")
                    }
                }
            }
        }
    }
}
