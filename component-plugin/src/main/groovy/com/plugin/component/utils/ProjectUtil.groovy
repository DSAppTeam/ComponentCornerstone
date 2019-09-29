package com.plugin.component.utils

import com.android.build.gradle.AppExtension
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.LibraryExtension
import com.plugin.component.Constants
import com.plugin.component.Logger
import com.plugin.component.Runtimes
import com.plugin.component.extension.module.ProjectInfo
import com.plugin.component.extension.option.DebugOption
import com.plugin.component.transform.InjectCodeTransform
import com.plugin.component.transform.ScanCodeTransform
import org.gradle.api.Project

class ProjectUtil {

    /**
     * 解析include exclude
     * @param modules
     * @return
     */
    static Set<String> getModuleName(String modules) {
        Set<String> result = new HashSet<>()
        if (modules != null && !modules.isEmpty()) {
            String[] strings = modules.split(",")
            if (strings != null && strings.length > 0) {
                for (String string : strings) {
                    if (string.startsWith(":")) {
                        result.add(string.substring(1, string.length()))
                    } else {
                        result.add(string)
                    }
                }
            }
        }
        return result
    }

    /**
     * example: 兼容 component(:library) 和 component(library)
     * @param componentValue
     * @return
     */
    static String getComponentValue(String componentValue) {
        if (componentValue == null || componentValue.isEmpty()) {
            return componentValue
        }
        if (componentValue.startsWith(":")) {
            componentValue = componentValue.substring(1, componentValue.length())
        }
        return componentValue
    }

    /**
     * 获取项目工程的主模块名，默认 'app'
     * @return
     */
    static String getMainModuleName() {
        String name = Runtimes.sMainModuleName
        if (name == null || name.isEmpty()) {
            return Constants.DEFAULT_MAIN_MODULE_NAME
        }
        return name
    }

    /**
     * 如果当前就是工程主模块，则默认 ture
     * 否则则判断是否有配置脚本
     * @param project
     * @return
     */
    static boolean isRunAlone(Project project) {
        if ((getModuleName(project)) == getMainModuleName()) {
            return true
        }
        if (Runtimes.getDebugInfo(project.name) == null) {
            return false
        }
        return Runtimes.getDebugInfo(project.name).enable
    }

    static List<String> getTasks(Project project) {
        return project.gradle.getStartParameter().taskNames
    }

    static String getTaskName(Project project) {
        return project.gradle.startParameter.taskNames.toString()
    }

    static String getModuleName(Project project) {
        return project.path.replace(":", "")
    }

    static boolean containValidPluginDefine(String string) {
        return string.contains("apply") &&
                string.contains("plugin") && (string.contains("com.android.library") || string.contains("com.android.application"))
    }

    static modifySourceSets(ProjectInfo projectInfo) {
        Project project = projectInfo.project
        BaseExtension baseExtension = project.extensions.getByName(Constants.ANDROID)
        modifySourceSets(baseExtension, Constants.MAIN)
        if (baseExtension instanceof AppExtension) {
            AppExtension appExtension = (AppExtension) baseExtension
            appExtension.getApplicationVariants().each {
                modifySourceSets(baseExtension, it.buildType.name)
                it.productFlavors.each {
                    modifySourceSets(baseExtension, it.name)
                }
                if (it.productFlavors.size() >= 1) {
                    if (it.productFlavors.size() > 1) {
                        modifySourceSets(baseExtension, it.flavorName)
                    }
                    modifySourceSets(baseExtension, it.name)
                }
            }
        } else if (baseExtension instanceof LibraryExtension) {
            LibraryExtension libraryExtension = (LibraryExtension) baseExtension
            libraryExtension.getLibraryVariants().each {
                modifySourceSets(baseExtension, it.buildType.name)
                it.productFlavors.each {
                    modifySourceSets(baseExtension, it.name)
                }
                if (it.productFlavors.size() >= 1) {
                    if (it.productFlavors.size() > 1) {
                        modifySourceSets(baseExtension, it.flavorName)
                    }
                    modifySourceSets(baseExtension, it.name)
                }
            }
        }
    }

    static modifySourceSets(BaseExtension baseExtension, String name) {
        def obj = baseExtension.sourceSets.getByName(name)
        obj.java.srcDirs.each {
            obj.aidl.srcDirs(it.absolutePath.replace(Constants.JAVA, Constants.SDK))
        }
    }

    static modifyDebugSets(ProjectInfo projectInfo) {
        Project project = projectInfo.project
        Logger.buildOutput("project[" + project.name + "] registerTransform => ScanCodeTransform")
        Logger.buildOutput("project[" + project.name + "] registerTransform => InjectCodeTransform")
        Logger.buildOutput("project[" + project.name + "] add sourceSets debug")
        BaseExtension baseExtension = project.extensions.getByName(Constants.ANDROID)
        def obj = baseExtension.sourceSets.getByName(Constants.MAIN)
        Set<String> projectsName = Runtimes.sDebugMap.keySet()
        def targetDebug = Constants.MANIFEST
        for (String projectName : projectsName) {
//            project.android.sourceSets {
//                main {
//                    manifest.srcFile "src/main/${projectName}/AndroidManifest.xml"
//                    java.srcDirs = [Constants.JAVA_PATH, "src/main/${projectName}/java"]
//                    res.srcDirs = [Constants.RES_PATH, "src/main/${projectName}/res"]
//                    assets.srcDirs = [Constants.ASSETS_PATH, "src/main/${projectName}/assets"]
//                    jniLibs.srcDirs = [Constants.JNILIBS_PATH, "src/main/${projectName}/jniLibs"]
//                }
//            }

            DebugOption debugOption = Runtimes.getDebugInfo(projectName)
            List<String> javaPath = new ArrayList<>()
            javaPath.add(Constants.JAVA_PATH)
            javaPath.add("src/main/library/java")
            obj.java.setSrcDirs(javaPath)

            List<String> resPath = new ArrayList<>()
            resPath.add(Constants.RES_PATH)
            resPath.add("src/main/library/res")
            obj.res.setSrcDirs(resPath)

            List<String> assetsPath = new ArrayList<>()
            assetsPath.add(Constants.ASSETS_PATH)
            assetsPath.add("src/main/library/assets")
            obj.assets.setSrcDirs(assetsPath)

            List<String> jniPath = new ArrayList<>()
            jniPath.add(Constants.JNILIBS_PATH)
            jniPath.add("src/main/library/jniLibs")
            obj.jniLibs.setSrcDirs(jniPath)


//            if (debugOption.enable) {
//                obj.manifest.srcFile("src/main/library/AndroidManifest.xml")
////                targetDebug = "src/main/${projectName}/AndroidManifest.xml"
//            }
        }
//        obj.manifest.srcFile(targetDebug)
    }

    /**
     * 返回项目设置的android jar路径
     * @param project
     * @param compileSdkVersion
     * @return
     */
    static String getAndroidJarPath(Project project, int compileSdkVersion) {
        def androidHome
        def env = System.getenv()

        //获取android jar路径
        if (env[Constants.ANDROID_HOME] != null) {
            androidHome = env[Constants.ANDROID_HOME]
        } else {
            def localProperties = new File(project.rootProject.rootDir, Constants.LOCAL_PROPERTIES)
            if (localProperties.exists()) {
                Properties properties = new Properties()
                localProperties.withInputStream { instr ->
                    properties.load(instr)
                }
                androidHome = properties.getProperty('sdk.dir')
            }
        }

        if (compileSdkVersion == 0) {
            throw new RuntimeException("component compileSdkVersion is not specified.")
        }

        def androidJar = new File(androidHome, "/platforms/android-${compileSdkVersion}/android.jar")
        if (!androidJar.exists()) {
            throw new RuntimeException("Failed to find Platform SDK with path: platforms;android-$compileSdkVersion")
        }
        return androidJar.absolutePath
    }
}
