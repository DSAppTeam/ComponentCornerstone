package com.plugin.component.extension

import com.plugin.component.Constants
import com.plugin.component.extension.module.ProjectInfo
import com.plugin.component.extension.option.sdk.SdkOption
import com.plugin.component.log.Logger
import com.plugin.component.Runtimes
import com.plugin.component.extension.module.Digraph
import com.plugin.component.extension.module.SourceFile
import com.plugin.component.extension.module.SourceSet
import com.plugin.component.extension.option.sdk.PublicationOption
import com.plugin.component.log.MutLineLog
import com.plugin.component.utils.GitUtil
import com.plugin.component.utils.HttpUrlConnectHelper
import com.plugin.component.utils.ProjectUtil
import com.plugin.component.utils.PublicationUtil
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.NodeList

import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.Transformer
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import java.nio.charset.Charset
import java.text.SimpleDateFormat

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
        implPublicationManifest = new HashMap<>()
        dependencyGraph = new Digraph<String>()
        publicationDependencies = new HashMap<>()

        boolean isAssemble = false
        boolean isPublish = false
        List<String> taskNames = ProjectUtil.getTasks(rootProject)
        if (!taskNames.isEmpty()) {
            for (String task : taskNames) {
                if (task.toUpperCase().contains("ASSEMBLE")
                        || task.contains("aR")
                        || task.contains("asR")
                        || task.contains("asD")
                        || task.toUpperCase().contains("TINKER")
                        || task.toUpperCase().contains("INSTALL")
                        || task.toUpperCase().contains("RESGUARD")) {
                    isAssemble = true
                } else if (task.toUpperCase().contains("COMPONENTPUBLISH")) {
                    isPublish = true
                }
            }
        }

        rootProject.gradle.buildFinished {
            if (it.failure != null) {
                Logger.buildOutput("build fail!")
                return
            }
            saveManifest(isAssemble)
            logPublishInfo(rootProject, isPublish || isAssemble)
            Runtimes.clean()
            Logger.buildOutput("build finished!")
        }
        loadSdkManifest()
        if (isAssemble) {
            loadImpManifest()
        }
    }


    private static void logPublishInfo(Project rootProject, boolean needLogTask) {
        if (!needLogTask) {
            return
        }
        boolean isPublish = Runtimes.sSdkOption.isPublishMode
        MutLineLog mutLineLog = new MutLineLog()
        MutLineLog mutLineLogFile = new MutLineLog()

        rootProject.allprojects.each { childProject ->
            if (!Runtimes.shouldApplyComponentPlugin(childProject)) return

            PublicationOption sdkPublication = Runtimes.getSdkPublication(childProject.name)
            PublicationOption implPublication = Runtimes.getImplPublication(childProject.name)
            ProjectInfo projectInfo = Runtimes.getProjectInfo(childProject.name)
            if (sdkPublication == null || implPublication == null || projectInfo == null) {
                return
            }
            isPublish = isPublish || projectInfo.isPublish
            mutLineLog.build4("***********************************************************************************************************")
            mutLineLog.build4("* SDK ${sdkPublication.name} publication=${PublicationUtil.getMavenGAV(sdkPublication)} publish=${sdkPublication.sdkNeedPublish}")
            mutLineLog.build4("* commitInfo:")
            mutLineLog.build4("* ${sdkPublication.sdkSourceSet.gitCommitInfo}")
            mutLineLog.build4("* commitTime:${sdkPublication.sdkSourceSet.commitTime} commitUser:${sdkPublication.sdkSourceSet.commitUser}")
            mutLineLog.build4("*                                              ")
            mutLineLog.build4("* Impl ${implPublication.name} publication=${PublicationUtil.getImpMavenGAV(implPublication, !projectInfo.isDebug)} publish=${implPublication.impNeedPublish}")
            mutLineLog.build4("* commitInfo:")
            mutLineLog.build4("* ${implPublication.impSourceSet.gitCommitInfo}")
            mutLineLog.build4("* commitTime:${implPublication.impSourceSet.commitTime} commitUser:${implPublication.impSourceSet.commitUser}")

            if (sdkPublication.sdkNeedPublish || implPublication.impNeedPublish) {
                mutLineLogFile.build("***********************************************************************************************************")
                if (sdkPublication.sdkNeedPublish) {
                    mutLineLogFile.build("* SDK ${sdkPublication.name} publication=${PublicationUtil.getMavenGAV(sdkPublication)} publish=${sdkPublication.sdkNeedPublish}")
                    mutLineLogFile.build("* commitInfo:")
                    mutLineLogFile.build("* ${sdkPublication.sdkSourceSet.gitCommitInfo}")
                    mutLineLogFile.build("* commitTime:${sdkPublication.sdkSourceSet.commitTime} commitUser:${sdkPublication.sdkSourceSet.commitUser}")
                }
                if (implPublication.impNeedPublish) {
                    mutLineLogFile.build("* Impl ${implPublication.name} publication=${PublicationUtil.getImpMavenGAV(implPublication, !projectInfo.isDebug)} publish=${implPublication.impNeedPublish}")
                    mutLineLogFile.build("* commitInfo:")
                    mutLineLogFile.build("* ${implPublication.impSourceSet.gitCommitInfo}")
                    mutLineLogFile.build("* commitTime:${implPublication.impSourceSet.commitTime} commitUser:${implPublication.impSourceSet.commitUser}")
                }
            }
        }
        mutLineLog.build4("***********************************************************************************************************")
        if (!mutLineLogFile.done().isEmpty()) {
            mutLineLogFile.build("***********************************************************************************************************")
        }
        if (isPublish) {
            Logger.buildBlockLog("Publish Info", mutLineLog)
            if (mutLineLogFile.done().isEmpty()) {
                return
            }
            /*
             * http://10.0.9.238:8088/probe/plugin-publish-log?log=${long}
             * 每次日志通过log传递
             * http://10.0.9.238:8080/godlike/plugin-logs/publish.log
             * 浏览器打开查看
             */
            def format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
            StringBuilder builder = new StringBuilder()
            String publishTile = "Publish Components in ${format.format(new Date())}"
            builder.append(" ↓↓↓↓↓↓↓↓↓↓   $publishTile   ↓↓↓↓↓↓↓↓↓↓\n")
            builder.append(mutLineLogFile.done() + "\n")
            builder.append(" ↑↑↑↑↑↑↑↑↑↑   $publishTile   ↑↑↑↑↑↑↑↑↑↑\n")
            String base = new String(Base64.urlEncoder.encode(builder.toString().getBytes("utf-8")), "utf-8")
            HttpUrlConnectHelper.sendRequest("http://10.0.9.238:8088/probe/plugin-publish-log?log=${base}", "GET")
            Logger.buildOutput("See http://10.0.9.238:8088/probe/log-query for more detail logs！")
        }
    }

    private void loadSdkManifest() {
        File sdkPublicationManifestFile = new File(Runtimes.sSdkDir, 'publicationManifest.xml')

        if (!sdkPublicationManifestFile.exists()) {
            return
        }

        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance()
        Document document = builderFactory.newDocumentBuilder().parse(sdkPublicationManifestFile)

        //搜寻所有publication节点
        NodeList publicationNodeList = document.documentElement.getElementsByTagName("publication")
        for (int i = 0; i < publicationNodeList.getLength(); i++) {

            Element publicationElement = (Element) publicationNodeList.item(i)

            PublicationOption publication = new PublicationOption()
            publication.project = publicationElement.getAttribute("project")
            publication.groupId = publicationElement.getAttribute("groupId")
            publication.artifactId = publicationElement.getAttribute("artifactId")
            publication.sdkVersion = publicationElement.getAttribute("version")
            if (publication.sdkVersion == "") publication.sdkVersion = null
            publication.invalid = Boolean.valueOf(publicationElement.getAttribute("invalid"))

            //如果有效
            if (!publication.invalid) {
                NodeList sourceSetNodeList = publicationElement.getElementsByTagName("sourceSet")
                Element sourceSetElement = (Element) sourceSetNodeList.item(0)
                SourceSet sourceSet = new SourceSet()
                sourceSet.path = sourceSetElement.getAttribute("path")
                sourceSet.lastModifiedSourceFile = new HashMap<>()
                sourceSet.gitVersion = sourceSetElement.getAttribute("gitVersion")
                try {
                    sourceSet.quickVerifyModified = sourceSetElement.getAttribute("quickVerifyModified").toLong()
                } catch (Exception ignored) {
                    sourceSet.quickVerifyModified = 0L
                }
                NodeList fileNodeList = sourceSetElement.getElementsByTagName("file")
                for (int k = 0; k < fileNodeList.getLength(); k++) {
                    Element fileElement = (Element) fileNodeList.item(k)
                    SourceFile sourceFile = new SourceFile()
                    sourceFile.path = fileElement.getAttribute("path")
                    sourceFile.lastModified = fileElement.getAttribute("lastModified").toLong()
                    sourceSet.lastModifiedSourceFile.put(sourceFile.path, sourceFile)
                }
                publication.sdkSourceSet = sourceSet
            }

            this.sdkPublicationManifest.put(PublicationUtil.getPublicationId(publication), publication)
        }
    }

    private void loadImpManifest() {
        File implPublicationManifestFile = new File(Runtimes.sImplDir, 'publicationManifest.xml')
        if (!implPublicationManifestFile.exists()) {
            return
        }

        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance()
        Document document = builderFactory.newDocumentBuilder().parse(implPublicationManifestFile)

        //搜寻所有publication节点
        NodeList publicationNodeList = document.documentElement.getElementsByTagName("publication")
        for (int i = 0; i < publicationNodeList.getLength(); i++) {

            Element publicationElement = (Element) publicationNodeList.item(i)

            PublicationOption publication = new PublicationOption()
            publication.project = publicationElement.getAttribute("project")
            publication.groupId = publicationElement.getAttribute("groupId")
            publication.artifactId = publicationElement.getAttribute("artifactId")
            publication.implVersion = publicationElement.getAttribute("version")
            if (publication.implVersion == "") publication.implVersion = null
            publication.invalid = Boolean.valueOf(publicationElement.getAttribute("invalid"))

            //如果有效
            if (!publication.invalid) {
                NodeList sourceSetNodeList = publicationElement.getElementsByTagName("sourceSet")
                Element sourceSetElement = (Element) sourceSetNodeList.item(0)
                SourceSet sourceSet = new SourceSet()
                sourceSet.path = sourceSetElement.getAttribute("path")
                sourceSet.lastModifiedSourceFile = new HashMap<>()
                sourceSet.gitVersion = sourceSetElement.getAttribute("gitVersion")
                try {
                    sourceSet.quickVerifyModified = sourceSetElement.getAttribute("quickVerifyModified").toLong()
                } catch (Exception ignored) {
                    sourceSet.quickVerifyModified = 0L
                }
                NodeList fileNodeList = sourceSetElement.getElementsByTagName("file")
                for (int k = 0; k < fileNodeList.getLength(); k++) {
                    Element fileElement = (Element) fileNodeList.item(k)
                    SourceFile sourceFile = new SourceFile()
                    sourceFile.path = fileElement.getAttribute("path")
                    sourceFile.lastModified = fileElement.getAttribute("lastModified").toLong()
                    sourceSet.lastModifiedSourceFile.put(sourceFile.path, sourceFile)
                }
                publication.impSourceSet = sourceSet
            }

            this.implPublicationManifest.put(PublicationUtil.getPublicationId(publication), publication)
        }

    }


    private void saveManifest(boolean isAssemble) {
        if (!Runtimes.sSdkDir.exists()) {
            Runtimes.sSdkDir.mkdirs()
        }
        saveSdkManifest()

        if (isAssemble) {
            if (!Runtimes.sImplDir.exists()) {
                Runtimes.sImplDir.mkdirs()
            }
            saveImpManifest()
        }

    }

    /**
     * 保存 impPublicationManifest.xml
     */
    private void saveImpManifest() {
        File implPublicationManifest = new File(Runtimes.sImplDir, 'publicationManifest.xml')

        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance()
        Document document = builderFactory.newDocumentBuilder().newDocument()
        Element manifestElement = document.createElement("manifest")
        this.implPublicationManifest.each {
            PublicationOption publication = it.value

            if (!publication.hit || publication.invalid) return

            Element publicationElement = document.createElement('publication')
            publicationElement.setAttribute('project', publication.project)
            publicationElement.setAttribute('groupId', publication.groupId)
            publicationElement.setAttribute('artifactId', publication.artifactId)
            publicationElement.setAttribute('version', publication.implVersion)
            publicationElement.setAttribute('invalid', publication.invalid ? "true" : "false")

            if (!publication.invalid) {
                Element sourceSetElement = document.createElement('sourceSet')
                sourceSetElement.setAttribute('path', publication.impSourceSet.path)
                sourceSetElement.setAttribute('quickVerifyModified', publication.impSourceSet.quickVerifyModified.toString())
                sourceSetElement.setAttribute("gitVersion", publication.impSourceSet.gitVersion)
//                publication.impSourceSet.lastModifiedSourceFile.each {
//                    SourceFile sourceFile = it.value
//                    Element sourceFileElement = document.createElement('file')
//                    sourceFileElement.setAttribute('path', sourceFile.path)
//                    sourceFileElement.setAttribute('lastModified', sourceFile.lastModified.toString())
//                    sourceSetElement.appendChild(sourceFileElement)
//                }
                publicationElement.appendChild(sourceSetElement)
            }
            manifestElement.appendChild(publicationElement)
        }

        Transformer transformer = TransformerFactory.newInstance().newTransformer()
        transformer.setOutputProperty(OutputKeys.INDENT, "yes")
        transformer.setOutputProperty(OutputKeys.CDATA_SECTION_ELEMENTS, "yes")
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2")
        transformer.transform(new DOMSource(manifestElement), new StreamResult(implPublicationManifest))
    }

    /**
     * 保存 sdkPublicationManifest.xml
     */
    private void saveSdkManifest() {
        File sdkPublicationManifestFile = new File(Runtimes.sSdkDir, 'publicationManifest.xml')

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
            publicationElement.setAttribute('version', publication.sdkVersion)
            publicationElement.setAttribute('invalid', publication.invalid ? "true" : "false")

            if (!publication.invalid) {
                Element sourceSetElement = document.createElement('sourceSet')
                sourceSetElement.setAttribute('path', publication.sdkSourceSet.path)
                sourceSetElement.setAttribute('quickVerifyModified', publication.sdkSourceSet.quickVerifyModified.toString())
                sourceSetElement.setAttribute("gitVersion", publication.sdkSourceSet.gitVersion)
//                publication.sdkSourceSet.lastModifiedSourceFile.each {
//                    SourceFile sourceFile = it.value
//                    Element sourceFileElement = document.createElement('file')
//                    sourceFileElement.setAttribute('path', sourceFile.path)
//                    sourceFileElement.setAttribute('lastModified', sourceFile.lastModified.toString())
//                    sourceSetElement.appendChild(sourceFileElement)
//                }
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
//                    Logger.buildOutput("addDependencyGraph: project:${projectName} dependency: ${it}")
                    if (it instanceof String && it.startsWith(Constants.COMPONENT_PRE)) {
//                        String sdkDependency = PublicationUtil.getPublication(publication)
                        String sdkDependency = PublicationUtil.parseComponentToPublicationId(it)
//                        Logger.buildOutput("add sdkDependency project:${projectName} dependency: ${sdkDependency}")
                        dependencyGraph.add(key, sdkDependency)
                        if (!dependencyGraph.isDag()) {
                            throw new RuntimeException("Circular dependency in project [${projectName}] with '${it}'.")
                        }
                    }
                }
            }

            if (publication.dependencies.compileOnly != null) {
                publication.dependencies.compileOnly.each {
                    if (it instanceof String && it.startsWith(Constants.COMPONENT_PRE)) {
//                        String sdkDependency = PublicationUtil.getPublication(publication)
                        String sdkDependency = PublicationUtil.parseComponentToPublicationId(it)
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
    boolean hasSdkModified(PublicationOption publication) {

        PublicationOption lastPublication = sdkPublicationManifest.get(PublicationUtil.getPublicationId(publication))
//        Logger.buildOutput("lastPublication:${lastPublication}")
//        Logger.buildOutput("currentPublication:${publication}")
        if (lastPublication == null) {
            return true
        }
        if (publication.invalid != lastPublication.invalid) {
            return true
        }
        return hasModifiedSourceSet(publication.sdkSourceSet, lastPublication.sdkSourceSet)
    }

    /**
     * 判断是否修改
     * @param publication
     * @return
     */
    boolean hasImpModified(PublicationOption publication) {
//        File file = new File(Runtimes.sImplDir, PublicationUtil.getAarName(publication))
//        if (!file.exists()) {
//            return true
//        }
        PublicationOption lastPublication = implPublicationManifest.get(PublicationUtil.getPublicationId(publication))
//        Logger.buildOutput("lastPublication:${lastPublication}")
//        Logger.buildOutput("currentPublication:${publication}")
        if (lastPublication == null) {
            return true
        }
        if (publication.invalid != lastPublication.invalid) {
            return true
        }
        return hasModifiedSourceSet(publication.impSourceSet, lastPublication.impSourceSet)
    }

    boolean hasModifyWithGitDiff(SourceSet sourceSet) {
        if (!GitUtil.gitExist){
            return false
        }
        String gitStatus = GitUtil.getGitDiff(sourceSet.path)
        if (gitStatus == null || gitStatus.isEmpty()) {
            return false
        }
        return true
    }

    private boolean hasModifiedSourceSet(SourceSet sourceSet1, SourceSet sourceSet2) {
        return sourceSet1.quickVerifyModified != sourceSet2.quickVerifyModified
//        return hasModifiedSourceFile(sourceSet1.lastModifiedSourceFile, sourceSet2.lastModifiedSourceFile)
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

    void addImpPublication(PublicationOption publication) {
        implPublicationManifest.put(PublicationUtil.getPublicationId(publication), publication)
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

    PublicationOption getSdkPublicationByProject(Project project) {
        String displayName = project.getDisplayName()
        String projectName = displayName.substring(displayName.indexOf("'") + 1, displayName.lastIndexOf("'"))
        PublicationOption publicationOption = null
        sdkPublicationManifest.each {
            if (projectName == it.value.project) {
                publicationOption = it.value
            }
        }
        return publicationOption
    }

    PublicationOption getImplPublicationByProject(Project project) {
        String displayName = project.getDisplayName()
        String projectName = displayName.substring(displayName.indexOf("'") + 1, displayName.lastIndexOf("'"))
        PublicationOption publicationOption = null
        implPublicationManifest.each {
            if (projectName == it.value.project) {
                publicationOption = it.value
            }
        }
        return publicationOption
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