package com.plugin.component

import com.android.build.gradle.AppExtension
import com.android.build.gradle.AppPlugin
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.LibraryPlugin
import com.android.build.gradle.TestedExtension
import com.plugin.component.extension.ComponentExtension
import com.plugin.component.extension.PublicationManager
import com.plugin.component.extension.module.PinInfo
import com.plugin.component.extension.module.ProjectInfo
import com.plugin.component.extension.option.debug.DebugDependenciesOption
import com.plugin.component.extension.option.pin.PinConfiguration
import com.plugin.component.extension.option.pin.PinOption
import com.plugin.component.extension.option.sdk.CompileOptions
import com.plugin.component.extension.option.debug.DebugConfiguration
import com.plugin.component.extension.option.sdk.PublicationDependenciesOption
import com.plugin.component.extension.option.sdk.PublicationOption
import com.plugin.component.extension.option.debug.DebugOption
import com.plugin.component.extension.option.sdk.SdkOption
import com.plugin.component.plugin.AbsPlugin
import com.plugin.component.transform.ComponentTransform
import com.plugin.component.transform.InjectCodeTransform
import com.plugin.component.transform.ScanCodeTransform
import com.plugin.component.utils.JarUtil
import com.plugin.component.utils.ProjectUtil
import com.plugin.component.utils.PublicationUtil
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.plugins.AppliedPlugin

class Runtimes {

    //sdk 发布信息
    private static Map<String, PublicationOption> sSdkPublicationMap = new HashMap<>()
    //impl 发布信息
    private static Map<String, PublicationOption> sImplPublicationMap = new HashMap<>()
    //模块信息
    private static Map<String, ProjectInfo> sProjectInfoMap = new HashMap<>()

    private static Map<String, PinConfiguration> sPinConfigurations = new HashMap<>()

    private static String sAndroidJarPath
    public static DebugOption sDebugOption
    public static SdkOption sSdkOption
    public static PinOption sPinOption
    public static ComponentExtension sExtension
    public static List<String> sAssembleModules = new ArrayList<>()
    public static File sSdkDir
    public static File sImplDir

    static initRuntimeConfigurationOnEvaluate(Project root) {
        sSdkDir = new File(root.projectDir, Constants.SDK_DIR)
        sImplDir = new File(root.projectDir, Constants.IMPL_DIR)
        Logger.buildOutput("sdk目录 File[" + sSdkDir.name + "]")
        Logger.buildOutput("impl目录 File[" + sSdkDir.name + "]")

        if (!sSdkDir.exists()) {
            sSdkDir.mkdirs()
            Logger.buildOutput("create File[" + sSdkDir.name + "]")
        }

        if (!sImplDir.exists()) {
            sImplDir.mkdirs()
            Logger.buildOutput("create File[" + sImplDir.name + "]")
        }

        ProjectUtil.getTasks(root).each {
            if (it == Constants.CLEAN) {
                if (!sSdkDir.deleteDir()) {
                    throw new RuntimeException("unable to delete dir " + sSdkDir.absolutePath)
                }
                if (!sImplDir.deleteDir()) {
                    throw new RuntimeException("unable to delete dir " + sImplDir.absolutePath)
                }
                sSdkDir.mkdirs()
                Logger.buildOutput("reset File[" + sSdkDir.name + "]")

                sImplDir.mkdirs()
                Logger.buildOutput("reset File[" + sImplDir.name + "]")
            }
        }
        root.repositories {
            flatDir {
                dirs sSdkDir.absolutePath
                Logger.buildOutput("flatDir Dir[" + sSdkDir.absolutePath + "]")

                dirs sImplDir.absolutePath
                Logger.buildOutput("flatDir Dir[" + sImplDir.absolutePath + "]")
            }
        }

        Logger.buildOutput("读取 sdk/impl manifest 配置文件...")
        PublicationManager.getInstance().loadManifest(root)

        Logger.buildOutput("读取 component.gradle 信息...")

        //todo sdk中依赖sdk，需要特别区分，预留后续逻辑
        PublicationDependenciesOption.metaClass.component { String value ->
            return Constants.COMPONENT_PRE + value
        }

        DebugDependenciesOption.metaClass.component { String value ->
            return Constants.DEBUG_COMPONENT_PRE + value
        }
    }

    static initRuntimeConfigurationAfterEvaluate(Project root, ComponentExtension componentExtension) {
        sExtension = componentExtension
        sDebugOption = sExtension.debugOption
        sSdkOption = sExtension.sdkOption
        sPinOption = sExtension.pinOption
        root.extensions.add("targetDebugName", sDebugOption.targetDebugName)
        sAndroidJarPath = ProjectUtil.getAndroidJarPath(root, componentExtension.sdkOption.compileSdkVersion)
        Logger.buildOutput("")
        Logger.buildOutput(" =====> component.gradle配置信息 <===== ")
        Logger.buildOutput("")
        Logger.buildOutput("    全局配置")
        Logger.buildOutput(componentExtension.toString())
        Logger.buildOutput("    SDK配置")
        Logger.buildOutput(componentExtension.sdkOption.toString())
        Logger.buildOutput("    PIN配置")
        Logger.buildOutput(componentExtension.pinOption.toString())
        Logger.buildOutput("    DEBUG配置")
        Logger.buildOutput(componentExtension.debugOption.toString())
        Logger.buildOutput(" =====> component.gradle配置信息 <===== ")
        Logger.buildOutput("")

        //初始化pins
        root.allprojects.each {
            for (PinConfiguration pinConfiguration : sPinOption.configurationList) {
                if (ProjectUtil.isProjectSame(it.name, pinConfiguration.name)) {
                    sPinConfigurations.put(it.name, pinConfiguration)
                    pinConfiguration.initMainPin(it)
                }
            }
        }

        Logger.buildOutput("")
        Logger.buildOutput("=====> 处理 sdk/impl jar <=====")
        List<String> topSort = PublicationManager.getInstance().dependencyGraph.topSort()
        Collections.reverse(topSort)
        topSort.each {
            PublicationOption publication = PublicationManager.getInstance().publicationDependencies.get(it)
            if (publication == null) {
                return
            }
            Project childProject = root.findProject(publication.project)
            PublicationUtil.filterPublicationDependencies(publication)
            if (publication.version != null) {
                JarUtil.handleMavenJar(childProject, publication)
            } else {
                JarUtil.handleLocalJar(childProject, publication)
            }
            PublicationManager.getInstance().hitPublication(publication)
        }
        Logger.buildOutput("=====> 处理 sdk/impl jar <=====")
        Logger.buildOutput("")

        //添加flat路径
        root.allprojects.each {
            if (it == root) return
            if (!shouldApplyComponentPlugin(it)) return
            Project childProject = it
            Logger.buildOutput("")
            Logger.buildOutput("=====> project[" + childProject.name + "]配置信息 <=====")
            childProject.repositories {
                flatDir {
                    dirs sSdkDir.absolutePath
                    Logger.buildOutput("add flatDir Dir[" + sSdkDir.absolutePath + "]")
                    dirs sImplDir.absolutePath
                    Logger.buildOutput("add flatDir Dir[" + sImplDir.absolutePath + "]")
                }
            }
        }

    }

    static hookAfterApplyingAndroidPlugin(Project root, AbsPlugin... plugins) {

        root.allprojects.each {
            if (it == root) return
            if (!shouldApplyComponentPlugin(it)) return

            Project childProject = it
            ProjectInfo projectInfo = new ProjectInfo(childProject)
            addProjectInfo(childProject.name, projectInfo)
            Logger.buildOutput("compileModuleName", projectInfo.compileModuleName)
            Logger.buildOutput("projectName", projectInfo.name)
            Logger.buildOutput("isDebugModule", projectInfo.isDebugModule())
            Logger.buildOutput("taskNames", projectInfo.taskNames)
            Logger.buildOutput("isSyncTask", projectInfo.isSync())
            Logger.buildOutput("isAssemble", projectInfo.isAssemble)
            Logger.buildOutput("isDebug", projectInfo.isDebug)
            Logger.buildOutput("=====> project[" + childProject.name + "]配置信息 <=====")
            Logger.buildOutput("")
//
//            childProject.plugins.all {
//                Class extensionClass
//                if (it instanceof AppPlugin) {
//                    extensionClass = AppExtension
//                } else if (it instanceof LibraryPlugin) {
//                    extensionClass = LibraryExtension
//                } else {
//                    return
//                }
//                childProject.extensions.configure(extensionClass, new Action<? extends TestedExtension>() {
//                    @Override
//                    void execute(TestedExtension testedExtension) {
//                        if (plugins != null && plugins.size() > 0) {
//                            for (AbsPlugin absPlugin : plugins) {
//                                absPlugin.evaluateAfterAndroidPlugin(childProject)
//                            }
//                        }
//                    }
//                })
//            }

            /**
             * 代替上述注解逻辑
             */
            childProject.plugins.whenObjectAdded {
                if (it instanceof AppPlugin || it instanceof LibraryPlugin) {
                    if (plugins != null && plugins.size() > 0) {
                        for (AbsPlugin absPlugin : plugins) {
                            absPlugin.evaluateAfterAndroidPlugin(childProject)
                        }
                    }
                    if (it instanceof AppPlugin) {
                        if (projectInfo.isDebugModule() || projectInfo.isCompileModuleAndAssemble()) {
                            Logger.buildOutput("plugin is AppPlugin")
                            Logger.buildOutput("registerTransform", "ScanCodeTransform")
                            Logger.buildOutput("registerTransform", "InjectCodeTransform")
                            childProject.extensions.findByType(BaseExtension.class).registerTransform(new ComponentTransform(childProject))
//                                childProject.extensions.findByType(BaseExtension.class).registerTransform(new InjectCodeTransform(childProject))
                        }
                    }
                    childProject.dependencies {
                        Logger.buildOutput("add dependency: " + Constants.CORE_DEPENDENCY)
                        implementation Constants.CORE_DEPENDENCY
                    }
                    if (plugins != null && plugins.size() > 0) {
                        for (AbsPlugin absPlugin : plugins) {
                            absPlugin.afterEvaluateAfterAndroidPlugin(childProject)
                        }
                    }
                }
            }
        }
    }

    /**
     * 测试 AppPlugin/LibraryPlugin 时许
     * t(evaluate) < t(configure) < t(whenObjectAdded) < t(withPlugin) < t(afterEvaluate)
     * t越大约慢
     * @param project
     */
    static void timeCallerTest(Project project) {
        project.plugins.all {
            Class extensionClass
            if (it instanceof AppPlugin) {
                extensionClass = AppExtension
            } else if (it instanceof LibraryPlugin) {
                extensionClass = LibraryExtension
            } else {
                return
            }
            /**
             * 在 evaluate 之后 afterEvaluate 之前调用 configure 最早
             */
            project.extensions.configure(extensionClass, new Action<? extends TestedExtension>() {
                @Override
                void execute(TestedExtension testedExtension) {
                    Logger.buildOutput(project.name + "==>" + "test-configure")
                }
            })
        }

        /**
         * 在 afterEvaluate 回调之前调用，在 configure 之后
         *
         */
        project.plugins.whenObjectAdded {
            if (it instanceof AppPlugin || it in LibraryPlugin) {
                Logger.buildOutput(project.name + "==>" + "test-whenObjectAdded")
            }
        }

        /**
         * 在 evaluate 之后 afterEvaluate 之前调用，比 configure 慢，最慢
         */
        project.getPluginManager().withPlugin("com.android.library", new Action<AppliedPlugin>() {
            @Override
            void execute(AppliedPlugin appliedPlugin) {
                Logger.buildOutput(project.name + "==>" + "test-withPlugin")
            }
        })
        project.getPluginManager().withPlugin("com.android.application", new Action<AppliedPlugin>() {
            @Override
            void execute(AppliedPlugin appliedPlugin) {
                Logger.buildOutput(project.name + "==>" + "test-withPlugin")
            }
        })
    }

    static void injectComponentPlugin(Project root) {
        root.allprojects.each {
            if (it == root) return
            if (!shouldApplyComponentPlugin(it)) return
            it.pluginManager.apply(Constants.PLUGIN_COMPONENT)
            Logger.buildOutput(it.name + "apply ==>" + "com.android.component")
//            timeCallerTest(it)
        }
    }


    static Map<String, PinConfiguration> getPinConfigurations() {
        return sPinConfigurations
    }

    static boolean hasPinModule() {
        return !sPinConfigurations.isEmpty()
    }

    static PinConfiguration getPinConfiguration(String name) {
        return sPinConfigurations.get(name)
    }

    static CompileOptions getCompileOption() {
        return sSdkOption.compileOption
    }

    static getAndroidJarPath() {
        return sAndroidJarPath
    }

    static boolean shouldApplyComponentPlugin(Project project) {
        return sExtension.shouldApplyComponentPlugin(project)
    }

    static String getDebugModuleName() {
        return sDebugOption.targetModuleName
    }

    static String getDebugTargetName() {
        return sDebugOption.targetDebugName
    }

    static List<DebugConfiguration> getDebugConfigurations() {
        return sDebugOption.configurationList
    }

    static void addImplPublication(String projectName, PublicationOption publicationOption) {
        sImplPublicationMap.put(projectName, publicationOption)
    }

    static PublicationOption getImplPublication(String projectName) {
        return sImplPublicationMap.get(projectName)
    }

    static void addSdkPublication(String projectName, PublicationOption publicationOption) {
        sSdkPublicationMap.put(projectName, publicationOption)
    }

    static PublicationOption getSdkPublication(String projectName) {
        return sSdkPublicationMap.get(projectName)
    }

    static Map<String, PublicationOption> getSdkPublicationMap() {
        return sSdkPublicationMap
    }

    static void addProjectInfo(String projectName, ProjectInfo projectInfo) {
        sProjectInfoMap.put(projectName, projectInfo)
    }

    static ProjectInfo getProjectInfo(String projectName) {
        return sProjectInfoMap.get(projectName)
    }

    static ProjectInfo getCompileProjectWhenAssemble() {
        Set<String> keys = sProjectInfoMap.keySet()
        for (String key : keys) {
            ProjectInfo projectInfo = sProjectInfoMap.get(key)
            if (projectInfo.isCompileModuleAndAssemble()) {
                return projectInfo
            }
        }
        return null
    }
}
