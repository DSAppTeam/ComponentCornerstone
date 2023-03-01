package com.plugin.component.utils

import com.android.build.gradle.AppExtension
import com.plugin.component.ComponentPlugin
import com.plugin.component.Constants
import com.plugin.component.extension.PublicationManager
import com.plugin.component.extension.option.sdk.PublicationDependencyModuleOption
import com.plugin.component.extension.option.sdk.SdkOption
import com.plugin.component.log.Logger
import com.plugin.component.Runtimes
import com.plugin.component.extension.module.ProjectInfo
import com.plugin.component.extension.module.SourceFile
import com.plugin.component.extension.module.SourceSet
import com.plugin.component.extension.option.sdk.PublicationOption
import com.plugin.component.task.CompileSdkTask
import org.apache.http.util.TextUtils
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ExcludeRule
import org.gradle.api.component.SoftwareComponent
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.TaskProvider

import java.text.SimpleDateFormat

class PublicationUtil {

    static getPublicationId(PublicationOption publication) {
        return publication.groupId + '-' + publication.artifactId
    }

    static getPublicationId(String groupId, String artifactId) {
        return groupId + '-' + artifactId
    }

    static getJarName(PublicationOption publication) {
        return publication.groupId + '-' + publication.artifactId + '.jar'
    }

    static getAarName(PublicationOption publication) {
        return publication.groupId + '-' + publication.artifactId + '-imp' + '.aar'
    }

    static getMavenGAV(PublicationOption publication) {
        return publication.groupId + ':' + publication.artifactId + ':' + publication.sdkVersion
    }

    static getImpMavenGAV(PublicationOption publication, boolean isRelease) {
        return publication.groupId + ':' + publication.getArtifactIdString() + "-imp${isRelease ? '' : '-debug'}" + ':' + publication.implVersion + "@aar"
    }

    static getLocalGAV(PublicationOption publication) {
        return ':' + publication.groupId + '-' + publication.artifactId + ':'
    }


    /**
     * 解析 component 依赖
     * @param projectInfo
     * @param value
     * @return
     */
    static parseComponent(ProjectInfo projectInfo, String value) {
        String key = ProjectUtil.getComponentValue(value)
        projectInfo.componentDependencies.add(key)
        PublicationOption publication = Runtimes.getSdkPublication(key)
        //对于依赖的模块，其使用插件但没有配置sdk，则直接依赖该项目
        if (publication == null) {
            return projectInfo.project.project(":" + key)
        } else {
            return getPublication(publication)
        }
    }

    /**
     * 暂时不支持sdk中依赖其他sdk，预留该逻辑
     * @param value
     * @return
     */
    static parseComponent(String value) {
        value = value.replace(Constants.COMPONENT_PRE, "")
        String key = ProjectUtil.getComponentValue(value)
        PublicationOption publication = Runtimes.getSdkPublication(key)
        return getPublication(publication)
    }

    static parseComponentToPublicationId(String value) {
        value = value.replace(Constants.COMPONENT_PRE, "")
        String key = ProjectUtil.getComponentValue(value)
        PublicationOption publication = Runtimes.getSdkPublication(key)
        return getPublicationId(publication)
    }

    static parseComponentDependency(String value) {
        value = value.replace(Constants.COMPONENT_PRE, "")
        String key = ProjectUtil.getComponentValue(value)
        PublicationOption publication = Runtimes.getSdkPublication(key)
        return publication
    }

    /**
     * 获取 publication 依赖
     * @param publication
     * @return
     */
    static getPublication(PublicationOption publication) {
        if (publication != null) {
            if (publication.invalid) {
                return []
            } else if (publication.useLocal) {
                return getLocalGAV(publication)
            } else {
                return getMavenGAV(publication)
            }
        } else {
            return []
        }
    }

    static void addPublicationDependencies(ProjectInfo projectInfo, PublicationOption publication) {
        if (publication.dependencies == null) return
        projectInfo.project.dependencies {
            if (publication.dependencies.compileOnly != null) {
                publication.dependencies.compileOnly.each {
                    def dependency = it
                    if (it instanceof String && it.startsWith(Constants.COMPONENT)) {
                        dependency = parseComponent(it)
                    }
                    Logger.buildOutput("compileOnly " + dependency)
                    if (dependency instanceof String) {
                        compileOnly dependency
                    } else if (dependency instanceof PublicationDependencyModuleOption) {
                        compileOnly(dependency.path) {
                            exclude dependency.exclude
                        }
                    }
                }
            }
            if (publication.dependencies.implementation != null) {
                publication.dependencies.implementation.each {
                    def dependency = it
                    if (it instanceof String && it.startsWith(Constants.COMPONENT)) {
                        dependency = parseComponent(it)
                    }
                    Logger.buildOutput("implementation " + dependency)
                    if (dependency instanceof String) {
                        implementation dependency
                    } else if (dependency instanceof PublicationDependencyModuleOption) {
                        implementation(dependency.path) {
                            exclude dependency.exclude
                        }
                    }
                }
            }
        }
    }

    /**
     * 初始化 pulication
     * @param project
     * @param publication
     */
    static void initPublication(Project project, PublicationOption publication, boolean isAutoVersion) {
        String displayName = project.getDisplayName()
        publication.project = displayName.substring(displayName.indexOf("'") + 1, displayName.lastIndexOf("'"))
        publication.sourceSetName = publication.scrName
        initSdkSourceSet(project, publication, isAutoVersion)
        initImpSourceSet(project, publication, isAutoVersion)

    }

    /**
     * 收集实现类相关的信息
     * @param project
     * @param publication
     */
    private static void initImpSourceSet(Project project, PublicationOption publication, boolean isAutoVersion) {
        def buildSdk = new File(project.projectDir, Constants.BUILD_IMPL_DIR)
        publication.impDir = new File(buildSdk, publication.scrName)

        SourceSet impSourceSet = new SourceSet()
        def impDir = project.projectDir
        impSourceSet.path = impDir.absolutePath
        impSourceSet.lastModifiedSourceFile = new HashMap<>()
        if (isAutoVersion && !publication.useUserImplVersion) {
            String split = "@_@"
            String gitInfo = FileUtil.shell("git log --max-count=1 --pretty=format:\"%cd${split}%h${split}%cn${split}%s\" --date=format:'%Y%m%d%H%M%S' ${impDir.absolutePath}")
            Logger.buildOutput("filename:$impDir.absolutePath gitInfo：$gitInfo")
            String[] infos = gitInfo.split(split)
            if (infos.size() == 4) {
                String commitTime = infos[0]
                String gitHash = infos[1]
                String commitUser = infos[2]
                String commitInfo = infos[3]
                String version = "${commitTime}_${gitHash}"
                Logger.buildOutput("filename:$impDir.absolutePath git最近提交：$version")
                if (!TextUtils.isEmpty(version)) {
                    impSourceSet.gitVersion = version
                    publication.implVersion = version
                    impSourceSet.commitTime = commitTime
                    impSourceSet.commitUser = commitUser
                    impSourceSet.gitCommitInfo = commitInfo
                }
            }
        }
        def sourceDir
        if (publication.sourceSetName.contains('/')) {
            sourceDir = new File(project.projectDir, publication.sourceSetName)
        } else {
            sourceDir = new File(project.projectDir, 'src/' + publication.sourceSetName)
        }
        long quickVerify = 0L
        project.fileTree(sourceDir).each {
            SourceFile sourceFile = new SourceFile()
            sourceFile.path = it.path
            sourceFile.lastModified = it.lastModified()
            impSourceSet.lastModifiedSourceFile.put(sourceFile.path, sourceFile)
            quickVerify += it.lastModified()
        }
        //检测build.gradle和proguard-rules.pro文件是否有变化
        File buildGradle = new File(project.projectDir, 'build.gradle')
        if (buildGradle.exists()) {
            SourceFile sourceFile = new SourceFile()
            sourceFile.path = buildGradle.path
            sourceFile.lastModified = buildGradle.lastModified()
            impSourceSet.lastModifiedSourceFile.put(sourceFile.path, sourceFile)
            quickVerify += buildGradle.lastModified()
        }
        File proguardRules = new File(project.projectDir, 'proguard-rules.pro')
        if (proguardRules.exists()) {
            SourceFile sourceFile = new SourceFile()
            sourceFile.path = proguardRules.path
            sourceFile.lastModified = proguardRules.lastModified()
            impSourceSet.lastModifiedSourceFile.put(sourceFile.path, sourceFile)
            quickVerify += proguardRules.lastModified()
        }

        impSourceSet.quickVerifyModified = quickVerify
        publication.impSourceSet = impSourceSet
        publication.impNeedPack = !impSourceSet.lastModifiedSourceFile.isEmpty()
    }

    private static void initSdkSourceSet(Project project, PublicationOption publication, boolean isAutoVersion) {
        def buildSdk = new File(project.projectDir, Constants.BUILD_SDK_DIR)
        publication.buildDir = new File(buildSdk, publication.scrName)

        SourceSet sdkSourceSet = new SourceSet()
        def sdkDir
        if (publication.sourceSetName.contains('/')) {
            sdkDir = new File(project.projectDir, publication.sourceSetName + '/sdk/')
        } else {
            sdkDir = new File(project.projectDir, 'src/' + publication.sourceSetName + '/sdk/')
        }
        sdkSourceSet.path = sdkDir.absolutePath
        sdkSourceSet.lastModifiedSourceFile = new HashMap<>()
        long quickVerify = 0L
        if (isAutoVersion && !publication.useUserSdkVersion) {
            String split = "@_@"
            String gitInfo = FileUtil.shell("git log --max-count=1 --pretty=format:\"%cd${split}%h${split}%cn${split}%s\" --date=format:'%Y%m%d%H%M%S' ${sdkDir.absolutePath}")
            Logger.buildOutput("filename:$sdkDir.absolutePath gitInfo：$gitInfo")
            String[] infos = gitInfo.split(split)
            if (infos.size() == 4) {
                String commitTime = infos[0]
                String gitHash = infos[1]
                String commitUser = infos[2]
                String commitInfo = infos[3]
                String version = "${commitTime}_${gitHash}"
                Logger.buildOutput("filename:$sdkDir.absolutePath git最近提交：$version")
                if (!TextUtils.isEmpty(version)) {
                    sdkSourceSet.gitVersion = version
                    publication.sdkVersion = version
                    sdkSourceSet.commitTime = commitTime
                    sdkSourceSet.commitUser = commitUser
                    sdkSourceSet.gitCommitInfo = commitInfo
                }
            }
        }
        project.fileTree(sdkDir).each {
            if (FileUtil.isValidPackSource(it)) {
                SourceFile sourceFile = new SourceFile()
                sourceFile.path = it.path
                sourceFile.lastModified = it.lastModified()
                quickVerify += it.lastModified()
                sdkSourceSet.lastModifiedSourceFile.put(sourceFile.path, sourceFile)
            }
        }
        sdkSourceSet.quickVerifyModified = quickVerify

        publication.sdkSourceSet = sdkSourceSet
        publication.invalid = quickVerify == 0L
    }

    static void createPublishingPublication(Project project, PublicationOption publication, String publicationName) {
        PublishingExtension publishing = project.extensions.getByName(Constants.PUBLISHING)
        MavenPublication mavenPublication = publishing.publications.maybeCreate(publicationName, MavenPublication)
        mavenPublication.groupId = publication.groupId
        mavenPublication.artifactId = publication.artifactId
        mavenPublication.version = publication.versionNew != null ? publication.versionNew : publication.sdkVersion
        mavenPublication.pom.packaging = 'jar'
        publishing.repositories(Runtimes.sSdkOption.repositories)

        File target = new File(Runtimes.sSdkDir, PublicationUtil.getJarName(publication))
        def outputsDir = new File(publication.buildDir, "outputs")
        mavenPublication.artifact source: target
        mavenPublication.artifact source: new File(outputsDir, "classes-source.jar"), classifier: 'sources'
        //todo 存在找不到引用问题 暂不处理
//        if (publication.dependencies != null) {
//            mavenPublication.pom.withXml {
//                def dependenciesNode = asNode().appendNode('dependencies')
//                if (publication.dependencies.implementation != null) {
//                    publication.dependencies.implementation.each {
//                        String[] gav
//                        def exclusion
//                        if (it instanceof PublicationDependencyModuleOption) {
//                            gav = it.path.split(":")
//                            exclusion = it.exclude
//                        } else {
//                            gav = it.split(":")
//                        }
//                        if (gav[1].startsWith(Constants.SDK_PRE)) {
//                            PublicationOption dependencyPublication = publicationManager.getPublicationByKey(gav[1].replace(Constants.SDK_PRE, ''))
//                            if (dependencyPublication.useLocal) {
//                                throw new RuntimeException("component publication [$dependencyPublication.groupId:$dependencyPublication.artifactId] has not publish yet.")
//                            }
//                        }
//                        if (gav.size() == 3) {
//                            //todo 存在本地引用的情况
//                            def dependencyNode = dependenciesNode.appendNode('dependency')
//                            dependencyNode.appendNode('groupId', gav[0])
//                            dependencyNode.appendNode('artifactId', gav[1])
//                            dependencyNode.appendNode('version', gav[2])
//                            dependencyNode.appendNode('scope', 'implementation')
//                            if (exclusion != null) {
//                                def exclusionsNode = dependencyNode.appendNode('exclusions')
//                                def exclusionNode = exclusionsNode.appendNode('exclusion')
//                                exclusionNode.appendNode('groupId', exclusion['group'])
//                                exclusionNode.appendNode('artifactId', exclusion['module'])
//                            }
//                        }
//                    }
//                }
//            }
//        }
    }

    static Task createPublishTask(Project project, Project androidProject, PublicationOption publication) {

//        for test
//        TaskProvider<Task> cleanTaskProvider = project.tasks.named("clean")
//        Task cleanTask = cleanTaskProvider.get()

        def taskName = 'compile[' + publication.artifactId + ']Source'
        def compileTask = project.getTasks().findByName(taskName)
        if (compileTask == null) {
            compileTask = project.getTasks().create(taskName, CompileSdkTask.class)
            compileTask.publication = publication
        }
//        cleanTask.finalizedBy(compileTask)

        def publicationName = 'Component-' + project.name + 'Sdk'
        String publishTaskNamePrefix = "publish${publicationName}PublicationTo"
        project.tasks.whenTaskAdded {
            if (it.name.startsWith(publishTaskNamePrefix)) {
                it.dependsOn compileTask
                it.doLast {
                    publication.invalid = false
                    publication.useLocal = false
                    PublicationManager.getInstance().addPublication(publication)
                }
            }
        }
        createPublishingPublication(project, publication, publicationName)
        TaskProvider<Task> publishTask = project.tasks.named("${publishTaskNamePrefix}MavenRepository")
        compileTask.finalizedBy(publishTask.get())

        return compileTask
    }

    static void createImpPublishingPublication(Project project, PublicationOption publication, String publicationName, boolean isRelease) {
        def publishing = project.extensions.getByName(Constants.PUBLISHING)
        MavenPublication mavenPublication = publishing.publications.maybeCreate(publicationName, MavenPublication)
        File target = new File(Runtimes.sImplDir, getAarName(publication))
        if (mavenPublication.getArtifacts().size() != 0) {
            //fixme 这里会执行两次所以过滤一下
            return
        }
        mavenPublication.groupId = publication.groupId
        if (isRelease) {
            mavenPublication.artifactId = publication.getArtifactIdString() + "-imp"
        } else {
            mavenPublication.artifactId = publication.getArtifactIdString() + "-imp-debug"
        }
        mavenPublication.version = publication.versionNew != null ? publication.versionNew : publication.implVersion
        mavenPublication.pom.packaging = 'aar'
        Logger.buildOutput("createImpPublishingPublication ${publicationName} Artifacts:${mavenPublication.getArtifacts().size()}")
        mavenPublication.getArtifacts().clear()
        mavenPublication.artifact(target.absolutePath)
        publishing.repositories(Runtimes.sSdkOption.repositories)
//        mavenPublication.pom.withXml {
//            def dependenciesNode = asNode().appendNode('dependencies')
//            def addDependency = { Dependency dep, String scope ->
//                if (dep.group == null || dep.version == null || dep.name == null || dep.name == "unspecified")
//                    return // ignore invalid dependencies
//
//                final dependencyNode = dependenciesNode.appendNode('dependency')
//                dependencyNode.appendNode('groupId', dep.group)
//                dependencyNode.appendNode('artifactId', dep.name)
//                dependencyNode.appendNode('version', dep.version)
//                dependencyNode.appendNode('scope', scope)
//
//                if (!dep.transitive) {
//                    // If this dependency is transitive, we should force exclude all its dependencies them from the POM
//                    final exclusionNode = dependencyNode.appendNode('exclusions').appendNode('exclusion')
//                    exclusionNode.appendNode('groupId', '*')
//                    exclusionNode.appendNode('artifactId', '*')
//                } else if (!dep.properties.excludeRules.empty) {
//                    // Otherwise add specified exclude rules
//                    final exclusionNode = dependencyNode.appendNode('exclusions').appendNode('exclusion')
//                    dep.properties.excludeRules.each { ExcludeRule rule ->
//                        exclusionNode.appendNode('groupId', rule.group ?: '*')
//                        exclusionNode.appendNode('artifactId', rule.module ?: '*')
//                    }
//                }
//            }
//
//            project.configurations.compile.getDependencies().each { dep -> addDependency(dep, "compile") }
//            project.configurations.api.getDependencies().each { dep -> addDependency(dep, "api") }
//            project.configurations.implementation.getDependencies().each { dep -> addDependency(dep, "implementation") }
//        }
    }

    /**
     * 把插件定义的依赖过滤出来
     * @param publication
     */
    static void filterPublicationDependencies(PublicationOption publication) {
        if (publication.dependencies != null) {
            if (publication.dependencies.compileOnly != null) {
                List<Object> compileOnly = new ArrayList<>()
                publication.dependencies.compileOnly.each {
                    if (it instanceof String && it.startsWith(Constants.COMPONENT_PRE)) {
//                        PublicationOption existPublication = PublicationManager.getInstance().getPublicationByKey(getPublicationId(publication))
                        PublicationOption existPublication = parseComponentDependency(it)
                        if (existPublication != null) {
                            if (existPublication.useLocal) {
                                compileOnly.add(getLocalGAV(existPublication))
                            } else {
                                compileOnly.add(getMavenGAV(existPublication))
                            }
                        }
                    } else {
                        compileOnly.add(it)
                    }
                }
                publication.dependencies.compileOnly = compileOnly
            }
            if (publication.dependencies.implementation != null) {
                List<Object> implementation = new ArrayList<>()
                publication.dependencies.implementation.each {
//                    Logger.buildOutput("filterDependencies : publication:${getPublicationId(publication)} implementation: ${it}")
                    if (it instanceof String && it.startsWith(Constants.COMPONENT_PRE)) {
//                        PublicationOption existPublication = PublicationManager.getInstance().getPublicationByKey(getPublicationId(publication))
                        PublicationOption existPublication = parseComponentDependency(it)
                        if (existPublication != null) {
                            if (existPublication.useLocal) {
                                implementation.add(getLocalGAV(existPublication))
//                                Logger.buildOutput("filterDependencies : publication:${getPublicationId(publication)} implementation useLocalGav: ${getLocalGAV(existPublication)}")
                            } else {
                                implementation.add(getMavenGAV(existPublication))
//                                Logger.buildOutput("filterDependencies : publication:${getPublicationId(publication)} implementation useMavenGav: ${getMavenGAV(existPublication)}")
                            }
                        }
                    } else {
                        implementation.add(it)
                    }
                }
                publication.dependencies.implementation = implementation
            }
        }
    }

    static void handleSdkPublish(Project childProject, Project androidProject, PublicationOption publication, Task dependTask) {
        if (publication.sdkVersion != null && publication.sdkNeedPublish) {
            if (JarUtil.isMavenJarExists(childProject, publication)) {
                publication.sdkNeedPublish = false
                return
            }
            childProject.plugins.apply(Constants.PLUGIN_MAVEN_PUBLISH)
            Logger.buildOutput("need publish sdk:${publication.name}")
            Task compileTask = createPublishTask(childProject, androidProject, publication)
            if (dependTask == null) {
                AppExtension appExtension = androidProject.extensions.getByType(AppExtension.class)
                appExtension.applicationVariants.each {
                    TaskProvider<Task> assembleTask = AarUtil.getAssembleTask(androidProject, it)
                    Logger.buildOutput("getAssembleTask:${assembleTask.name}")
                    dependTask = assembleTask.get()
                    dependTask.finalizedBy(compileTask)
                }
            } else {
                compileTask.dependsOn(dependTask)
                dependTask.finalizedBy(compileTask)
            }
        }
    }

    static void handleImplPublish(Project project, PublicationOption publication, boolean isDebug, Task dependTask) {
        boolean isAarExists = AarUtil.isArrExits(project, publication, !isDebug)
        if (isAarExists) {
            publication.impNeedPublish = false
            Logger.buildOutput("Handle ${publication.name} ImplPublish,isDebug:${isDebug} impNeedPublish:${publication.impNeedPublish}")
            return
        }
        Logger.buildOutput("Handle ${publication.name} ImplPublish,isDebug:${isDebug} impNeedPublish:${publication.impNeedPublish}")
        project.plugins.apply(Constants.PLUGIN_MAVEN_PUBLISH)
        if (isDebug) {
            //注册打包arr task
            AarUtil.packImpAar(project, ComponentPlugin.androidProject, publication, dependTask)
        } else {
            AarUtil.packImpAarRelease(project, ComponentPlugin.androidProject, publication, dependTask)
        }
    }
}
