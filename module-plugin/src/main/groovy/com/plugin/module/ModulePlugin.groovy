package com.plugin.module

import com.android.build.gradle.AppPlugin
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.LibraryPlugin
import com.plugin.module.extension.option.RunAloneOption
import com.plugin.module.extension.option.DependenciesOption
import com.plugin.module.extension.ModuleExtension
import com.plugin.module.extension.module.ProjectInfo
import com.plugin.module.listener.OnModuleExtensionListener
import com.plugin.module.extension.option.PublicationOption
import com.plugin.module.extension.PublicationManager
import com.plugin.module.transform.CodeTransform
import com.plugin.module.utils.JarUtil
import com.plugin.module.utils.MisUtil
import com.plugin.module.utils.ProjectUtil
import com.plugin.module.utils.PublicationUtil
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 *   ./gradlew --no-daemon ModulePlugin  -Dorg.gradle.debug=true
 *   ./gradlew --no-daemon [clean, :app:generateDebugSources, :library:generateDebugSources, :module_lib:generateDebugSources]  -Dorg.gradle.debug=true
 */
class ModulePlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        boolean isRoot = project == project.rootProject
        if (isRoot) {
            handleRootProject(project)
        } else {
            handleProject(project)
        }
    }

    /**
     * 处理非rootproject
     * @param project
     */
    private void handleProject(Project project) {

        ProjectInfo projectInfo = ModuleRuntime.projectInfos.get(project.name)

        project.dependencies.metaClass.misPublication { Object value ->
            String[] gav = MisUtil.filterGAV(value)
            if (projectInfo.isRunAlone && projectInfo.isAssembleTask()) {
                project.dependencies {
                    implementation PublicationUtil.getPublication(gav[0], gav[1])
                }
                return project.project(':library')
            }
            return PublicationUtil.getPublication(gav[0], gav[1])
        }

        List<PublicationOption> publications = ModuleRuntime.publicationManager.getPublicationByProject(project)

        project.dependencies {
            publications.each {
                api PublicationUtil.getPublication(it.groupId, it.artifactId)
            }
        }

        if (project.gradle.startParameter.taskNames.isEmpty()) {
            publications.each {
                PublicationUtil.addPublicationDependencies(project, it)
            }
        }

        project.afterEvaluate {

            MisUtil.addMisSourceSets(project)
            List<PublicationOption> publicationList = ModuleRuntime.publicationManager.getPublicationByProject(project)
            List<PublicationOption> publicationPublishList = new ArrayList<>()
            publicationList.each {
                if (it.version != null) {
                    publicationPublishList.add(it)
                }
            }

            if (publicationPublishList.size() > 0) {
                project.plugins.apply('maven-publish')
                def publishing = project.extensions.getByName('publishing')
                if (ModuleRuntime.sModuleExtension.configure != null) {
                    publishing.repositories ModuleRuntime.sModuleExtension.configure
                }

                publicationPublishList.each {
                    PublicationUtil.createPublishTask(project, it)
                }
            }
        }
    }

    /**
     * 1. 初始化 mis-sdk/manifest 文件
     * 2. 读取 module.gradle 配置完成子project信息 module-plugin 所需要信息手机
     * @param project
     */
    private void handleRootProject(Project project) {

        ModuleRuntime.misDir = new File(project.projectDir, '.gradle/mis')
        if (!ModuleRuntime.misDir.exists()) {
            ModuleRuntime.misDir.mkdirs()
            Logger.buildOutput("create File[" + ModuleRuntime.misDir.name + "]")
        }

        ProjectUtil.getTasks(project).each {
            if (it == 'clean') {
                if (!ModuleRuntime.misDir.deleteDir()) {
                    throw new RuntimeException("unable to delete dir " + ModuleRuntime.misDir.absolutePath)
                }
                ModuleRuntime.misDir.mkdirs()
                Logger.buildOutput("reset File[" + ModuleRuntime.misDir.name + "]")
            }
        }

        project.repositories {
            flatDir {
                dirs ModuleRuntime.misDir.absolutePath
                Logger.buildOutput(project.name + "-flatDir Dir[" + ModuleRuntime.misDir.absolutePath + "]")
            }
        }

        ModuleRuntime.publicationManager = PublicationManager.getInstance()
        ModuleRuntime.publicationManager.loadManifest(project, ModuleRuntime.misDir)
        ModuleRuntime.sModuleExtension = project.getExtensions().create(Constants.EXTENSION_NAME, ModuleExtension, new OnModuleExtensionListener() {

            @Override
            void onPublicationAdded(Project childProject, PublicationOption publication) {
                PublicationUtil.initPublication(childProject, publication)
                ModuleRuntime.publicationManager.addDependencyGraph(publication)
                ModuleRuntime.publicationMap.put(childProject.name, publication)
            }

            @Override
            void onAloneConfigAdded(Project childProject, RunAloneOption aloneConfiguration) {
                ModuleRuntime.aloneRunMap.put(childProject.name, aloneConfiguration)
            }
        })

        project.afterEvaluate {

            ModuleRuntime.androidJarPath = MisUtil.getAndroidJarPath(project, ModuleRuntime.sModuleExtension.compileSdkVersion)
            Logger.buildOutput("androidJarPath", ModuleRuntime.androidJarPath)

            DependenciesOption.metaClass.misPublication { String value ->
                String[] gav = MisUtil.filterGAV(value)
                return Constants.MODULE_SDK_PRE + gav[0] + ':' + gav[1] + ':' + gav[2]
            }

            project.allprojects.each {
                if (it == project) return
                Project childProject = it
                def moduleScript = new File(childProject.projectDir, 'module.gradle')
                if (moduleScript.exists()) {
                    ModuleRuntime.sModuleExtension.currentChildProject = childProject
                    project.apply from: moduleScript
                }
            }

            List<String> topSort = ModuleRuntime.publicationManager.dependencyGraph.topSort()
            Collections.reverse(topSort)
            topSort.each {
                PublicationOption publication = ModuleRuntime.publicationManager.publicationDependencies.get(it)
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
                ModuleRuntime.publicationManager.hitPublication(publication)
            }

            project.allprojects.each {
                if (it == project) return
                Project childProject = it

                ProjectInfo projectInfo = new ProjectInfo(childProject)
                if (projectInfo.isVailModulePluginTarget) {
                    childProject.repositories {
                        flatDir {
                            dirs ModuleRuntime.misDir.absolutePath
                            Logger.buildOutput(childProject.name + "-flatDir Dir[" + ModuleRuntime.misDir.absolutePath + "]")
                        }
                    }
                    ModuleRuntime.projectInfos.put(childProject.name, projectInfo)

                    Logger.buildOutput("compileModuleName", projectInfo.compileModuleName)
                    Logger.buildOutput("taskNames", projectInfo.taskNames)
                    Logger.buildOutput("moduleName", projectInfo.currentModuleName)
                    Logger.buildOutput("isAssemble", projectInfo.isAssembleTask())
                    Logger.buildOutput("isRunAlone", projectInfo.isRunAlone)

                    childProject.plugins.whenObjectAdded {
                        if (it instanceof AppPlugin || it instanceof LibraryPlugin) {
                            childProject.pluginManager.apply('com.android.module')
                            Logger.buildOutput("project.apply plugin: com.android.module")
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
