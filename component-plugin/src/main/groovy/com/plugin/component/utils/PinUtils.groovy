package com.plugin.component.utils

import com.plugin.component.extension.module.MicroModule
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

    static String getAndroidManifestPackageName(File androidManifest) {
        def builderFactory = DocumentBuilderFactory.newInstance()
        builderFactory.setNamespaceAware(true)
        Element manifestXml = builderFactory.newDocumentBuilder().parse(androidManifest).documentElement
        return manifestXml.getAttribute("package")
    }

    static MicroModule buildMicroModule(Project project, String microModulePath) {
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
        MicroModule microModule = new MicroModule()
        microModule.name = microModuleName
        microModule.microModuleDir = microModuleDir
        return microModule
    }

    private static String removeTrailingColon(String microModulePath) {
        return microModulePath.startsWith(":") ? microModulePath.substring(1) : microModulePath
    }


}