package com.plugin.component.utils

import com.android.build.gradle.AppExtension
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.LibraryExtension
import com.plugin.component.Constants
import com.plugin.component.log.Logger
import com.plugin.component.Runtimes
import com.plugin.component.extension.module.ProjectInfo
import com.plugin.component.extension.option.debug.DebugConfiguration
import com.plugin.component.log.MutLineLog
import org.gradle.api.Project

class ProjectUtil {

    static Project getProject(Project root, String childName) {
        def result = null
        root.allprojects.each {
            if (it.name == getComponentValue(childName)) {
                result = it
            }
        }
        return result
    }

    static String getProjectName(String projectName) {
        if (projectName == null || projectName.isEmpty()) {
            return String
        }
        return projectName.replace(":", "")
    }

    static String getProjectName(Project project) {
        if (project == null) {
            return null
        }
        return project.name.replace(":", "")
    }

    static boolean isProjectSame(String name1, String name2) {
        return name1 == name2
    }

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
                    result.add(getProjectName(string))
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

    static List<String> getTasks(Project project) {
        return project.gradle.getStartParameter().taskNames
    }

    static String getTaskName(Project project) {
        return project.gradle.startParameter.taskNames.toString()
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

    static modifyDebugSets(Project root, ProjectInfo projectInfo) {
        if (root == null || projectInfo == null) {
            return
        }
        MutLineLog mutLineLog = new MutLineLog()
        Project project = projectInfo.project
        BaseExtension baseExtension = project.extensions.getByName(Constants.ANDROID)
        def objMain = baseExtension.sourceSets.getByName(Constants.MAIN)
        def objAndroidTest = baseExtension.sourceSets.getByName("androidTest")
        def configurations = Runtimes.getDebugConfigurations()
        if (!configurations.isEmpty()) {
            mutLineLog.build4("是否配置了调试选项 = 是")
            for (DebugConfiguration configuration : configurations) {
                def componentName = configuration.name
                def file = new File(project.projectDir, "src/main/" + componentName + "/")
                if (file == null || !file.exists()) {
                    mutLineLog.build4(" skip component[" + componentName + "] directory does not exist!")
                    continue
                }
                if (isProjectSame(componentName, Runtimes.getDebugTargetName())) {
                    mutLineLog.build4("add component[" + componentName + "] sourceSets to Main")
                    def applicationId = "com.component.debug." + componentName
                    mutLineLog.build4("修改前 debug apk applicationId", baseExtension.defaultConfig.applicationId)
                    mutLineLog.build4("修改后 debug apk applicationId", applicationId)
                    baseExtension.defaultConfig.setApplicationId(applicationId)
                    objMain.java.srcDir("src/main/" + componentName + "/java")
                    objMain.res.srcDir("src/main/" + componentName + "/res")
                    objMain.assets.srcDir("src/main/" + componentName + "/assets")
                    objMain.jniLibs.srcDir("src/main/" + componentName + "/jniLibs")
                    objMain.manifest.srcFile("src/main/" + componentName + "/AndroidManifest.xml")
                    if (configuration.dependencies.implementation != null) {
                        mutLineLog.build4("add dependencies ==> ")
                        projectInfo.project.dependencies {
                            configuration.dependencies.implementation.each {
                                def dependency = it
                                if (it instanceof String && it.startsWith(Constants.DEBUG_COMPONENT_PRE)) {
                                    dependency = PublicationUtil.parseComponent(projectInfo, it.replace(Constants.DEBUG_COMPONENT_PRE, ""))
                                }
                                mutLineLog.build4(" implementation " + dependency)
                                implementation dependency
                            }
                        }
                    }

                } else {
                    mutLineLog.build4("add component[" + componentName + "] sourceSets to AndroidTest")
                    objAndroidTest.java.srcDir("src/main/" + componentName + "/java")
                    objAndroidTest.res.srcDir("src/main/" + componentName + "/res")
                    objAndroidTest.assets.srcDir("src/main/" + componentName + "/assets")
                    objAndroidTest.jniLibs.srcDir("src/main/" + componentName + "/jniLibs")
                    objAndroidTest.manifest.srcFile("src/main/" + componentName + "/AndroidManifest.xml")
                }
            }
        } else {
            mutLineLog.build4("是否配置了调试选项 = 否")
        }
        mutLineLog.build4("* DebugModule[" + project.name + "]" + "Main sourceSets")
        mutLineLog.build4(" java = " + sourceSetDirToString(objMain.java.srcDirs))
        mutLineLog.build4(" res = " + sourceSetDirToString(objMain.res.srcDirs))
        mutLineLog.build4(" assets = " + sourceSetDirToString(objMain.assets.srcDirs))
        mutLineLog.build4(" jniLibs = " + sourceSetDirToString(objMain.jniLibs.srcDirs))
        mutLineLog.build4(" manifest = " + objMain.manifest.srcFile.path)
        mutLineLog.build4("* DebugModule[" + project.name + "]" + "AndroidTest sourceSets")
        mutLineLog.build4(" java = " + sourceSetDirToString(objAndroidTest.java.srcDirs))
        mutLineLog.build4(" res = " + sourceSetDirToString(objAndroidTest.res.srcDirs))
        mutLineLog.build4(" assets = " + sourceSetDirToString(objAndroidTest.assets.srcDirs))
        mutLineLog.build4(" jniLibs = " + sourceSetDirToString(objAndroidTest.jniLibs.srcDirs))
        mutLineLog.build4(" manifest = " + objAndroidTest.manifest.srcFile.path)

        Logger.buildBlockLog("调试模块调整 SOURCESET ", mutLineLog)
    }

    private static String sourceSetDirToString(Set<File> set) {
        StringBuilder stringBuilder = new StringBuilder()
        if (set != null && !set.isEmpty()) {
            if (set.size() == 1) {
                for (File file : set) {
                    stringBuilder.append("[ " + file.path + " ]")
                }
            } else {
                stringBuilder.append("[ \n")
                for (File file : set) {
                    stringBuilder.append("                      " + file.path + "\n")
                }
                stringBuilder.append("                 ]")
            }
        }
        return stringBuilder.toString()
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
