package com.plugin.module

import com.android.build.gradle.BaseExtension
import com.plugin.module.extension.ModuleRuntime
import com.plugin.module.extension.module.AloneConfiguration
import com.plugin.module.extension.module.AssembleTask
import com.plugin.module.extension.module.Dependencies
import com.plugin.module.extension.ModuleExtension
import com.plugin.module.extension.module.SourceFile
import com.plugin.module.extension.module.SourceSet
import com.plugin.module.listener.OnModuleExtensionListener
import com.plugin.module.extension.publication.Publication
import com.plugin.module.extension.publication.PublicationManager
import com.plugin.module.transform.CodeTransform
import com.plugin.module.utils.FileUtil
import com.plugin.module.utils.JarUtil
import com.plugin.module.utils.MisUtil
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.maven.MavenPublication

/**
 *   ./gradlew --no-daemon ModulePlugin  -Dorg.gradle.debug=true
 */
class ModulePlugin implements Plugin<Project> {

    static File misDir
    public static ModuleExtension sModuleExtension
    static String androidJarPath
    static PublicationManager publicationManager
    Project project

    @Override
    void apply(Project project) {
        this.project = project;
        if (project == project.rootProject) {
            handleRootProject(project)
        } else {
            handleProject(project)
        }
    }

    /**
     * 处理非rootproject
     * @param project
     */
    private void handleProject(Project project) {

        String compileModule = Constants.DEFAULT_APP_NAME

        //获取运行task名称
        String taskNames = project.gradle.startParameter.taskNames.toString()
        Logger.buildOutput("taskNames is " + taskNames)

        //获取运行模块名称
        String module = project.path.replace(":", "")
        Logger.buildOutput("current module is " + module)

        //解析AssembleTask
        Logger.buildOutput("startParameter.taskNames is " + project.gradle.startParameter.taskNames)
        AssembleTask assembleTask = com.plugin.module.utils.Utils.parseTaskInfo(project.gradle.startParameter.taskNames)
        if (assembleTask.isAssemble) {
            compileModule = com.plugin.module.utils.Utils.parseMainModuleName(project, assembleTask)
            Logger.buildOutput("compile module is : " + compileModule)
        }

        //需要在特定的模块中声明 isRunAlone，用于判断是否单独运行
        if (!project.hasProperty(Constants.PROPERTIES_ISRUNALONE)) {
            throw new RuntimeException("you should set isRunAlone in " + module + "'s gradle.properties")
        }


        boolean isRunAlone = Boolean.parseBoolean((project.properties.get("isRunAlone")))
        String mainModuleName = project.rootProject.property(Constants.PROPERTIES_MAIN_MODULE_NAME)


        //当且仅当 isRunAlone 为ture需要判断
        if (isRunAlone && assembleTask.isAssemble) {
            //如果运行的模块就是app模块，或者当前运行的模块就是我们配置的mainmodulename，则默认需要单独运行，其他组件强制修改为false
            if (module.equals(compileModule)) {
                isRunAlone = true
            } else {
                isRunAlone = false
            }
        }
        project.setProperty("isRunAlone", isRunAlone)
        Logger.buildOutput("setProperty isRunAlone(" + isRunAlone + ")")
        boolean needAloneSourceSrt = false

        //根据配置添加各种组件依赖，并且自动化生成组件加载代码
        if (isRunAlone) {
            project.apply plugin: Constants.PLUGIN_APPLICATION
            Logger.buildOutput("project.apply plugin: com.android.application")

            //对于组件，则需要读取alone目录进行运行
            if (!module.equals(mainModuleName)) {
                needAloneSourceSrt = true;

                project.android.sourceSets {
                    main {
                        manifest.srcFile Constants.AFTER_MANIFEST_PATH
                        java.srcDirs = [Constants.JAVA_PATH, Constants.AFTER_JAVA_PATH]
                        res.srcDirs = [Constants.RES_PATH, Constants.AFTER_RES_PATH]
                        assets.srcDirs = [Constants.ASSETS_PATH, Constants.AFTER_ASSETS_PATH]
                        jniLibs.srcDirs = [Constants.JNILIBS_PATH, Constants.AFTER_JNILIBS_PATH]
                    }
                }

            }
            if (assembleTask.isAssemble && module.equals(compileModule)) {
                com.plugin.module.utils.Utils.compileComponents(project, assembleTask)
                //参考https://github.com/luojilab/DDComponentForAndroid/issues/122
                project.extensions.findByType(BaseExtension.class).registerTransform(new CodeTransform(project));
//                project.android.registerTransform(new CodeTransform(project))
            }
        } else {
            project.apply plugin: Constants.PLUGIN_LIBRARY
            Logger.buildOutput("project.apply plugin: com.android.library")
        }


//        if (!MisUtil.hasAndroidPlugin(project)) {
//            throw new GradleException("The android or android-library plugin must be applied to the project.")
//        }


        //解析 misPublication(xxxx) 中的字符串转化为 maven 格式
        project.dependencies.metaClass.misPublication { Object value ->
            String[] gav = MisUtil.filterGAV(value)
            return getPublication(gav[0], gav[1])
        }

        //获取当前project 可能依赖scope 使用的 misPublication(xxxx)，转化为 implementation
        List<Publication> publications = publicationManager.getPublicationByProject(project)
        project.dependencies {
            publications.each {
                implementation getPublication(it.groupId, it.artifactId)
            }
        }

        //添加每一个 publication 的依赖
        if (project.gradle.startParameter.taskNames.isEmpty()) {
            publications.each {
                addPublicationDependencies(it)
            }
        }


        project.afterEvaluate {
            //调整sourceSet
            MisUtil.addMisSourceSets(project)

//            if(needAloneSourceSrt){
//                MisUtil.addAloneSourceSet(project)
//            }


            List<Publication> publicationList = publicationManager.getPublicationByProject(project)

            List<Publication> publicationPublishList = new ArrayList<>()
            publicationList.each {
                if (it.version != null) {
                    publicationPublishList.add(it)
                }
            }

            if (publicationPublishList.size() > 0) {
                project.plugins.apply('maven-publish')
                def publishing = project.extensions.getByName('publishing')
                if (sModuleExtension.configure != null) {
                    publishing.repositories sModuleExtension.configure
                }

                publicationPublishList.each {
                    createPublishTask(it)
                }
            }
        }
    }

    /**
     * 处理root peoject
     * @param project
     */
    private void handleRootProject(Project project) {
        //重置 .gradle/mis 文件
        misDir = new File(project.projectDir, '.gradle/mis')
        if (!misDir.exists()) {
            misDir.mkdirs()
        }
        project.gradle.getStartParameter().taskNames.each {
            if (it == 'clean') {
                if (!misDir.deleteDir()) {
                    throw new RuntimeException("unable to delete dir " + misDir.absolutePath)
                }
                misDir.mkdirs()
            }
        }
        project.repositories {
            flatDir {
                dirs misDir.absolutePath
            }
        }

        //读取 manifest
        publicationManager = PublicationManager.getInstance()
        publicationManager.loadManifest(project, misDir)

        sModuleExtension = project.getExtensions().create("module", ModuleExtension, new OnModuleExtensionListener() {

            @Override
            void onPublicationAdded(Project childProject, Publication publication) {
                initPublication(childProject, publication)
                publicationManager.addDependencyGraph(publication)
                ModuleRuntime.publicationMap.put(childProject.name, publication)
            }

            @Override
            void onAloneConfigAdded(Project childProject, AloneConfiguration aloneConfiguration) {
                ModuleRuntime.aloneRunMap.put(childProject.name, aloneConfiguration)
            }
        })

        project.allprojects.each {
            if (it == project) return
            Project childProject = it
            childProject.repositories {
                flatDir {
                    dirs misDir.absolutePath
                }
            }
        }

        project.afterEvaluate {

            androidJarPath = MisUtil.getAndroidJarPath(project, sModuleExtension.compileSdkVersion)

            Dependencies.metaClass.misPublication { String value ->
                String[] gav = MisUtil.filterGAV(value)
                return 'mis-' + gav[0] + ':' + gav[1] + ':' + gav[2]
            }

            project.allprojects.each {
                if (it == project) return
                Project childProject = it
                def misScript = new File(childProject.projectDir, 'module.gradle')
                if (misScript.exists()) {
                    sModuleExtension.currentChildProject = childProject
                    project.apply from: misScript
                }
            }

            List<String> topSort = publicationManager.dependencyGraph.topSort()
            Collections.reverse(topSort)
            topSort.each {
                Publication publication = publicationManager.publicationDependencies.get(it)
                if (publication == null) {
                    return
                }

                Project childProject = project.findProject(publication.project)
                filterPublicationDependencies(publication)
                if (publication.version != null) {
                    handleMavenJar(childProject, publication)
                } else {
                    handleLocalJar(childProject, publication)
                }
                publicationManager.hitPublication(publication)
            }

        }
    }


    def filterPublicationDependencies(Publication publication) {
        if (publication.dependencies != null) {
            if (publication.dependencies.compileOnly != null) {
                List<Object> compileOnly = new ArrayList<>()
                publication.dependencies.compileOnly.each {
                    if (it instanceof String && it.startsWith('mis-')) {
                        String[] gav = MisUtil.filterGAV(it.replace('mis-', ''))
                        Publication existPublication = publicationManager.getPublicationByKey(gav[0] + '-' + gav[1])
                        if (existPublication != null) {
                            if (existPublication.useLocal) {
                                compileOnly.add(':mis-' + existPublication.groupId + '-' + existPublication.artifactId + ':')
                            } else {
                                compileOnly.add(existPublication.groupId + ':' + existPublication.artifactId + ':' + existPublication.version)
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
                    if (it instanceof String && it.startsWith('mis-')) {
                        String[] gav = MisUtil.filterGAV(it.replace('mis-', ''))
                        Publication existPublication = publicationManager.getPublicationByKey(gav[0] + '-' + gav[1])
                        if (existPublication != null) {
                            if (existPublication.useLocal) {
                                implementation.add(':mis-' + existPublication.groupId + '-' + existPublication.artifactId + ':')
                            } else {
                                implementation.add(existPublication.groupId + ':' + existPublication.artifactId + ':' + existPublication.version)
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

    def handleLocalJar(Project project, Publication publication) {
        File target = new File(misDir, 'mis-' + publication.groupId + '-' + publication.artifactId + '.jar')

        if (publication.invalid) {
            publicationManager.addPublication(publication)
            if (target.exists()) {
                target.delete()
            }
            return
        }

        if (target.exists()) {
            boolean hasModifiedSource = publicationManager.hasModified(publication)
            if (!hasModifiedSource) {
                publication.invalid = false
                publication.useLocal = true
                publicationManager.addPublication(publication)
                return
            }
        }

        File releaseJar = JarUtil.packJavaSourceJar(project, publication, androidJarPath, sModuleExtension.compileOptions, true)
        if (releaseJar == null) {
            publication.invalid = true
            publicationManager.addPublication(publication)
            if (target.exists()) {
                target.delete()
            }
            return
        }

        FileUtil.copyFile(releaseJar, target)
        publication.invalid = false
        publication.useLocal = true
        publicationManager.addPublication(publication)
    }

    def handleMavenJar(Project project, Publication publication) {
        File target = new File(misDir, 'mis-' + publication.groupId + '-' + publication.artifactId + '.jar')
        if (publication.invalid) {
            publicationManager.addPublication(publication)
            if (target.exists()) {
                target.delete()
            }
            return
        }

        boolean hasModifiedSource = publicationManager.hasModified(publication)

        if (target.exists()) {
            if (hasModifiedSource) {
                def releaseJar = JarUtil.packJavaSourceJar(project, publication, androidJarPath, sModuleExtension.compileOptions, true)
                if (releaseJar == null) {
                    publication.invalid = true
                    publicationManager.addPublication(publication)
                    if (target.exists()) {
                        target.delete()
                    }
                    return
                }
                FileUtil.copyFile(releaseJar, target)
            }
            publication.invalid = false
            publication.useLocal = true
            publicationManager.addPublication(publication)
        } else if (!hasModifiedSource) {
            Publication lastPublication = publicationManager.getPublication(publication.groupId, publication.artifactId)
            if (lastPublication.version != publication.version) {
                publication.versionNew = publication.version
                publication.version = lastPublication.version
            }
            publication.invalid = false
            publication.useLocal = false
            publicationManager.addPublication(publication)
            return
        } else {
            def releaseJar = JarUtil.packJavaSourceJar(project, publication, androidJarPath, sModuleExtension.compileOptions, false)
            if (releaseJar == null) {
                publication.invalid = true
                publicationManager.addPublication(publication)
                if (target.exists()) {
                    target.delete()
                }
                return
            }

            Publication lastPublication = publicationManager.getPublication(publication.groupId, publication.artifactId)
            if (lastPublication == null) {
                lastPublication = publication
            }
            boolean equals = JarUtil.compareMavenJar(project, lastPublication, releaseJar.absolutePath)
            if (equals) {
                if (target.exists()) {
                    target.delete()
                }
                publication.useLocal = false
            } else {
                releaseJar = JarUtil.packJavaSourceJar(project, publication, androidJarPath, sModuleExtension.compileOptions, true)
                FileUtil.copyFile(releaseJar, target)
                publication.useLocal = true
            }
            publication.invalid = false
            publicationManager.addPublication(publication)
        }
    }

    def getPublication(String groupId, String artifactId) {
        Publication publication = publicationManager.getPublication(groupId, artifactId)
        if (publication != null) {
            if (publication.invalid) {
                return []
            } else if (publication.useLocal) {
                return ':mis-' + publication.groupId + '-' + publication.artifactId + ':'
            } else {
                return publication.groupId + ':' + publication.artifactId + ':' + publication.version
            }
        } else {
            return []
        }
    }

    void addPublicationDependencies(Publication publication) {
        if (publication.dependencies == null) return
        project.dependencies {
            if (publication.dependencies.compileOnly != null) {
                publication.dependencies.compileOnly.each {
                    compileOnly it
                }
            }
            if (publication.dependencies.implementation != null) {
                publication.dependencies.implementation.each {
                    implementation it
                }
            }
        }
    }


    void createPublishTask(Publication publication) {
        def taskName = 'compileMis[' + publication.artifactId + ']Source'
        def compileTask = project.getTasks().findByName(taskName)
        if (compileTask == null) {
            compileTask = project.getTasks().create(taskName, CompileMisTask.class)
            compileTask.publication = publication
            compileTask.dependsOn 'clean'
        }

        def publicationName = 'Mis[' + publication.artifactId + ']'
        String publishTaskNamePrefix = "publish${publicationName}PublicationTo"
        project.tasks.whenTaskAdded {
            if (it.name.startsWith(publishTaskNamePrefix)) {
                it.dependsOn compileTask
                it.doLast {
                    new File(misDir, 'mis-' + publication.groupId + '-' + publication.artifactId + '.jar').delete()
                }
            }
        }
        createPublishingPublication(publication, publicationName)
    }

    void createPublishingPublication(Publication publication, String publicationName) {
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
                        if (gav[1].startsWith('mis-')) {
                            Publication dependencyPublication = publicationManager.getPublicationByKey(gav[1].replace('mis-', ''))
                            if (dependencyPublication.useLocal) {
                                throw new RuntimeException("mis publication [$dependencyPublication.groupId:$dependencyPublication.artifactId] has not publish yet.")
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

    void initPublication(Project project, Publication publication) {
        String displayName = project.getDisplayName()
        publication.project = displayName.substring(displayName.indexOf("'") + 1, displayName.lastIndexOf("'"))
        def buildMis = new File(project.projectDir, 'build/mis')

        publication.sourceSetName = publication.name
        publication.buildDir = new File(buildMis, publication.name)

        SourceSet misSourceSet = new SourceSet()
        def misDir
        if (publication.sourceSetName.contains('/')) {
            misDir = new File(project.projectDir, publication.sourceSetName + '/mis/')
        } else {
            misDir = new File(project.projectDir, 'src/' + publication.sourceSetName + '/mis/')
        }
        misSourceSet.path = misDir.absolutePath
        misSourceSet.lastModifiedSourceFile = new HashMap<>()
        project.fileTree(misDir).each {
            if (it.name.endsWith('.java') || it.name.endsWith('.kt')) {
                SourceFile sourceFile = new SourceFile()
                sourceFile.path = it.path
                sourceFile.lastModified = it.lastModified()
                misSourceSet.lastModifiedSourceFile.put(sourceFile.path, sourceFile)
            }
        }

        publication.misSourceSet = misSourceSet
        publication.invalid = misSourceSet.lastModifiedSourceFile.isEmpty()
    }

}
