package com.plugin.component

import com.android.build.gradle.AppPlugin
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.LibraryPlugin
import com.plugin.component.extension.option.DependenciesOption
import com.plugin.component.extension.module.ProjectInfo
import com.plugin.component.extension.option.PublicationOption
import com.plugin.component.extension.option.RunAloneOption
import com.plugin.component.listener.OnModuleExtensionListener
import com.plugin.component.transform.CodeTransform
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
 *  组件插件入口
 *  created by yummylau 2019/08/09
 */
class ComponentPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        boolean isRoot = project == project.rootProject
        Logger.buildOutput("\n\n======> " + project.name + " <======\n")
        if (isRoot) {
            handleRootProject(project)
        } else {
            handleProject(project)
        }
    }

    private void handleProject(Project project) {

        ProjectInfo projectInfo = PluginRuntime.sProjectInfoMap.get(project.name)

        project.dependencies.metaClass.component { Object value ->
            PublicationUtil.parseComponent(projectInfo, value)
        }

        //实现模块导入 sdk
        List<PublicationOption> publications = PluginRuntime.sPublicationManager.getPublicationByProject(project)
        project.dependencies {
            publications.each {
                api PublicationUtil.getPublication(it.groupId, it.artifactId)
            }
        }

        //实现模块导入 sdk依赖
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


    private void handleRootProject(Project project) {
        PluginRuntime.sSdkDir = new File(project.projectDir, Constants.SDK_DIR)
        if (!PluginRuntime.sSdkDir.exists()) {
            PluginRuntime.sSdkDir.mkdirs()
            Logger.buildOutput("create File[" + PluginRuntime.sSdkDir.name + "]")
        }

        ProjectUtil.getTasks(project).each {
            if (it == Constants.CLEAN) {
                if (!PluginRuntime.sSdkDir.deleteDir()) {
                    throw new RuntimeException("unable to delete dir " + PluginRuntime.sSdkDir.absolutePath)
                }
                PluginRuntime.sSdkDir.mkdirs()
                Logger.buildOutput("reset File[" + PluginRuntime.sSdkDir.name + "]")
            }
        }

        project.repositories {
            flatDir {
                dirs PluginRuntime.sSdkDir.absolutePath
                Logger.buildOutput(project.name + "-flatDir Dir[" + PluginRuntime.sSdkDir.absolutePath + "]")
            }
        }
        PluginRuntime.sPublicationManager = PublicationManager.getInstance()
        PluginRuntime.sPublicationManager.loadManifest(project, PluginRuntime.sSdkDir)
        PluginRuntime.sModuleExtension = project.getExtensions().create(Constants.COMPONENT, ComponentExtension, new OnModuleExtensionListener() {

            @Override
            void onPublicationOptionAdded(Project childProject, PublicationOption publication) {
                PublicationUtil.initPublication(childProject, publication)
                PluginRuntime.sPublicationManager.addDependencyGraph(publication)
                PluginRuntime.sPublicationMap.put(childProject.name, publication)
            }

            @Override
            void onRunAloneOptionAdded(Project childProject, RunAloneOption aloneConfiguration) {
                PluginRuntime.sRunAloneMap.put(childProject.name, aloneConfiguration)
            }
        })

        project.afterEvaluate {

            PluginRuntime.sAndroidJarPath = ProjectUtil.getAndroidJarPath(project, PluginRuntime.sModuleExtension.compileSdkVersion)
            Logger.buildOutput("sAndroidJarPath", PluginRuntime.sAndroidJarPath)

            DependenciesOption.metaClass.component { String value ->
                String[] gav = PublicationUtil.filterGAV(value)
                return Constants.SDK_PRE + gav[0] + ':' + gav[1] + ':' + gav[2]
            }

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

                ProjectInfo projectInfo = new ProjectInfo(childProject)
                if (projectInfo.isVailModulePluginTarget) {
                    childProject.repositories {
                        flatDir {
                            dirs PluginRuntime.sSdkDir.absolutePath
                            Logger.buildOutput(childProject.name + "-flatDir Dir[" + PluginRuntime.sSdkDir.absolutePath + "]")
                        }
                    }
                    PluginRuntime.sProjectInfoMap.put(childProject.name, projectInfo)

                    Logger.buildOutput("compileModuleName", projectInfo.compileModuleName)
                    Logger.buildOutput("taskNames", projectInfo.taskNames)
                    Logger.buildOutput("moduleName", projectInfo.currentModuleName)
                    Logger.buildOutput("isAssemble", projectInfo.isAssembleTask())
                    Logger.buildOutput("isRunAlone", projectInfo.isRunAlone)

                    childProject.plugins.whenObjectAdded {
                        if (it instanceof AppPlugin || it instanceof LibraryPlugin) {
                            childProject.pluginManager.apply(Constants.PLUGIN_COMPONENT)
                            Logger.buildOutput("project.apply plugin: com.android.component")
                        }
                    }

                    if (projectInfo.isRunAlone) {
                        childProject.apply plugin: Constants.PLUGIN_APPLICATION
                        Logger.buildOutput("project.apply plugin: com.android.application")

                        if (!projectInfo.isMainModule()) {
                            childProject.android.sourceSets {
                                main {
                                    manifest.srcFile Constants.AFTER_MANIFEST_PATH
                                    java.srcDirs = [Constants.JAVA_PATH, Constants.AFTER_JAVA_PATH]
                                    res.srcDirs = [Constants.RES_PATH, Constants.AFTER_RES_PATH]
                                    assets.srcDirs = [Constants.ASSETS_PATH, Constants.AFTER_ASSETS_PATH]
                                    jniLibs.srcDirs = [Constants.JNILIBS_PATH, Constants.AFTER_JNILIBS_PATH]
                                }
                            }
                            if (projectInfo.isCompileModuleAndAssemble()) {
                                childProject.extensions.findByType(BaseExtension.class).registerTransform(new CodeTransform(childProject))
                            }
                        }
                    } else {
                        childProject.apply plugin: Constants.PLUGIN_LIBRARY
                        Logger.buildOutput("project.apply plugin: com.android.library")
                    }
                }
            }
        }
    }
}
