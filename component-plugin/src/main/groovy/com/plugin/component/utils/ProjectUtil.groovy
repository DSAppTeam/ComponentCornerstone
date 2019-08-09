package com.plugin.component.utils

import com.android.build.gradle.AppExtension
import com.android.build.gradle.AppPlugin
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.LibraryPlugin
import com.plugin.component.Constants
import com.plugin.component.PluginRuntime
import org.gradle.api.Project

class ProjectUtil {

    /**
     * 是否有效注入的对象，只有实现application
     * @param project
     * @return
     */
    static boolean isModulePluginTarget(Project project) {
        return project.plugins.contains(AppPlugin.class) || project.plugins.contains(LibraryPlugin.class)
    }

    /**
     * 获取项目工程的主模块名，默认 'app'
     * @return
     */
    static String getMainModuleName() {
        String name = PluginRuntime.sModuleExtension.mainModuleName
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
        if (PluginRuntime.sRunAloneMap.get(project.name) == null) {
            return false
        }
        return PluginRuntime.sRunAloneMap.get(project.name).runAlone
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

    static addSdkSourceSets(Project project) {
        BaseExtension baseExtension = project.extensions.getByName(Constants.ANDROID)
        addSdkSourceSets(baseExtension, Constants.MAIN)
        if (baseExtension instanceof AppExtension) {
            AppExtension appExtension = (AppExtension) baseExtension
            appExtension.getApplicationVariants().each {
                addSdkSourceSets(baseExtension, it.buildType.name)
                it.productFlavors.each {
                    addSdkSourceSets(baseExtension, it.name)
                }
                if (it.productFlavors.size() >= 1) {
                    if (it.productFlavors.size() > 1) {
                        addSdkSourceSets(baseExtension, it.flavorName)
                    }
                    addSdkSourceSets(baseExtension, it.name)
                }
            }
        } else if (baseExtension instanceof LibraryExtension) {
            LibraryExtension libraryExtension = (LibraryExtension) baseExtension
            libraryExtension.getLibraryVariants().each {
                addSdkSourceSets(baseExtension, it.buildType.name)
                it.productFlavors.each {
                    addSdkSourceSets(baseExtension, it.name)
                }
                if (it.productFlavors.size() >= 1) {
                    if (it.productFlavors.size() > 1) {
                        addSdkSourceSets(baseExtension, it.flavorName)
                    }
                    addSdkSourceSets(baseExtension, it.name)
                }
            }
        }
    }

    static addSdkSourceSets(BaseExtension baseExtension, String name) {
        def obj = baseExtension.sourceSets.getByName(name)
        obj.java.srcDirs.each {
            obj.aidl.srcDirs(it.absolutePath.replace(Constants.JAVA, Constants.SDK))
        }
    }

    /**
     * 是否是包含android插件
     * @param project
     * @return
     */
    static boolean hasAndroidPlugin(Project project) {
        if (project.plugins.findPlugin("com.android.application") || project.plugins.findPlugin("android") ||
                project.plugins.findPlugin("com.android.test")) {
            return true
        } else if (project.plugins.findPlugin("com.android.library") || project.plugins.findPlugin("android-library")) {
            return true
        } else {
            return false
        }
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
