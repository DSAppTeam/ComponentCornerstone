package com.plugin.component.plugin

import com.android.build.gradle.AppExtension
import com.android.build.gradle.AppPlugin
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.LibraryPlugin
import com.android.build.gradle.TestedExtension
import com.android.build.gradle.api.BaseVariant
import com.android.builder.model.ProductFlavor
import com.plugin.component.Runtimes
import com.plugin.component.extension.module.PinInfo
import com.plugin.component.extension.option.pin.PinConfiguration
import com.plugin.component.utils.PinUtils
import com.plugin.component.utils.ProjectUtil
import org.gradle.BuildListener
import org.gradle.BuildResult
import org.gradle.api.Action
import org.gradle.api.DomainObjectSet
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.initialization.Settings
import org.gradle.api.invocation.Gradle

class PinPlugin extends BasePlugin {

    private final static String NORMAL = 'normal'
    private final static String ASSEMBLE_OR_GENERATE = 'assemble_or_generate'

    private final static String APPLY_NORMAL_MICRO_MODULE_SCRIPT = 'apply_normal_micro_module_script'
    private final static String APPLY_INCLUDE_MICRO_MODULE_SCRIPT = 'apply_include_micro_module_script'
    private final static String APPLY_EXPORT_MICRO_MODULE_SCRIPT = 'apply_export_micro_module_script'

    String startTaskState = NORMAL

    String applyScriptState
    boolean appliedLibraryPlugin

    PinConfiguration pinConfiguration
    PinInfo currentPin
    Project project


    boolean isSupportPins() {
        return pinConfiguration != null
    }

    @Override
    void evaluateBeforeAndroidPlugin(Project project) {

        this.project = project
        pinConfiguration = Runtimes.getPinConfiguration(project.name)
        if (!isSupportPins()) {
            return
        }

        //是否是sync
        if (project.gradle.getStartParameter().taskNames.size() == 0) {
            startTaskState = NORMAL
        } else {
            startTaskState = ASSEMBLE_OR_GENERATE
        }

        //非sync情况下，在configuration添加的时候添加依赖的时候进行处理
        if (startTaskState != NORMAL) {
            project.getConfigurations().whenObjectAdded {
                Configuration configuration = it
                configuration.dependencies.whenObjectAdded {
                    if (applyScriptState == APPLY_INCLUDE_MICRO_MODULE_SCRIPT) {
                        configuration.dependencies.remove(it)
                        return
                    } else if (applyScriptState == APPLY_NORMAL_MICRO_MODULE_SCRIPT
                            || applyScriptState == APPLY_EXPORT_MICRO_MODULE_SCRIPT) {
                        return
                    } else if (currentPin == null && startTaskState == ASSEMBLE_OR_GENERATE) {
                        return
                    } else if (it.group != null && it.group.startsWith('com.android.tools')) {
                        return
                    }

                    configuration.dependencies.remove(it)
                }
            }
        }

        project.dependencies.metaClass.microModule { String path ->
            if (currentPin == null || applyScriptState == APPLY_NORMAL_MICRO_MODULE_SCRIPT) {
                return []
            }

            if (applyScriptState == APPLY_INCLUDE_MICRO_MODULE_SCRIPT) {
                pinConfiguration.setPinDependency(currentPin.name, path)
                return []
            }

            PinInfo microModule = pinConfiguration.getIncludePin(path)

            def result = []
            if (startTaskState == ASSEMBLE_OR_GENERATE) {
                PinUtils.addMicroModuleSourceSet(project, microModule, pinConfiguration.productFlavorInfo)
                PinUtils.applyMicroModuleScript(project, microModule, currentPin)
                microModule.appliedScript = true
            }
            return result
        }

        project.plugins.all {
            Class extensionClass
            if (it instanceof AppPlugin) {
                extensionClass = AppExtension
            } else if (it instanceof LibraryPlugin) {
                extensionClass = LibraryExtension
            } else {
                return
            }
            project.extensions.configure(extensionClass, new Action<? extends TestedExtension>() {
                @Override
                void execute(TestedExtension testedExtension) {
                    boolean isLibrary
                    DomainObjectSet<BaseVariant> baseVariants
                    if (testedExtension instanceof AppExtension) {
                        AppExtension appExtension = (AppExtension) testedExtension
                        baseVariants = appExtension.applicationVariants
                    } else {
                        LibraryExtension libraryExtension = (LibraryExtension) testedExtension
                        baseVariants = libraryExtension.libraryVariants
                        isLibrary = true
                    }

                    baseVariants.all { BaseVariant variant ->
                        if (pinConfiguration.codeCheckEnabled) {
                            def taskNamePrefix = isLibrary ? 'package' : 'merge'
                            List<String> sourceFolders = new ArrayList<>()
                            sourceFolders.add('main')
                            sourceFolders.add(variant.buildType.name)
                            if (variant.productFlavors.size() > 0) {
                                sourceFolders.add(variant.name)
                                sourceFolders.add(variant.flavorName)
                                for (ProductFlavor productFlavor : variant.productFlavors) {
                                    sourceFolders.add(productFlavor.name)
                                }
                                PinUtils.checkMicroModuleBoundary(project, pinConfiguration, taskNamePrefix, variant.buildType.name, variant.flavorName, sourceFolders)
                            } else {
                                PinUtils.checkMicroModuleBoundary(project, pinConfiguration, taskNamePrefix, variant.buildType.name, null, sourceFolders)
                            }
                        }
                    }
                }
            })
        }
    }

    @Override
    void afterEvaluateBeforeAndroidPlugin(Project project) {

        if (!isSupportPins()) {
            return
        }

        pinConfiguration.initProductFlavor()

        if (pinConfiguration.mainPin == null) {
            throw new GradleException("the main PinInfo could not be found in ${project.getDisplayName()}.")
        }

        appliedLibraryPlugin = project.pluginManager.hasPlugin('com.android.library')
        applyScriptState = APPLY_INCLUDE_MICRO_MODULE_SCRIPT
        pinConfiguration.includePins.each {
            PinInfo microModule = it.value
            pinConfiguration.dependencyGraph.add(microModule.name)
            applyMicroModuleScript(project, microModule)
        }

        //清除所有 srouceSet 信息
        PinUtils.clearOriginSourceSet(project, pinConfiguration.productFlavorInfo)

        //如果非 sync
        if (startTaskState == ASSEMBLE_OR_GENERATE) {
            //读取所有 export 配置
            applyScriptState = APPLY_EXPORT_MICRO_MODULE_SCRIPT
            boolean hasExportMainMicroModule = false
            boolean isEmpty = pinConfiguration.exportPins.isEmpty()
            List<String> dependencySort = pinConfiguration.dependencyGraph.topSort()
            dependencySort.each {
                if (isEmpty || pinConfiguration.exportPins.containsKey(it)) {
                    PinInfo microModule = pinConfiguration.getIncludePin(it)
                    if (microModule == null) {
                        throw new GradleException("PinInfo with path '${it}' could not be found in ${project.getDisplayName()}.")
                    }

                    if (microModule == pinConfiguration.mainPin) {
                        hasExportMainMicroModule = true
                    }

                    if (microModule.appliedScript) return

                    PinUtils.addMicroModuleSourceSet(project, microModule, pinConfiguration.productFlavorInfo)
                    applyMicroModuleScript(project, microModule)
                    microModule.appliedScript = true
                }
            }

            if (!hasExportMainMicroModule) {
                throw new GradleException("the main PinInfo '${pinConfiguration.mainPin.name}' is not in the export list.")
            }
        } else {
            applyScriptState = APPLY_NORMAL_MICRO_MODULE_SCRIPT
            pinConfiguration.includePins.each {
                PinInfo microModule = it.value
                PinUtils.addMicroModuleSourceSet(project, microModule, pinConfiguration.productFlavorInfo)
                applyMicroModuleScript(project, microModule)
            }
        }
        currentPin = null

        PinUtils.generateAndroidManifest(project, pinConfiguration, startTaskState)

        project.tasks.preBuild.doFirst {
            PinUtils.clearOriginSourceSet(project, pinConfiguration.productFlavorInfo)
            if (startTaskState == ASSEMBLE_OR_GENERATE) {
                pinConfiguration.includePins.each {
                    PinInfo pin = it.value
                    if (pin.appliedScript) {
                        PinUtils.addMicroModuleSourceSet(project, pin, pinConfiguration.productFlavorInfo)
                    }
                }
            } else {
                pinConfiguration.includePins.each {
                    PinUtils.addMicroModuleSourceSet(project, it.value, pinConfiguration.productFlavorInfo)
                }
            }
            PinUtils.generateAndroidManifest(project, pinConfiguration, startTaskState)
        }
    }

    @Override
    void afterAllEvaluate(Project root) {
        if (Runtimes.hasPinModule()) {
            root.gradle.addBuildListener(new BuildListener() {
                @Override
                void buildStarted(Gradle gradle) {

                }

                @Override
                void settingsEvaluated(Settings settings) {

                }

                @Override
                void projectsLoaded(Gradle gradle) {

                }

                @Override
                void projectsEvaluated(Gradle gradle) {

                }

                @Override
                void buildFinished(BuildResult buildResult) {
                    // generate microModules.xml for PinInfo IDEA plugin.
                    def ideaFile = new File(buildResult.gradle.rootProject.rootDir, '.idea')
                    if (!ideaFile.exists()) return
                    def pininfos = '<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<modules>\n'

                    Map<String, PinConfiguration> allPins = Runtimes.getPinConfigurations()
                    Set<String> moduleNames = allPins.keySet()
                    for (String moduleName : moduleNames) {
                        PinConfiguration pins = allPins.get(moduleName)
                        Project project = ProjectUtil.getProject(root, pins.name)
                        if (project != null) {
                            def displayName = project.displayName
                            pininfos += '    <module name=\"' + displayName.substring(displayName.indexOf("'") + 1, displayName.lastIndexOf("'")) + '\" path=\"' + project.projectDir.getCanonicalPath() + '\">\n'
                            pins.includePins.each {
                                PinInfo pin = it.value
                                pininfos += '        <pin name=\"' + pin.name + '\" path=\"' + pin.pinDir.getCanonicalPath() + '\" />\n'
                            }
                            pininfos += '    </module>\n'
                        }
                    }

                    pininfos += '</modules>'

                    def pins = new File(ideaFile, 'modulePins.xml')
                    pins.write(pininfos, 'utf-8')
                }
            })
        }
    }


    void applyMicroModuleScript(Project project, PinInfo microModule) {
        def pinBuild = new File(microModule.pinDir, 'build.gradle')
        if (pinBuild.exists()) {
            PinInfo tempMicroModule = currentPin
            currentPin = microModule
            project.apply from: pinBuild.absolutePath
            currentPin = tempMicroModule
        }
    }
}
