package com.plugin.component.utils

import com.android.build.gradle.BaseExtension
import com.android.manifmerger.ManifestMerger2
import com.android.manifmerger.MergingReport
import com.android.manifmerger.XmlDocument
import com.android.utils.ILogger
import com.plugin.component.check.CodeChecker
import com.plugin.component.extension.module.PinInfo
import com.plugin.component.extension.module.ProductFlavorInfo
import com.plugin.component.extension.option.pin.PinConfiguration
import com.plugin.component.plugin.PinPlugin
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.w3c.dom.Element

import javax.xml.parsers.DocumentBuilderFactory

class PinUtils {

    static String upperCase(String str) {
        char[] ch = str.toCharArray()
        if (ch[0] >= 'a' && ch[0] <= 'z') {
            ch[0] -= 32
        }
        return String.valueOf(ch)
    }

    static void generateAndroidManifest(Project project, PinConfiguration pinConfiguration, String startTaskState) {
        if ((startTaskState == PinPlugin.ASSEMBLE_OR_GENERATE || !pinConfiguration.exportPins.isEmpty()) && isMainSourceSetEmpty(project)) {
            setMainSourceSetManifest()
            return
        }
        mergeAndroidManifest(project, 'main', pinConfiguration, startTaskState)

        pinConfiguration.productFlavorInfo.buildTypes.each {
            mergeAndroidManifest(project, it, pinConfiguration, startTaskState)
        }

        if (!pinConfiguration.productFlavorInfo.singleDimension) {
            pinConfiguration.productFlavorInfo.productFlavors.each {
                mergeAndroidManifest(project, it, pinConfiguration, startTaskState)
            }
        }

        pinConfiguration.productFlavorInfo.combinedProductFlavors.each {
            mergeAndroidManifest(project, it, pinConfiguration, startTaskState)

            def productFlavor = it
            pinConfiguration.productFlavorInfo.buildTypes.each {
                mergeAndroidManifest(project, productFlavor + upperCase(it), pinConfiguration, startTaskState)
            }
        }

        def androidTest = 'androidTest'
        mergeAndroidManifest(project, androidTest, pinConfiguration, startTaskState)
        mergeAndroidManifest(project, androidTest + 'Debug', pinConfiguration, startTaskState)
        if (!pinConfiguration.productFlavorInfo.singleDimension) {
            pinConfiguration.productFlavorInfo.productFlavors.each {
                mergeAndroidManifest(project, androidTest + upperCase(it), pinConfiguration, startTaskState)
            }
        }
        pinConfiguration.productFlavorInfo.combinedProductFlavors.each {
            mergeAndroidManifest(project,androidTest + Utils.upperCase(it), pinConfiguration, startTaskState)
            mergeAndroidManifest(project,androidTest + Utils.upperCase(it) + 'Debug', pinConfiguration, startTaskState)
        }
    }

    static void mergeAndroidManifest(Project project, String variantName, PinConfiguration pinConfiguration, String startTaskState) {
        File mainManifestFile = new File(pinConfiguration.mainPin.pinDir, "/src/${variantName}/AndroidManifest.xml")
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

        pinConfiguration.includePins.each {
            PinInfo pin = it.value
            if (startTaskState == PinPlugin.ASSEMBLE_OR_GENERATE && !pin.appliedScript) return
            if (pin.name == pinConfiguration.mainPin.name) return
            def pinManifestFile = new File(pin.pinDir, "/src/${variantName}/AndroidManifest.xml")
            if (pinManifestFile.exists()) {
                invoker.addLibraryManifest(pinManifestFile)
            }
        }

        def mergingReport = invoker.merge()
        if (!mergingReport.result.success) {
            mergingReport.log(logger)
            throw new GradleException(mergingReport.reportString)
        }
        def moduleAndroidManifest = mergingReport.getMergedDocument(MergingReport.MergedManifestKind.MERGED)
        moduleAndroidManifest = new String(moduleAndroidManifest.getBytes('UTF-8'))

        def saveDir = new File(project.projectDir, "build/pin/merge-manifest/${variantName}")
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
        obj.manifest.srcFile project.projectDir.absolutePath + "/build/pin/merge-manifest/${variantName}/AndroidManifest.xml"
    }

    static void addMicroModuleSourceSet(Project project, PinInfo pin, ProductFlavorInfo productFlavorInfo) {
        addVariantSourceSet(project, pin, 'main')

        productFlavorInfo.buildTypes.each {
            addVariantSourceSet(project, pin, it)
        }

        if (!productFlavorInfo.singleDimension) {
            productFlavorInfo.productFlavors.each {
                addVariantSourceSet(project, pin, it)
            }
        }

        productFlavorInfo.combinedProductFlavors.each {
            addVariantSourceSet(project, pin, it)
            def flavorName = it
            productFlavorInfo.buildTypes.each {
                addVariantSourceSet(project, pin, flavorName + upperCase(it))
            }
        }

        def testTypes = ['androidTest', 'test']
        testTypes.each {
            def testType = it
            addVariantSourceSet(project, pin, testType)

            if (testType == 'test') {
                productFlavorInfo.buildTypes.each {
                    addVariantSourceSet(project, pin, testType + upperCase(it))
                }
            } else {
                addVariantSourceSet(project, pin, testType + 'Debug')
            }

            if (!productFlavorInfo.singleDimension) {
                productFlavorInfo.productFlavors.each {
                    addVariantSourceSet(project, pin, testType + upperCase(it))
                }
            }

            productFlavorInfo.combinedProductFlavors.each {
                def productFlavorName = testType + upperCase(it)
                addVariantSourceSet(project, pin, productFlavorName)

                if (testType == 'test') {
                    productFlavorInfo.buildTypes.each {
                        addVariantSourceSet(project, pin, productFlavorName + upperCase(it))
                    }
                } else {
                    addVariantSourceSet(project, pin, productFlavorName + 'Debug')
                }
            }
        }
    }

    static void clearOriginSourceSet(Project project, ProductFlavorInfo productFlavorInfo) {
        clearModuleSourceSet(project, 'main')

        // buildTypes
        productFlavorInfo.buildTypes.each {
            clearModuleSourceSet(project, it)
        }

        if (!productFlavorInfo.singleDimension) {
            productFlavorInfo.productFlavors.each {
                clearModuleSourceSet(project, it)
            }
        }

        productFlavorInfo.combinedProductFlavors.each {
            clearModuleSourceSet(project, it)
            def flavorName = it
            productFlavorInfo.buildTypes.each {
                clearModuleSourceSet(project, flavorName + upperCase(it))
            }
        }

        def testTypes = ['androidTest', 'test']
        testTypes.each {
            def testType = it
            clearModuleSourceSet(project, testType)

            if (testType == 'test') {
                productFlavorInfo.buildTypes.each {
                    clearModuleSourceSet(project, testType + upperCase(it))
                }
            } else {
                clearModuleSourceSet(project, testType + 'Debug')
            }

            if (!productFlavorInfo.singleDimension) {
                productFlavorInfo.productFlavors.each {
                    clearModuleSourceSet(project, testType + upperCase(it))
                }
            }

            productFlavorInfo.combinedProductFlavors.each {
                def productFlavorName = testType + upperCase(it)
                clearModuleSourceSet(project, productFlavorName)

                if (testType == 'test') {
                    productFlavorInfo.buildTypes.each {
                        clearModuleSourceSet(project, productFlavorName + upperCase(it))
                    }
                } else {
                    clearModuleSourceSet(project, productFlavorName + 'Debug')
                }
            }
        }
    }

    static boolean isMainSourceSetEmpty(Project project) {
        BaseExtension android = project.extensions.getByName('android')
        def obj = android.sourceSets.findByName('main')
        if (obj == null) {
            return true
        }
        return obj.java.srcDirs.size() == 0
    }

    static setMainSourceSetManifest(Project project, String dir) {
        BaseExtension android = project.extensions.getByName('android')
        def obj = android.sourceSets.findByName('main')
        if (obj == null) {
            obj = android.sourceSets.create('main')
        }
        File mainManifestFile = new File(dir, '/src/main/AndroidManifest.xml')
        obj.manifest.srcFile mainManifestFile
    }

    static clearModuleSourceSet(Project project, def type) {
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

    static addVariantSourceSet(Project project, PinInfo microModule, def type) {
        def absolutePath = microModule.pinDir.absolutePath
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


    static void checkMicroModuleBoundary(Project project, PinConfiguration pinConfiguration, String taskPrefix, String buildType, String flavorName, List<String> sourceFolders) {
        CodeChecker codeChecker

        def buildTypeFirstUp = upperCase(buildType)
        def productFlavorFirstUp = flavorName != null ? upperCase(flavorName) : ""

        def mergeResourcesTaskName = taskPrefix + productFlavorFirstUp + buildTypeFirstUp + 'Resources'
        def packageResourcesTask = project.tasks.findByName(mergeResourcesTaskName)
        if (packageResourcesTask != null) {
            codeChecker = new CodeChecker(project, pinConfiguration, pinConfiguration.productFlavorInfo, buildType, flavorName)
            packageResourcesTask.doLast {
                codeChecker.checkResources(mergeResourcesTaskName, sourceFolders)
            }
        }

        def compileJavaTaskName = "compile${productFlavorFirstUp}${buildTypeFirstUp}JavaWithJavac"
        def compileJavaTask = project.tasks.findByName(compileJavaTaskName)
        if (compileJavaTask != null) {
            compileJavaTask.doLast {
                if (codeChecker == null) {
                    codeChecker = new CodeChecker(project, pinConfiguration, pinConfiguration.productFlavorInfo, buildType, flavorName)
                }
                codeChecker.checkClasses(mergeResourcesTaskName, sourceFolders)
            }
        }
    }

    static String getAndroidManifestPackageName(File androidManifest) {
        def builderFactory = DocumentBuilderFactory.newInstance()
        builderFactory.setNamespaceAware(true)
        Element manifestXml = builderFactory.newDocumentBuilder().parse(androidManifest).documentElement
        return manifestXml.getAttribute("package")
    }

    /**
     * 构建pin工程
     * @param project
     * @param microModulePath
     * @return
     */
    static PinInfo buildPin(Project project, String microModulePath) {
        String[] pathElements = removeTrailingColon(microModulePath).split(":")
        int pathElementsLen = pathElements.size()
        File parentMicroModuleDir = project.projectDir
        for (int j = 0; j < pathElementsLen; j++) {
            parentMicroModuleDir = new File(parentMicroModuleDir, pathElements[j])
        }
        File microModuleDir = parentMicroModuleDir.canonicalFile
        String microModuleName = microModuleDir.absolutePath.replace(project.projectDir.absolutePath, "")
        if (File.separator == "\\") {
            microModuleName = microModuleName.replaceAll("\\\\", ":")
        } else {
            microModuleName = microModuleName.replaceAll("/", ":")
        }
        if (!microModuleDir.exists()) {
            return null
        }
        PinInfo microModule = new PinInfo()
        microModule.name = microModuleName
        microModule.pinDir = microModuleDir
        return microModule
    }

    /**
     * 删除冒号
     * @param microModulePath
     * @return
     */
    private static String removeTrailingColon(String microModulePath) {
        return microModulePath.startsWith(":") ? microModulePath.substring(1) : microModulePath
    }


}