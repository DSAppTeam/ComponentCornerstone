package com.plugin.component.extension

import com.plugin.component.Logger
import com.plugin.component.Runtimes
import com.plugin.component.extension.module.Digraph
import com.plugin.component.extension.module.SourceFile
import com.plugin.component.extension.module.SourceSet
import com.plugin.component.extension.option.PublicationOption
import com.plugin.component.utils.PublicationUtil
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.NodeList
import com.plugin.component.Constants

import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.Transformer
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

/**
 * 发布sdk管理
 * created by yummylau 2019/08/09
 */
class PublicationManager {

    private static PublicationManager sPublicationManager

    private Map<String, PublicationOption> sdkPublicationManifest
    private Map<String, PublicationOption> implPublicationManifest

    Digraph<String> dependencyGraph
    Map<String, PublicationOption> publicationDependencies

    static getInstance() {
        if (sPublicationManager == null) {
            sPublicationManager = new PublicationManager()
        }
        return sPublicationManager
    }

    /**
     * 目前只处理sdk
     * @param rootProject
     */
    void loadManifest(Project rootProject) {

        sdkPublicationManifest = new HashMap<>()
        dependencyGraph = new Digraph<String>()
        publicationDependencies = new HashMap<>()

        rootProject.gradle.buildFinished {
            Runtimes.resetProjectInfoScript()
            if (it.failure != null) {
                Logger.buildOutput("build fail!")
                return
            }
            saveManifest()
            Logger.buildOutput("build finished!")
        }

        File sdkPublicationManifest = new File(Runtimes.sSdkDir, 'publicationManifest.xml')
        File implPublicationManifest = new File(Runtimes.sImplDir, 'publicationManifest.xml')

        if (!sdkPublicationManifest.exists()) {
            return
        }

        if (!implPublicationManifest.exists()) {
            return
        }

        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance()
        Document document = builderFactory.newDocumentBuilder().parse(sdkPublicationManifest)

        //搜寻所有publication节点
        NodeList publicationNodeList = document.documentElement.getElementsByTagName("publication")
        for (int i = 0; i < publicationNodeList.getLength(); i++) {

            Element publicationElement = (Element) publicationNodeList.item(i)

            PublicationOption publication = new PublicationOption()
            publication.project = publicationElement.getAttribute("project")
            publication.groupId = publicationElement.getAttribute("groupId")
            publication.artifactId = publicationElement.getAttribute("artifactId")
            publication.version = publicationElement.getAttribute("version")
            if (publication.version == "") publication.version = null
            publication.invalid = Boolean.valueOf(publicationElement.getAttribute("invalid"))

            //如果有效
            if (!publication.invalid) {
                NodeList sourceSetNodeList = publicationElement.getElementsByTagName("sourceSet")
                Element sourceSetElement = (Element) sourceSetNodeList.item(0)
                SourceSet sourceSet = new SourceSet()
                sourceSet.path = sourceSetElement.getAttribute("path")
                sourceSet.lastModifiedSourceFile = new HashMap<>()
                NodeList fileNodeList = sourceSetElement.getElementsByTagName("file")
                for (int k = 0; k < fileNodeList.getLength(); k++) {
                    Element fileElement = (Element) fileNodeList.item(k)
                    SourceFile sourceFile = new SourceFile()
                    sourceFile.path = fileElement.getAttribute("path")
                    sourceFile.lastModified = fileElement.getAttribute("lastModified").toLong()
                    sourceSet.lastModifiedSourceFile.put(sourceFile.path, sourceFile)
                }
                publication.misSourceSet = sourceSet
            }

            this.sdkPublicationManifest.put(PublicationUtil.getPublicationId(publication), publication)
        }

    }

    /**
     * 保存 sdkPublicationManifest.xml
     */
    private void saveManifest() {
        if (!Runtimes.sSdkDir.exists()) {
            Runtimes.sSdkDir.mkdirs()
        }
        File sdkPublicationManifestFile = new File(Runtimes.sSdkDir, 'publicationManifest.xml')
        File implPublicationManifest = new File(Runtimes.sSdkDir, 'publicationManifest.xml')

        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance()
        Document document = builderFactory.newDocumentBuilder().newDocument()
        Element manifestElement = document.createElement("manifest")
        this.sdkPublicationManifest.each {
            PublicationOption publication = it.value

            if (!publication.hit || publication.invalid) return

            Element publicationElement = document.createElement('publication')
            publicationElement.setAttribute('project', publication.project)
            publicationElement.setAttribute('groupId', publication.groupId)
            publicationElement.setAttribute('artifactId', publication.artifactId)
            publicationElement.setAttribute('version', publication.version)
            publicationElement.setAttribute('invalid', publication.invalid ? "true" : "false")

            if (!publication.invalid) {
                Element sourceSetElement = document.createElement('sourceSet')
                sourceSetElement.setAttribute('path', publication.misSourceSet.path)
                publication.misSourceSet.lastModifiedSourceFile.each {
                    SourceFile sourceFile = it.value
                    Element sourceFileElement = document.createElement('file')
                    sourceFileElement.setAttribute('path', sourceFile.path)
                    sourceFileElement.setAttribute('lastModified', sourceFile.lastModified.toString())
                    sourceSetElement.appendChild(sourceFileElement)
                }
                publicationElement.appendChild(sourceSetElement)
            }
            manifestElement.appendChild(publicationElement)
        }

        Transformer transformer = TransformerFactory.newInstance().newTransformer()
        transformer.setOutputProperty(OutputKeys.INDENT, "yes")
        transformer.setOutputProperty(OutputKeys.CDATA_SECTION_ELEMENTS, "yes")
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2")
        transformer.transform(new DOMSource(manifestElement), new StreamResult(sdkPublicationManifestFile))
    }

    void addDependencyGraph(String projectName, PublicationOption publication) {
        def key = PublicationUtil.getPublicationId(publication)
        publicationDependencies.put(key, publication)
        dependencyGraph.add(key)
        if (publication.dependencies != null) {
            if (publication.dependencies.implementation != null) {
                publication.dependencies.implementation.each {
                    if (it instanceof String && it.startsWith(Constants.COMPONENT_PRE)) {
                        String sdkDependency = PublicationUtil.getPublication(publication)
                        dependencyGraph.add(key, sdkDependency)
                        if (!dependencyGraph.isDag()) {
                            throw new RuntimeException("Circular dependency in project [${projectName}] with '${sdkDependency}'.")
                        }
                    }
                }
            }

            if (publication.dependencies.compileOnly != null) {
                publication.dependencies.compileOnly.each {
                    if (it instanceof String && it.startsWith(Constants.COMPONENT_PRE)) {
                        String sdkDependency = PublicationUtil.getPublication(publication)
                        dependencyGraph.add(key, sdkDependency)
                        if (!dependencyGraph.isDag()) {
                            throw new RuntimeException("Circular dependency in project [${projectName}] with '${sdkDependency}'.")
                        }
                    }
                }
            }
        }
    }

    /**
     * 判断是否修改
     * @param publication
     * @return
     */
    boolean hasModified(PublicationOption publication) {
        PublicationOption lastPublication = sdkPublicationManifest.get(PublicationUtil.getPublicationId(publication))
        if (lastPublication == null) {
            return true
        }
        if (publication.invalid != lastPublication.invalid) {
            return true
        }
        return hasModifiedSourceSet(publication.misSourceSet, lastPublication.misSourceSet)
    }

    private boolean hasModifiedSourceSet(SourceSet sourceSet1, SourceSet sourceSet2) {
        return hasModifiedSourceFile(sourceSet1.lastModifiedSourceFile, sourceSet2.lastModifiedSourceFile)
    }

    private boolean hasModifiedSourceFile(Map<String, SourceFile> map1, Map<String, SourceFile> map2) {
        if (map1.size() != map2.size()) {
            return true
        }
        for (Map.Entry<String, SourceFile> entry1 : map1.entrySet()) {
            SourceFile sourceFile1 = entry1.getValue()
            SourceFile sourceFile2 = map2.get(entry1.getKey())
            if (sourceFile2 == null) {
                return true
            }
            if (sourceFile1.lastModified != sourceFile2.lastModified) {
                return true
            }
        }
        return false
    }

    void addPublication(PublicationOption publication) {
        sdkPublicationManifest.put(PublicationUtil.getPublicationId(publication), publication)
    }

    PublicationOption getPublication(String groupId, String artifactId) {
        return sdkPublicationManifest.get(PublicationUtil.getPublicationId(groupId, artifactId))
    }

    PublicationOption getPublicationByKey(String key) {
        return sdkPublicationManifest.get(key)
    }

    List<PublicationOption> getPublicationByProject(Project project) {
        String displayName = project.getDisplayName()
        String projectName = displayName.substring(displayName.indexOf("'") + 1, displayName.lastIndexOf("'"))

        List<PublicationOption> publications = new ArrayList<>()
        sdkPublicationManifest.each {
            if (projectName == it.value.project) {
                publications.add(it.value)
            }
        }
        return publications
    }

    void hitPublication(PublicationOption publication) {
        PublicationOption existsPublication = sdkPublicationManifest.get(PublicationUtil.getPublicationId(publication))
        if (existsPublication == null) return

        if (existsPublication.hit) {
            validPublication(publication, existsPublication)
        } else {
            existsPublication.hit = true
        }
    }

    private void validPublication(PublicationOption publication, PublicationOption existsPublication) {
        if (publication.project != existsPublication.project) {
            throw new GradleException("Already exists publication " + existsPublication.groupId + ":" + existsPublication.artifactId + " in project '${existsPublication.project}'.")
        }
    }

}