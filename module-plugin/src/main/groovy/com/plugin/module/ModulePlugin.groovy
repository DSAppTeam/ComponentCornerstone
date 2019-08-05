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

        String compileModule = Constants.DEFAULT_APP_NAME

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
        boolean needAloneSourceSrt = false

        //根据配置添加各种组件依赖，并且自动化生成组件加载代码
        if (isRunAlone) {
            project.apply plugin: Constants.PLUGIN_APPLICATION
            Logger.buildOutput("project.apply plugin: com.android.application")

            //对于组件，则需要读取alone目录进行运行
            if (!module.equals(mainModuleName)) {
                needAloneSourceSrt = true;

                project.android.sourceSets {
                    main {
                        manifest.srcFile Constants.AFTER_MANIFEST_PATH
                        java.srcDirs = [Constants.JAVA_PATH, Constants.AFTER_JAVA_PATH]
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


//        if (!MisUtil.hasAndroidPlugin(project)) {
//            throw new GradleException("The android or android-library plugin must be applied to the project.")
//        }


        //解析 misPublication(xxxx) 中的字符串转化为 maven 格式
        project.dependencies.metaClass.misPublication { Object value ->
            String[] gav = MisUtil.filterGAV(value)
            return PublicationUtil.getPublication(gav[0], gav[1])
        }

        //获取当前project 可能依赖scope 使用的 misPublication(xxxx)，转化为 implementation
        List<Publication> publications = ModuleRuntime.publicationManager.getPublicationByProject(project)
        project.dependencies {
            publications.each {
                implementation PublicationUtil.getPublication(it.groupId, it.artifactId)
            }
        }

        //添加每一个 publication 的依赖
        if (project.gradle.startParameter.taskNames.isEmpty()) {
            publications.each {
                PublicationUtil.addPublicationDependencies(project, it)
            }
        }


        project.afterEvaluate {
            //调整sourceSet
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
        }
        project.gradle.getStartParameter().taskNames.each {
            if (it == 'clean') {
                if (!ModuleRuntime.misDir.deleteDir()) {
                    throw new RuntimeException("unable to delete dir " + ModuleRuntime.misDir.absolutePath)
                }
                ModuleRuntime.misDir.mkdirs()
            }
        }
        project.repositories {
            flatDir {
                dirs ModuleRuntime.misDir.absolutePath
            }
        }

        //读取 manifest
        ModuleRuntime.publicationManager = PublicationManager.getInstance()
        ModuleRuntime.publicationManager.loadManifest(project, ModuleRuntime.misDir)
        ModuleRuntime.sModuleExtension = project.getExtensions().create("module", ModuleExtension, new OnModuleExtensionListener() {

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
                }
            }
        }

        project.afterEvaluate {

            ModuleRuntime.androidJarPath = MisUtil.getAndroidJarPath(project, ModuleRuntime.sModuleExtension.compileSdkVersion)

            Dependencies.metaClass.misPublication { String value ->
                String[] gav = MisUtil.filterGAV(value)
                return Constants.MODULE_SDK_PRE + gav[0] + ':' + gav[1] + ':' + gav[2]
            }

            project.allprojects.each {
                if (it == project) return
                Project childProject = it
                def misScript = new File(childProject.projectDir, 'module.gradle')
                if (misScript.exists()) {
                    ModuleRuntime.sModuleExtension.currentChildProject = childProject
                    project.apply from: misScript
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
