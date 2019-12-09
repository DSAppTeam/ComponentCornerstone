package com.plugin.component.check

import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.NodeList

import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.Transformer
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

class CheckManifest {

    Document document
    Element rootElement

    String packageName
    Map<String, MicroModuleFile> lastModifiedResourcesMap
    Map<String, MicroModuleFile> lastModifiedClassesMap

    void load(File sourceFile) {
        if (!sourceFile.exists()) return
        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance()
        document = builderFactory.newDocumentBuilder().parse(sourceFile)
        rootElement = document.documentElement
        packageName = rootElement.getAttribute("package")
    }

    void setResourcesLastModified(long lastModified) {
        resourcesLastModified = lastModified
    }

    Map<String, MicroModuleFile> getResourcesMap() {
        if (lastModifiedResourcesMap != null) return lastModifiedResourcesMap

        lastModifiedResourcesMap = new HashMap<>()
        if (rootElement == null) return lastModifiedResourcesMap

        NodeList resourcesNodeList = rootElement.getElementsByTagName("resources")
        if (resourcesNodeList.length == 0) {
            return lastModifiedResourcesMap
        }
        Element resourcesElement = (Element) resourcesNodeList.item(0)
        NodeList fileNodeList = resourcesElement.getElementsByTagName("file")
        for (int i = 0; i < fileNodeList.getLength(); i++) {
            Element fileElement = (Element) fileNodeList.item(i)
            MicroModuleFile microModuleFile = new MicroModuleFile()
            microModuleFile.name = fileElement.getAttribute("name")
            microModuleFile.microModuleName = fileElement.getAttribute("microModuleName")
            microModuleFile.path = fileElement.getAttribute("path")
            microModuleFile.lastModified = fileElement.getAttribute("lastModified").toLong()
            lastModifiedResourcesMap.put(microModuleFile.path, microModuleFile)
        }
        return lastModifiedResourcesMap
    }

    Map<String, MicroModuleFile> getClassesMap() {
        if (lastModifiedClassesMap != null) return lastModifiedClassesMap

        lastModifiedClassesMap = new HashMap<>()
        if (rootElement == null) return lastModifiedClassesMap

        NodeList classesNodeList = rootElement.getElementsByTagName("classes")
        if (classesNodeList.length == 0) {
            return lastModifiedClassesMap
        }
        Element classesElement = (Element) classesNodeList.item(0)
        NodeList fileNodeList = classesElement.getElementsByTagName("file")
        for (int i = 0; i < fileNodeList.getLength(); i++) {
            Element fileElement = (Element) fileNodeList.item(i)
            MicroModuleFile microModuleFile = new MicroModuleFile()
            microModuleFile.name = fileElement.getAttribute("name")
            microModuleFile.microModuleName = fileElement.getAttribute("microModuleName")
            microModuleFile.path = fileElement.getAttribute("path")
            microModuleFile.lastModified = fileElement.getAttribute("lastModified").toLong()
            lastModifiedClassesMap.put(microModuleFile.path, microModuleFile)
        }
        return lastModifiedClassesMap
    }

    void save(File destFile) {
        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance()
        Document documentTemp = builderFactory.newDocumentBuilder().newDocument()
        Element microModuleXmlTemp = documentTemp.createElement("micro-module")
        microModuleXmlTemp.setAttribute("package", packageName)
        // resources
        Element resourcesElement = documentTemp.createElement("resources")
        microModuleXmlTemp.appendChild(resourcesElement)
        if (lastModifiedResourcesMap != null) {
            lastModifiedResourcesMap.each {
                MicroModuleFile resourceFile = it.value
                Element fileElement = documentTemp.createElement("file")
                fileElement.setAttribute("name", resourceFile.name)
                fileElement.setAttribute("path", resourceFile.path)
                fileElement.setAttribute("lastModified", resourceFile.lastModified.toString())
                fileElement.setAttribute("microModuleName", resourceFile.microModuleName)
                resourcesElement.appendChild(fileElement)
            }
        }

        // classes
        if (lastModifiedClassesMap != null) {
            Element classesElement = documentTemp.createElement("classes")
            microModuleXmlTemp.appendChild(classesElement)
            lastModifiedClassesMap.each {
                MicroModuleFile resourceFile = it.value
                Element fileElement = documentTemp.createElement("file")
                fileElement.setAttribute("name", resourceFile.name)
                fileElement.setAttribute("path", resourceFile.path)
                fileElement.setAttribute("lastModified", resourceFile.lastModified.toString())
                fileElement.setAttribute("microModuleName", resourceFile.microModuleName)
                classesElement.appendChild(fileElement)
            }
            microModuleXmlTemp.appendChild(classesElement)
        } else if (rootElement != null) {
            NodeList classesNodeList = rootElement.getElementsByTagName("classes")
            if (classesNodeList.length == 1) {
                Element classesElement = (Element) classesNodeList.item(0)
                microModuleXmlTemp.appendChild(documentTemp.importNode(classesElement, true))
            }
        }
        // save
        Transformer transformer = TransformerFactory.newInstance().newTransformer()
        transformer.setOutputProperty(OutputKeys.INDENT, "yes")
        transformer.setOutputProperty(OutputKeys.CDATA_SECTION_ELEMENTS, "yes")
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2")
        transformer.transform(new DOMSource(microModuleXmlTemp), new StreamResult(destFile))
    }

}