package com.plugin.pins

import com.android.build.gradle.*
import com.android.build.gradle.api.BaseVariant
import com.android.builder.model.ProductFlavor
import com.android.manifmerger.ManifestMerger2
import com.android.manifmerger.MergingReport
import com.android.manifmerger.XmlDocument
import com.android.utils.ILogger
import com.plugin.pins.check.CodeChecker
import com.plugin.pins.extension.DefaultMicroModuleExtension
import com.plugin.pins.extension.OnMicroModuleListener
import org.gradle.BuildListener
import org.gradle.BuildResult
import org.gradle.api.*
import org.gradle.api.artifacts.Configuration
import org.gradle.api.initialization.Settings
import org.gradle.api.invocation.Gradle

class MicroModulePlugin implements Plugin<Project> {

    private final static String NORMAL = 'normal'
    private final static String ASSEMBLE_OR_GENERATE = 'assemble_or_generate'

    private final static String APPLY_NORMAL_MICRO_MODULE_SCRIPT = 'apply_normal_micro_module_script'
    private final static String APPLY_INCLUDE_MICRO_MODULE_SCRIPT = 'apply_include_micro_module_script'
    private final static String APPLY_EXPORT_MICRO_MODULE_SCRIPT = 'apply_export_micro_module_script'

    private final static BuildListener buildListener = new BuildListener() {

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
            // generate microModules.xml for MicroModule IDEA plugin.
            def ideaFile = new File(buildResult.gradle.rootProject.rootDir, '.idea')
            if (!ideaFile.exists()) return

            def microModuleInfo = '<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<modules>\n'
            buildResult.gradle.rootProject.allprojects.each {
                MicroModulePlugin microModulePlugin = it.plugins.findPlugin('micro-module')
                if (microModulePlugin == null) return

                def displayName = it.displayName
                microModuleInfo += '    <module name=\"' + displayName.substring(displayName.indexOf("'") + 1, displayName.lastIndexOf("'")) + '\" path=\"' + it.projectDir.getCanonicalPath() + '\">\n'
                microModulePlugin.microModuleInfo.includeMicroModules.each {
                    MicroModule microModule = it.value
                    microModuleInfo += '        <microModule name=\"' + microModule.name + '\" path=\"' + microModule.microModuleDir.getCanonicalPath() + '\" />\n'
                }
                microModuleInfo += '    </module>\n'
            }
            microModuleInfo += '</modules>'

            def microModules = new File(ideaFile, 'microModules.xml')
            microModules.write(microModuleInfo, 'utf-8')
        }
    }

    Project project

    String startTaskState = NORMAL

    MicroModuleInfo microModuleInfo
    ProductFlavorInfo productFlavorInfo

    MicroModule currentMicroModule
    String applyScriptState

    boolean appliedLibraryPlugin

    boolean clearedOriginSourceSets

    void apply(Project project) {
        this.project = project
        this.microModuleInfo = new MicroModuleInfo(project)

        project.gradle.removeListener(buildListener)
        project.gradle.addBuildListener(buildListener)

        if (project.gradle.getStartParameter().taskNames.size() == 0) {
            startTaskState = NORMAL
        } else {
            startTaskState = ASSEMBLE_OR_GENERATE
        }

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
                    } else if (currentMicroModule == null && startTaskState == ASSEMBLE_OR_GENERATE) {
                        return
                    } else if (it.group != null && it.group.startsWith('com.android.tools')) {
                        return
                    }

                    configuration.dependencies.remove(it)
                }
            }
        }

        DefaultMicroModuleExtension microModuleExtension = project.extensions.create(MicroModuleExtension, 'microModule', DefaultMicroModuleExtension, project)
        microModuleExtension.onMicroModuleListener = new OnMicroModuleListener() {

            @Override
            void addIncludeMicroModule(MicroModule microModule, boolean mainMicroModule) {
                if (mainMicroModule) {
                    microModuleInfo.setMainMicroModule(microModule)
                } else {
                    microModuleInfo.addIncludeMicroModule(microModule)
                }

                if(!clearedOriginSourceSets) {
                    productFlavorInfo = new ProductFlavorInfo(project)
                    clearedOriginSourceSets = true
                    clearOriginSourceSet()

                    if(microModuleInfo.mainMicroModule != null) {
                        addMicroModuleSourceSet(microModuleInfo.mainMicroModule)
                    }
                }

                addMicroModuleSourceSet(microModule)
            }

            @Override
            void addExportMicroModule(String... microModulePaths) {
                microModulePaths.each {
                    microModuleInfo.addExportMicroModule(it)
                }
            }

        }

        project.dependencies.metaClass.microModule { String path ->
            if (currentMicroModule == null || applyScriptState == APPLY_NORMAL_MICRO_MODULE_SCRIPT) {
                return []
            }

            if (applyScriptState == APPLY_INCLUDE_MICRO_MODULE_SCRIPT) {
                microModuleInfo.setMicroModuleDependency(currentMicroModule.name, path)
                return []
            }

            MicroModule microModule = microModuleInfo.getMicroModule(path)

            def result = []
            if (startTaskState == ASSEMBLE_OR_GENERATE) {
                addMicroModuleSourceSet(microModule)
                applyMicroModuleScript(microModule)
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
                        if (microModuleExtension.codeCheckEnabled) {
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
                                checkMicroModuleBoundary(taskNamePrefix, variant.buildType.name, variant.flavorName, sourceFolders)
                            } else {
                                checkMicroModuleBoundary(taskNamePrefix, variant.buildType.name, null, sourceFolders)
                            }
                        }
                    }
                }
            })
        }

        project.afterEvaluate {
            microModuleExtension.onMicroModuleListener = null
            if (microModuleInfo.mainMicroModule == null) {
                throw new GradleException("the main MicroModule could not be found in ${project.getDisplayName()}.")
            }

            appliedLibraryPlugin = project.pluginManager.hasPlugin('com.android.library')

            productFlavorInfo = new ProductFlavorInfo(project)

            applyScriptState = APPLY_INCLUDE_MICRO_MODULE_SCRIPT
            microModuleInfo.includeMicroModules.each {
                MicroModule microModule = it.value
                microModuleInfo.dependencyGraph.add(microModule.name)
                applyMicroModuleScript(microModule)
            }

            clearOriginSourceSet()
            if (startTaskState == ASSEMBLE_OR_GENERATE) {
                applyScriptState = APPLY_EXPORT_MICRO_MODULE_SCRIPT
                boolean hasExportMainMicroModule = false
                boolean isEmpty = microModuleInfo.exportMicroModules.isEmpty()
                List<String> dependencySort = microModuleInfo.dependencyGraph.topSort()
                dependencySort.each {
                    if (isEmpty || microModuleInfo.exportMicroModules.containsKey(it)) {
                        MicroModule microModule = microModuleInfo.getMicroModule(it)
                        if (microModule == null) {
                            throw new GradleException("MicroModule with path '${it}' could not be found in ${project.getDisplayName()}.")
                        }

                        if (microModule == microModuleInfo.mainMicroModule) {
                            hasExportMainMicroModule = true
                        }

                        if (microModule.appliedScript) return

                        addMicroModuleSourceSet(microModule)
                        applyMicroModuleScript(microModule)
                        microModule.appliedScript = true
                    }
                }

                if (!hasExportMainMicroModule) {
                    throw new GradleException("the main MicroModule '${microModuleInfo.mainMicroModule.name}' is not in the export list.")
                }
            } else {
                applyScriptState = APPLY_NORMAL_MICRO_MODULE_SCRIPT
                microModuleInfo.includeMicroModules.each {
                    MicroModule microModule = it.value
                    addMicroModuleSourceSet(microModule)
                    applyMicroModuleScript(microModule)
                }
            }
            currentMicroModule = null

            generateAndroidManifest()

            project.tasks.preBuild.doFirst {
                clearOriginSourceSet()
                if (startTaskState == ASSEMBLE_OR_GENERATE) {
                    microModuleInfo.includeMicroModules.each {
                        MicroModule microModule = it.value
                        if (microModule.appliedScript) {
                            addMicroModuleSourceSet(microModule)
                        }
                    }
                } else {
                    microModuleInfo.includeMicroModules.each {
                        addMicroModuleSourceSet(it.value)
                    }
                }
                generateAndroidManifest()
            }
        }
    }

    def generateAndroidManifest() {
        if ((startTaskState == ASSEMBLE_OR_GENERATE || !microModuleInfo.exportMicroModules.isEmpty()) && isMainSourceSetEmpty()) {
            setMainSourceSetManifest()
            return
        }
        mergeAndroidManifest('main')

        productFlavorInfo.buildTypes.each {
            mergeAndroidManifest(it)
        }

        if (!productFlavorInfo.singleDimension) {
            productFlavorInfo.productFlavors.each {
                mergeAndroidManifest(it)
            }
        }

        productFlavorInfo.combinedProductFlavors.each {
            mergeAndroidManifest(it)

            def productFlavor = it
            productFlavorInfo.buildTypes.each {
                mergeAndroidManifest(productFlavor + Utils.upperCase(it))
            }
        }

        def androidTest = 'androidTest'
        mergeAndroidManifest(androidTest)
        mergeAndroidManifest(androidTest + 'Debug')
        if (!productFlavorInfo.singleDimension) {
            productFlavorInfo.productFlavors.each {
                mergeAndroidManifest(androidTest + Utils.upperCase(it))
            }
        }
        productFlavorInfo.combinedProductFlavors.each {
            mergeAndroidManifest(androidTest + Utils.upperCase(it))
            mergeAndroidManifest(androidTest + Utils.upperCase(it) + 'Debug')
        }
    }

    def mergeAndroidManifest(String variantName) {
        File mainManifestFile = new File(microModuleInfo.mainMicroModule.microModuleDir, "/src/${variantName}/AndroidManifest.xml")
        if (!mainManifestFile.exists()) return
        ManifestMerger2.MergeType mergeType = ManifestMerger2.MergeType.APPLICATION
        XmlDocument.Type documentType = XmlDocument.Type.MAIN
        def logger = new ILogger() {
            @Override
            void error(Throwable t, String msgFormat, Object... args) {
                println(msgFormat)
            }

            @Override
            void warning(String msgFormat, Object... args) {

            }

            @Override
            void info(String msgFormat, Object... args) {

            }

            @Override
            void verbose(String msgFormat, Object... args) {

            }
        }
        ManifestMerger2.Invoker invoker = new ManifestMerger2.Invoker(mainManifestFile, logger, mergeType, documentType)
        invoker.withFeatures(ManifestMerger2.Invoker.Feature.NO_PLACEHOLDER_REPLACEMENT)

        microModuleInfo.includeMicroModules.each {
            MicroModule microModule = it.value
            if (startTaskState == ASSEMBLE_OR_GENERATE && !microModule.appliedScript) return
            if (microModule.name == microModuleInfo.mainMicroModule.name) return
            def microManifestFile = new File(microModule.microModuleDir, "/src/${variantName}/AndroidManifest.xml")
            if (microManifestFile.exists()) {
                invoker.addLibraryManifest(microManifestFile)
            }
        }

        def mergingReport = invoker.merge()
        if (!mergingReport.result.success) {
            mergingReport.log(logger)
            throw new GradleException(mergingReport.reportString)
        }
        def moduleAndroidManifest = mergingReport.getMergedDocument(MergingReport.MergedManifestKind.MERGED)
        moduleAndroidManifest = new String(moduleAndroidManifest.getBytes('UTF-8'))

        def saveDir = new File(project.projectDir, "build/microModule/merge-manifest/${variantName}")
        saveDir.mkdirs()
        def AndroidManifestFile = new File(saveDir, 'AndroidManifest.xml')
        AndroidManifestFile.createNewFile()
        AndroidManifestFile.write(moduleAndroidManifest)

        def extensionContainer = project.getExtensions()
        BaseExtension android = extensionContainer.getByName('android')
        def obj = android.sourceSets.findByName(variantName)
        if (obj == null) {
            return
        }
        obj.manifest.srcFile project.projectDir.absolutePath + "/build/microModule/merge-manifest/${variantName}/AndroidManifest.xml"
    }

    def addMicroModuleSourceSet(MicroModule microModule) {
        addVariantSourceSet(microModule, 'main')

        productFlavorInfo.buildTypes.each {
            addVariantSourceSet(microModule, it)
        }

        if (!productFlavorInfo.singleDimension) {
            productFlavorInfo.productFlavors.each {
                addVariantSourceSet(microModule, it)
            }
        }

        productFlavorInfo.combinedProductFlavors.each {
            addVariantSourceSet(microModule, it)
            def flavorName = it
            productFlavorInfo.buildTypes.each {
                addVariantSourceSet(microModule, flavorName + Utils.upperCase(it))
            }
        }

        def testTypes = ['androidTest', 'test']
        testTypes.each {
            def testType = it
            addVariantSourceSet(microModule, testType)

            if (testType == 'test') {
                productFlavorInfo.buildTypes.each {
                    addVariantSourceSet(microModule, testType + Utils.upperCase(it))
                }
            } else {
                addVariantSourceSet(microModule, testType + 'Debug')
            }

            if (!productFlavorInfo.singleDimension) {
                productFlavorInfo.productFlavors.each {
                    addVariantSourceSet(microModule, testType + Utils.upperCase(it))
                }
            }

            productFlavorInfo.combinedProductFlavors.each {
                def productFlavorName = testType + Utils.upperCase(it)
                addVariantSourceSet(microModule, productFlavorName)

                if (testType == 'test') {
                    productFlavorInfo.buildTypes.each {
                        addVariantSourceSet(microModule, productFlavorName + Utils.upperCase(it))
                    }
                } else {
                    addVariantSourceSet(microModule, productFlavorName + 'Debug')
                }
            }
        }
    }

    def clearOriginSourceSet() {
        clearModuleSourceSet('main')

        // buildTypes
        productFlavorInfo.buildTypes.each {
            clearModuleSourceSet(it)
        }

        if (!productFlavorInfo.singleDimension) {
            productFlavorInfo.productFlavors.each {
                clearModuleSourceSet(it)
            }
        }

        productFlavorInfo.combinedProductFlavors.each {
            clearModuleSourceSet(it)
            def flavorName = it
            productFlavorInfo.buildTypes.each {
                clearModuleSourceSet(flavorName + Utils.upperCase(it))
            }
        }

        def testTypes = ['androidTest', 'test']
        testTypes.each {
            def testType = it
            clearModuleSourceSet(testType)

            if (testType == 'test') {
                productFlavorInfo.buildTypes.each {
                    clearModuleSourceSet(testType + Utils.upperCase(it))
                }
            } else {
                clearModuleSourceSet(testType + 'Debug')
            }

            if (!productFlavorInfo.singleDimension) {
                productFlavorInfo.productFlavors.each {
                    clearModuleSourceSet(testType + Utils.upperCase(it))
                }
            }

            productFlavorInfo.combinedProductFlavors.each {
                def productFlavorName = testType + Utils.upperCase(it)
                clearModuleSourceSet(productFlavorName)

                if (testType == 'test') {
                    productFlavorInfo.buildTypes.each {
                        clearModuleSourceSet(productFlavorName + Utils.upperCase(it))
                    }
                } else {
                    clearModuleSourceSet(productFlavorName + 'Debug')
                }
            }
        }
    }

    def isMainSourceSetEmpty() {
        BaseExtension android = project.extensions.getByName('android')
        def obj = android.sourceSets.findByName('main')
        if (obj == null) {
            return true
        }
        return obj.java.srcDirs.size() == 0;
    }

    def setMainSourceSetManifest() {
        BaseExtension android = project.extensions.getByName('android')
        def obj = android.sourceSets.findByName('main')
        if (obj == null) {
            obj = android.sourceSets.create('main')
        }
        File mainManifestFile = new File(microModuleInfo.mainMicroModule.microModuleDir, '/src/main/AndroidManifest.xml')
        obj.manifest.srcFile mainManifestFile
    }

    def addVariantSourceSet(MicroModule microModule, def type) {
        def absolutePath = microModule.microModuleDir.absolutePath
        BaseExtension android = project.extensions.getByName('android')
        def obj = android.sourceSets.findByName(type)
        if (obj == null) {
            obj = android.sourceSets.create(type)
        }

        obj.java.srcDir(absolutePath + "/src/${type}/java")
        obj.java.srcDir(absolutePath + "/src/${type}/kotlin")
        obj.res.srcDir(absolutePath + "/src/${type}/res")
        obj.jni.srcDir(absolutePath + "/src/${type}/jni")
        obj.jniLibs.srcDir(absolutePath + "/src/${type}/jniLibs")
        obj.aidl.srcDir(absolutePath + "/src/${type}/aidl")
        obj.assets.srcDir(absolutePath + "/src/${type}/assets")
        obj.shaders.srcDir(absolutePath + "/src/${type}/shaders")
        obj.resources.srcDir(absolutePath + "/src/${type}/resources")
        obj.renderscript.srcDir(absolutePath + "/src/${type}/rs")
    }

    def clearModuleSourceSet(def type) {
        def srcDirs = []
        BaseExtension android = project.extensions.getByName('android')
        def obj = android.sourceSets.findByName(type)
        if (obj == null) {
            return
        }
        obj.java.srcDirs = srcDirs
        obj.res.srcDirs = srcDirs
        obj.jni.srcDirs = srcDirs
        obj.jniLibs.srcDirs = srcDirs
        obj.aidl.srcDirs = srcDirs
        obj.assets.srcDirs = srcDirs
        obj.shaders.srcDirs = srcDirs
        obj.resources.srcDirs = srcDirs
        obj.renderscript.srcDirs = srcDirs
    }

    void applyMicroModuleScript(MicroModule microModule) {
        def microModuleBuild = new File(microModule.microModuleDir, 'build.gradle')
        if (microModuleBuild.exists()) {
            MicroModule tempMicroModule = currentMicroModule
            currentMicroModule = microModule
            project.apply from: microModuleBuild.absolutePath
            currentMicroModule = tempMicroModule
        }
    }

    def checkMicroModuleBoundary(String taskPrefix, String buildType, String flavorName, List<String> sourceFolders) {
        CodeChecker codeChecker

        def buildTypeFirstUp = Utils.upperCase(buildType)
        def productFlavorFirstUp = flavorName != null ? Utils.upperCase(flavorName) : ""

        def mergeResourcesTaskName = taskPrefix + productFlavorFirstUp + buildTypeFirstUp + 'Resources'
        def packageResourcesTask = project.tasks.findByName(mergeResourcesTaskName)
        if (packageResourcesTask != null) {
            codeChecker = new CodeChecker(project, microModuleInfo, productFlavorInfo, buildType, flavorName)
            packageResourcesTask.doLast {
                codeChecker.checkResources(mergeResourcesTaskName, sourceFolders)
            }
        }

        def compileJavaTaskName = "compile${productFlavorFirstUp}${buildTypeFirstUp}JavaWithJavac"
        def compileJavaTask = project.tasks.findByName(compileJavaTaskName)
        if (compileJavaTask != null) {
            compileJavaTask.doLast {
                if (codeChecker == null) {
                    codeChecker = new CodeChecker(project, microModuleInfo, productFlavorInfo, buildType, flavorName)
                }
                codeChecker.checkClasses(mergeResourcesTaskName, sourceFolders)
            }
        }
    }

}