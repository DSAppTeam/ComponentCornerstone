package com.plugin.module

import com.android.build.gradle.BaseExtension
import com.plugin.module.extension.ModuleRuntime
import com.plugin.module.extension.module.AloneConfiguration
import com.plugin.module.extension.module.AssembleTask
import com.plugin.module.extension.module.Dependencies
import com.plugin.module.extension.ModuleExtension
import com.plugin.module.listener.OnModuleExtensionListener
import com.plugin.module.extension.publication.Publication
import com.plugin.module.extension.publication.PublicationManager
import com.plugin.module.transform.CodeTransform
import com.plugin.module.utils.JarUtil
import com.plugin.module.utils.MisUtil
import com.plugin.module.utils.ProjectUtil
import com.plugin.module.utils.PublicationUtil
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 *   ./gradlew --no-daemon ModulePlugin  -Dorg.gradle.debug=true
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

        String compileModule = Constants.DEFAULT_MAIN_MODULE_NAME

        String taskNames = ProjectUtil.getTaskName(project)
        String module = ProjectUtil.getModuleName(project)


        AssembleTask assembleTask = ProjectUtil.parseTaskInfo(ProjectUtil.getTasks(project))

        if (assembleTask.isAssemble) {
            compileModule = ProjectUtil.parseMainModuleName(project, assembleTask)
            Logger.buildOutput("compileModule", compileModule)
        }

        Logger.buildOutput("taskNames", taskNames)
        Logger.buildOutput("module", module)
        Logger.buildOutput("isAssemble", assembleTask.isAssemble)


        boolean isRunAlone = ProjectUtil.isRunalone(project)
        Logger.buildOutput("runalone", isRunAlone)

        if (isRunAlone && assembleTask.isAssemble) {
            if (module.equals(compileModule)) {
                isRunAlone = true
            } else {
                isRunAlone = false
            }
        }

        //根据配置添加各种组件依赖，并且自动化生成组件加载代码
        if (isRunAlone) {
            project.apply plugin: Constants.PLUGIN_APPLICATION
            Logger.buildOutput("project.apply plugin: com.android.application")

            //对于组件，则需要读取alone目录进行运行
            if (!module.equals(ProjectUtil.mainModuleName)) {
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
//                com.plugin.module.utils.Utils.compileComponents(project, assembleTask)
                    //参考https://github.com/luojilab/DDComponentForAndroid/issues/122
                    //                project.android.registerTransform(new CodeTransform(project))
                    project.extensions.findByType(BaseExtension.class).registerTransform(new CodeTransform(project))
                }
            }

        } else {
            project.apply plugin: Constants.PLUGIN_LIBRARY
            Logger.buildOutput("project.apply plugin: com.android.library")
        }


        project.dependencies.metaClass.misPublication { Object value ->
            String[] gav = MisUtil.filterGAV(value)
            return PublicationUtil.getPublication(gav[0], gav[1])
        }

        List<Publication> publications = ModuleRuntime.publicationManager.getPublicationByProject(project)
        project.dependencies {
            publications.each {
                implementation PublicationUtil.getPublication(it.groupId, it.artifactId)
            }
        }
        if (project.gradle.startParameter.taskNames.isEmpty()) {
            publications.each {
                PublicationUtil.addPublicationDependencies(project, it)
            }
        }


        project.afterEvaluate {
            MisUtil.addMisSourceSets(project)
            List<Publication> publicationList = ModuleRuntime.publicationManager.getPublicationByProject(project)
            List<Publication> publicationPublishList = new ArrayList<>()
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
            void onPublicationAdded(Project childProject, Publication publication) {
                PublicationUtil.initPublication(childProject, publication)
                ModuleRuntime.publicationManager.addDependencyGraph(publication)
                ModuleRuntime.publicationMap.put(childProject.name, publication)
            }

            @Override
            void onAloneConfigAdded(Project childProject, AloneConfiguration aloneConfiguration) {
                ModuleRuntime.aloneRunMap.put(childProject.name, aloneConfiguration)
            }
        })

        project.allprojects.each {
            if (it == project) return
            Project childProject = it
            childProject.repositories {
                flatDir {
                    dirs ModuleRuntime.misDir.absolutePath
                    Logger.buildOutput(childProject.name + "-flatDir Dir[" + ModuleRuntime.misDir.absolutePath + "]")
                }
            }
        }

        project.afterEvaluate {

            ModuleRuntime.androidJarPath = MisUtil.getAndroidJarPath(project, ModuleRuntime.sModuleExtension.compileSdkVersion)
            Logger.buildOutput("androidJarPath", ModuleRuntime.androidJarPath)

            Dependencies.metaClass.misPublication { String value ->
                String[] gav = MisUtil.filterGAV(value)
                return Constants.MODULE_SDK_PRE + gav[0] + ':' + gav[1] + ':' + gav[2]
            }

            project.allprojects.each {
                if (it == project) return
                Project childProject = it
                def moduleScript = new File(childProject.projectDir, 'module.gradle')
                if (moduleScript.exists()) {
                    ModuleRuntime.sModuleExtension.currentChildProject = childProject
                    Logger.buildOutput("apply childProject(" + childProject.name + ")'s module.gradle")
                    project.apply from: moduleScript
                }
            }

            List<String> topSort = ModuleRuntime.publicationManager.dependencyGraph.topSort()
            Collections.reverse(topSort)
            topSort.each {
                Publication publication = ModuleRuntime.publicationManager.publicationDependencies.get(it)
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
        }
    }
}
