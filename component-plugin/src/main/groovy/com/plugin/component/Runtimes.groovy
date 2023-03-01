package com.plugin.component

import com.android.build.gradle.AppExtension
import com.android.build.gradle.AppPlugin
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.LibraryPlugin
import com.android.build.gradle.TestedExtension
import com.plugin.component.extension.ComponentExtension
import com.plugin.component.extension.PublicationManager
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
import com.plugin.component.log.Logger
import com.plugin.component.log.MutLineLog
import com.plugin.component.plugin.AbsPlugin
import com.plugin.component.task.ComponentPublishTask
import com.plugin.component.transform.ComponentTransform
import com.plugin.component.utils.FileUtil
import com.plugin.component.utils.GitUtil
import com.plugin.component.utils.JarUtil
import com.plugin.component.utils.ProjectUtil
import com.plugin.component.utils.PublicationUtil
import org.gradle.BuildListener
import org.gradle.api.Action
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.AppliedPlugin
import org.gradle.api.tasks.TaskExecutionException
import org.gradle.api.tasks.TaskProvider
import org.gradle.internal.event.BroadcastDispatch
import org.gradle.internal.event.ListenerBroadcast
import org.gradle.invocation.DefaultGradle
import org.gradle.listener.ClosureBackedMethodInvocationDispatch

import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.util.concurrent.ConcurrentHashMap

class Runtimes {

    //sdk 发布信息
    private static Map<String, PublicationOption> sSdkPublicationMap = new HashMap<>()
    //impl 发布信息
    private static Map<String, PublicationOption> sImplPublicationMap = new HashMap<>()
    //模块信息
    private static Map<String, ProjectInfo> sProjectInfoMap = new ConcurrentHashMap<>()

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
        //sdk目录及文件读取流程
        sSdkDir = new File(root.projectDir, Constants.SDK_DIR)
        sImplDir = new File(root.projectDir, Constants.IMPL_DIR)
        if (!sSdkDir.exists()) {
            sSdkDir.mkdirs()
        }
        if (!sImplDir.exists()) {
            sImplDir.mkdirs()
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
                sImplDir.mkdirs()
            }
        }

        root.repositories {
            flatDir {
                dirs sSdkDir.absolutePath
                dirs sImplDir.absolutePath
            }
        }
        PublicationManager.getInstance().loadManifest(root)
        Logger.buildOutput("load Manifest")
        PublicationDependenciesOption.metaClass.component { String value ->
            return Constants.COMPONENT_PRE + value
        }
        DebugDependenciesOption.metaClass.component { String value ->
            return Constants.DEBUG_COMPONENT_PRE + value
        }
        Logger.buildBlockLog(
                "初始化辅助目录",
                new MutLineLog()
                        .build4("* 创建SDK目录 = " + sSdkDir.absolutePath)
                        .build4("* 根项目添加 flatDir = " + sSdkDir.absolutePath)
                        .build4("* 读取SDK目录下 publicationManifest.xml")
        )
    }

    private static void initGitHooks(Project root) {
        if (GitUtil.gitExist && sSdkOption.needGitHook) {
            File hooksDir = new File(root.projectDir, Constants.HOOKS_DIR)
            if (!hooksDir.exists()) {
                hooksDir.mkdirs()
            }
            File prePushHook = new File(hooksDir, "pre-push")
            if (!prePushHook.exists()) {
                prePushHook.createNewFile()
                FileOutputStream fos = new FileOutputStream(prePushHook)
                OutputStreamWriter osw = new OutputStreamWriter(fos, "UTF-8")
                osw.write(Constants.HOOKS_CONTENT)
                osw.flush()
                osw.close()
            }
            String hooksDirPath = hooksDir.absolutePath
            String cmd = "git config core.hooksPath ${hooksDirPath} && chmod 700 ${hooksDirPath}${File.separator}*"
            Logger.buildOutput("hooks Path:${hooksDirPath}")
            Logger.buildOutput("hooks cmd:${cmd}")
            FileUtil.shell(cmd)
        }
    }

    static initRuntimeConfigurationAfterEvaluate(Project root, ComponentExtension componentExtension) {
        sExtension = componentExtension
        sDebugOption = sExtension.debugOption
        sSdkOption = sExtension.sdkOption
        sPinOption = sExtension.pinOption
        root.extensions.add("targetDebugName", sDebugOption.targetDebugName)
        sAndroidJarPath = ProjectUtil.getAndroidJarPath(root, componentExtension.sdkOption.compileSdkVersion)
        initGitHooks(root)
        Logger.buildBlockLog(
                "component.gradle 脚本信息",
                new MutLineLog()
                        .build4("* 添加ext(targetDebugName) = " + sDebugOption.targetDebugName)
                        .build4("* 全局配置")
                        .build4(componentExtension.toString())
                        .build4("* SDK配置")
                        .build4(componentExtension.sdkOption.toString())
                        .build4("* PIN配置")
                        .build4(componentExtension.pinOption.toString())
                        .build4("* DEBUG配置")
                        .build4(componentExtension.debugOption.toString())
        )

        MutLineLog mutLineLog = new MutLineLog()
        //初始化pins
        root.allprojects.each {
            for (PinConfiguration pinConfiguration : sPinOption.configurationList) {
                if (ProjectUtil.isProjectSame(it.name, pinConfiguration.name)) {
                    sPinConfigurations.put(it.name, pinConfiguration)
                    pinConfiguration.initMainPin(it)
                }
            }
        }

        //添加flat路径
        root.allprojects.each {
            if (it == root) return
            if (!shouldApplyComponentPlugin(it)) return
            Project childProject = it
            ProjectInfo projectInfo = new ProjectInfo(childProject)
            addProjectInfo(childProject.name, projectInfo)
            childProject.repositories {
                flatDir {
                    dirs sSdkDir.absolutePath
                    dirs sImplDir.absolutePath
                    mutLineLog.build4("* " + childProject.name + " add flatDir Dir[" + sSdkDir.absolutePath + "]")
                    mutLineLog.build4("* " + childProject.name + " add flatDir Dir[" + sImplDir.absolutePath + "]")
                }
            }
        }

        mutLineLog.build4("* 初始化 pins main 目录")
        List<String> topSort = PublicationManager.getInstance().dependencyGraph.topSort()
        Collections.reverse(topSort)
        topSort.each {
            PublicationOption publication = PublicationManager.getInstance().publicationDependencies.get(it)
            if (publication == null) {
                return
            }
            Project childProject = root.findProject(publication.project)
            ProjectInfo projectInfo = getProjectInfo(childProject.name)
            if (projectInfo != null && projectInfo.isPublish || sSdkOption.isPublishMode) {
                publication.sdkNeedPublish = true
                publication.impNeedPublish = true
                publication.forceUseLocal(true)
                //发布时删除本地文件，确保不会使用本地文件
                File targetSdk = new File(sSdkDir, PublicationUtil.getJarName(publication))
                if (targetSdk.exists()) {
                    targetSdk.delete()
                }
                File targetImpl = new File(sImplDir, PublicationUtil.getAarName(publication))
                if (targetImpl.exists()) {
                    targetImpl.delete()
                }
            }
            PublicationUtil.filterPublicationDependencies(publication)
            if (/*!publication.forceLocal && */publication.sdkVersion != null) {
                mutLineLog.build4("* " + JarUtil.handleMavenJar(childProject, publication))
            } else {
                mutLineLog.build4("* " + JarUtil.handleLocalJar(childProject, publication))
            }
            PublicationManager.getInstance().hitPublication(publication)
        }

        Logger.buildBlockLog("预处理插件", mutLineLog)
    }

    static hookAfterApplyingAndroidPlugin(Project root, Project androidProject, AbsPlugin... plugins) {

        MutLineLog mutLineLog = new MutLineLog()
        root.allprojects.each {
            if (it == root) return
            if (!shouldApplyComponentPlugin(it)) return

            Project childProject = it
            ProjectInfo projectInfo = getProjectInfo(it.name)


            mutLineLog.build4("* " + childProject.name)
            mutLineLog.build4("     compileModuleName = " + projectInfo.compileModuleName)
            mutLineLog.build4("     projectName = " + projectInfo.name)
            mutLineLog.build4("     isDebugModule = " + projectInfo.isDebugModule())
            mutLineLog.build4("     taskNames = " + projectInfo.taskNames)
            mutLineLog.build4("     isSyncTask = " + projectInfo.isSync())
            mutLineLog.build4("     isAssemble = " + projectInfo.isAssemble)
            mutLineLog.build4("     isPublish = " + projectInfo.isPublish)
            mutLineLog.build4("     isDebug = " + projectInfo.isDebug)
            mutLineLog.build4("     modules = " + projectInfo.modules)

            childProject.plugins.whenObjectAdded {
                if (it instanceof AppPlugin || it instanceof LibraryPlugin) {
                    if (plugins != null && plugins.size() > 0) {
                        for (AbsPlugin absPlugin : plugins) {
                            absPlugin.evaluateAfterAndroidPlugin(childProject)
                        }
                    }
                    if (it instanceof AppPlugin) {
                        if (projectInfo.isDebugModule() || projectInfo.isCompileModuleAndAssemble()) {
                            childProject.extensions.findByType(BaseExtension.class).registerTransform(new ComponentTransform(childProject))
                        }
                    }
                    childProject.dependencies {
                        implementation Constants.CORE_DEPENDENCY
                    }
                    if (plugins != null && plugins.size() > 0) {
                        for (AbsPlugin absPlugin : plugins) {
                            absPlugin.afterEvaluateAfterAndroidPlugin(childProject, androidProject)
                        }
                    }
                }
            }
        }
        Logger.buildBlockLog("子 PROJECT 信息", mutLineLog)
    }

    //AGP4.1.0后调整了插件入口，废弃。
    //把所有 监听了 projectsEvaluated 的匿名内部类移除
    static List<BroadcastDispatch<BuildListener>> hookProjectsEvaluatedAction(Project project) {
        def removeDispatch = new ArrayList<BroadcastDispatch<BuildListener>>()
        try {
            ListenerBroadcast<BuildListener> buildListenerBroadcast
            def fBuildListenerBroadcast = DefaultGradle.class.getDeclaredField("buildListenerBroadcast")
            Field modifiersField = Field.class.getDeclaredField("modifiers");
            modifiersField.setAccessible(true)
            modifiersField.setInt(fBuildListenerBroadcast, fBuildListenerBroadcast.getModifiers() & ~Modifier.FINAL)
            fBuildListenerBroadcast.setAccessible(true)
            buildListenerBroadcast = (ListenerBroadcast<BuildListener>) fBuildListenerBroadcast.get(project.gradle)

            Field fBroadcast = ListenerBroadcast.class.getDeclaredField("broadcast")
            fBroadcast.setAccessible(true)
            BroadcastDispatch<BuildListener> broadcast = (BroadcastDispatch<BuildListener>) fBroadcast.get(buildListenerBroadcast)
            def fDispatchers = broadcast.getClass().getDeclaredField("dispatchers")
            fDispatchers.setAccessible(true)
            ArrayList<BroadcastDispatch<BuildListener>> dispatchers = (ArrayList<BroadcastDispatch<BuildListener>>) fDispatchers.get(broadcast)

            Class clazz =
                    Class.forName("org.gradle.internal.event.BroadcastDispatch\$ActionInvocationHandler")
            Class clazz2 =
                    Class.forName("org.gradle.listener.ClosureBackedMethodInvocationDispatch")

            def iterator = dispatchers.iterator()

            while (iterator.hasNext()) {
                try {
                    def next = iterator.next()
                    def fDispatch = next.getClass().getDeclaredField("dispatch")
                    fDispatch.setAccessible(true)
                    def dispatch = fDispatch.get(next)
                    Logger.buildOutput("dispatch:$dispatch")
                    def fMethodName
                    if (dispatch instanceof ClosureBackedMethodInvocationDispatch) {
                        fMethodName = clazz2.getDeclaredField("methodName")
                    } else if (dispatch.getClass() == clazz) {
                        fMethodName = clazz.getDeclaredField("methodName")
                    } else {
                        continue
                    }
                    fMethodName.setAccessible(true)
                    def methodName = fMethodName.get(dispatch)
                    if (methodName instanceof String && methodName.contains("projectsEvaluated")) {
                        removeDispatch.add(next)
                        iterator.remove()
                    }
                } catch (Exception ignore) {
                    ignore.printStackTrace()
                }
            }

        } catch (Exception ignore) {
            ignore.printStackTrace()
        }
        return removeDispatch
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

        MutLineLog mutLineLog = new MutLineLog()

        root.allprojects.each {
            if (it == root) return
            if (!shouldApplyComponentPlugin(it)) return
            it.pluginManager.apply(Constants.PLUGIN_COMPONENT)
            mutLineLog.build4("* " + it.name + " apply plugin: com.android.component")
        }
        Logger.buildBlockLog("子 PROJECT 注入插件", mutLineLog)
    }

    static void registerPublishTask(Project root) {
        TaskProvider<ComponentPublishTask> debugTask = root.tasks.register("ComponentPublishDebug", ComponentPublishTask.class)
        TaskProvider<ComponentPublishTask> releaseTask = root.tasks.register("ComponentPublishRelease", ComponentPublishTask.class)
    }

    static void checkPublishEnable(Project project) {
        boolean shouldShutdown = false
        project.rootProject.allprojects.each { childProject ->
            if (childProject == project) return
            if (!shouldApplyComponentPlugin(childProject)) return

            PublicationOption sdkPublication = getSdkPublication(childProject.name)
            PublicationOption implPublication = getImplPublication(childProject.name)

            if (sdkPublication == null || implPublication == null) {
                return
            }

            if (!GitUtil.canPublish(sdkPublication.sdkSourceSet.path) || !GitUtil.canPublish(implPublication.impSourceSet.path)) {
                shouldShutdown = true
            }
        }
        if (shouldShutdown) {
            throw new GradleException("Must commit and push changes before publish！！！")
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

    static void clean() {
        sSdkPublicationMap.clear()
        sImplPublicationMap.clear()
        sProjectInfoMap.clear()
        sPinConfigurations.clear()
        sAssembleModules.clear()
    }
}
