package com.plugin.component.utils

import com.plugin.component.Constants
import com.plugin.component.Runtimes
import com.plugin.component.extension.module.ProjectInfo
import com.plugin.component.extension.module.SourceFile
import com.plugin.component.extension.module.SourceSet
import com.plugin.component.extension.option.PublicationOption
import com.plugin.component.task.CompileSdkTask
import org.gradle.api.Project
import org.gradle.api.publish.maven.MavenPublication

class PublicationUtil {

    static getPublicationId(PublicationOption publication) {
        return publication.groupId + '-' + publication.artifactId
    }

    static getPublicationId(String groupId,String artifactId) {
        return groupId + '-' + artifactId
    }

    static getJarName(PublicationOption publication) {
        return publication.groupId + '-' + publication.artifactId + '.jar'
    }

    static getMavenGAV(PublicationOption publication) {
        return publication.groupId + ':' + publication.artifactId + ':' + publication.version
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
        PublicationOption publication = Runtimes.getSdkPublication(key)
        if (publication != null) {
            if (projectInfo.aloneEnableAndNoSync()) {
                //解决多渠道依赖时缺失依赖project渠道信息的问题，0.1.1 版本
//                Map<String,Object> map = new HashMap<>()
//                map.put("path",value)
//                map.put("configuration",'default')
//                return projectInfo.project.getDependencies().project(map)
                //gradle版本升级解决
                return projectInfo.project.project(':' + value)
            } else {
                return getPublication(publication)
            }
        }
        return []
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
                    if (it instanceof String && it.startsWith(Constants.COMPONENT)) {
                        compileOnly parseComponent(projectInfo, it.replace(Constants.COMPONENT, ""))
                    } else {
                        compileOnly it
                    }
                }
            }
            if (publication.dependencies.implementation != null) {
                publication.dependencies.implementation.each {
                    if (it instanceof String && it.startsWith(Constants.COMPONENT)) {
                        implementation parseComponent(projectInfo, it.replace(Constants.COMPONENT, ""))
                    } else {
                        implementation it
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
    static void initPublication(Project project, PublicationOption publication) {
        String displayName = project.getDisplayName()
        publication.project = displayName.substring(displayName.indexOf("'") + 1, displayName.lastIndexOf("'"))
        def buildSdk = new File(project.projectDir, publication.isSdk ? Constants.BUILD_SDK_DIR : Constants.BUILD_IMPL_DIR)

        publication.sourceSetName = publication.name
        publication.buildDir = new File(buildSdk, publication.name)

        SourceSet misSourceSet = new SourceSet()
        def misDir
        if (publication.sourceSetName.contains('/')) {
            misDir = new File(project.projectDir, publication.sourceSetName + '/sdk/')
        } else {
            misDir = new File(project.projectDir, 'src/' + publication.sourceSetName + '/sdk/')
        }
        misSourceSet.path = misDir.absolutePath
        misSourceSet.lastModifiedSourceFile = new HashMap<>()
        project.fileTree(misDir).each {
            if (FileUtil.isValidPackSource(it)) {
                SourceFile sourceFile = new SourceFile()
                sourceFile.path = it.path
                sourceFile.lastModified = it.lastModified()
                misSourceSet.lastModifiedSourceFile.put(sourceFile.path, sourceFile)
            }
        }

        publication.misSourceSet = misSourceSet
        publication.invalid = misSourceSet.lastModifiedSourceFile.isEmpty()
    }

    static void createPublishingPublication(Project project, PublicationOption publication, String publicationName) {
        def publishing = project.extensions.getByName('publishing')
        MavenPublication mavenPublication = publishing.publications.maybeCreate(publicationName, MavenPublication)
        mavenPublication.groupId = publication.groupId
        mavenPublication.artifactId = publication.artifactId
        mavenPublication.version = publication.versionNew != null ? publication.versionNew : publication.version
        mavenPublication.pom.packaging = 'jar'

        def outputsDir = new File(publication.buildDir, "outputs")
        mavenPublication.artifact source: new File(outputsDir, "classes.jar")
        mavenPublication.artifact source: new File(outputsDir, "classes-source.jar"), classifier: 'sources'

        if (publication.dependencies != null) {
            mavenPublication.pom.withXml {
                def dependenciesNode = asNode().appendNode('dependencies')
                if (publication.dependencies.implementation != null) {
                    publication.dependencies.implementation.each {
                        def gav = it.split(":")
                        if (gav[1].startsWith(Constants.SDK_PRE)) {
                            PublicationOption dependencyPublication = publicationManager.getPublicationByKey(gav[1].replace(Constants.SDK_PRE, ''))
                            if (dependencyPublication.useLocal) {
                                throw new RuntimeException("component publication [$dependencyPublication.groupId:$dependencyPublication.artifactId] has not publish yet.")
                            }
                        }
                        def dependencyNode = dependenciesNode.appendNode('dependency')
                        dependencyNode.appendNode('groupId', gav[0])
                        dependencyNode.appendNode('artifactId', gav[1])
                        dependencyNode.appendNode('version', gav[2])
                        dependencyNode.appendNode('scope', 'implementation')
                    }
                }
            }
        }
    }

    static void createPublishTask(Project project, PublicationOption publication) {
        def taskName = 'compileMis[' + publication.artifactId + ']Source'
        def compileTask = project.getTasks().findByName(taskName)
        if (compileTask == null) {
            compileTask = project.getTasks().create(taskName, CompileSdkTask.class)
            compileTask.publication = publication
            compileTask.dependsOn 'clean'
        }

        def publicationName = 'component[' + publication.artifactId + ']'
        String publishTaskNamePrefix = "publish${publicationName}PublicationTo"
        project.tasks.whenTaskAdded {
            if (it.name.startsWith(publishTaskNamePrefix)) {
                it.dependsOn compileTask
                it.doLast {
                    new File(misDir, PublicationUtil.getJarName(publication)).delete()
                }
            }
        }
        PublicationUtil.createPublishingPublication(project, publication, publicationName)
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
                        PublicationOption existPublication = PublicationManager.getInstance().getPublicationByKey(getPublicationId(publication))
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
                    if (it instanceof String && it.startsWith(Constants.COMPONENT_PRE)) {
                        PublicationOption existPublication = PublicationManager.getInstance().getPublicationByKey(getPublicationId(publication))
                        if (existPublication != null) {
                            if (existPublication.useLocal) {
                                implementation.add(getLocalGAV(existPublication))
                            } else {
                                implementation.add(getMavenGAV(existPublication))
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
}
