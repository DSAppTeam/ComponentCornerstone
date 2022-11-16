package com.plugin.component.utils

import com.android.build.gradle.AppExtension
import com.android.build.gradle.api.ApplicationVariant
import com.plugin.component.Runtimes
import com.plugin.component.extension.PublicationManager
import com.plugin.component.extension.option.sdk.PublicationOption
import com.plugin.component.log.Logger
import com.plugin.component.task.AarLocalPublishTask
import com.plugin.component.task.ComponentPublishTask
import org.apache.http.util.TextUtils
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.tasks.TaskProvider

class AarUtil {

    static void packImpAar(Project currentProject, Project androidProject, PublicationOption publication, Task dependTask) {
        AppExtension appExtension = androidProject.extensions.getByType(AppExtension.class)
        appExtension.applicationVariants.each {
            if (it.buildType.name.toLowerCase().contains("debug")) {
                if (dependTask == null) {
                    TaskProvider<Task> assembleTask = getAssembleTask(androidProject, it)
                    Logger.buildOutput("getAssembleTask:${assembleTask.name}")
                    dependTask = assembleTask.get()
                }
                hookBundleAarTask(currentProject, dependTask, it.buildType.name, publication, false)
            }
        }
    }

    static void packImpAarRelease(Project currentProject, Project androidProject, PublicationOption publication, Task dependTask) {
        AppExtension appExtension = androidProject.extensions.getByType(AppExtension.class)
        appExtension.applicationVariants.each {
            if (it.buildType.name.toLowerCase().contains("release")) {
                if (dependTask == null) {
                    TaskProvider<Task> assembleTask = getAssembleTask(androidProject, it)
                    Logger.buildOutput("getAssembleTask:${assembleTask.name}")
                    dependTask = assembleTask.get()
                }
                hookBundleAarTask(currentProject, dependTask, it.buildType.name, publication, true)
            }
        }
    }

    static TaskProvider<Task> getAssembleTask(Project androidProject, ApplicationVariant variant) {
        TaskProvider<Task> assembleTask =
                androidProject.tasks.named("assemble${variant.flavorName.capitalize()}${variant.buildType.name.capitalize()}")
        return assembleTask
    }

    static boolean isArrExits(Project project, PublicationOption publication, boolean isRelease) {
        String filePath = null
        String fileName = isRelease ? publication.getArtifactIdString() + "-imp-" + publication.implVersion + ".aar" : publication.getArtifactIdString() + "-imp-debug-" + publication.implVersion + ".aar"
        String artifactId = isRelease ? publication.getArtifactIdString() + "-imp" : publication.getArtifactIdString() + "-imp-debug"
        //  http://172.16.xxx.xxx:8081/nexus/content/groups/public/com/xxx/cif/xxx-cif-api/0.0.1-SNAPSHOT/xxx-cif-api-0.0.1-20170515.040917-89.jar
        String url = Runtimes.sSdkOption.getMavenUrl()
        String result = HttpUrlConnectHelper.sendRequest("$url/${publication.groupId.replace('.', '/')}/$artifactId/${publication.implVersion}/$fileName","HEAD")
        if (!TextUtils.isEmpty(result)) {
            filePath = "$url/${publication.groupId.replace('.', '/')}/$artifactId/${publication.implVersion}/$fileName"
        }
//        def name = "component[${publication.groupId}-${publication.artifactId}]Classpath"
//        Configuration configuration = project.configurations.create(name)
//        project.dependencies.add(name, PublicationUtil.getImpMavenGAV(publication, isRelease))
//        try {
//            configuration.copy().files.each {
//                Logger.buildOutput("aarName:${it.name}")
//                if (it.name.endsWith(fileName)) {
//                    filePath = it.absolutePath
//                }
//            }
//        } catch (Exception e) {
////            e.printStackTrace()
//            Logger.buildOutput(e.getMessage())
//        }
//        project.configurations.remove(configuration)
        return filePath != null
    }

    private static void hookBundleAarTask(Project currentProject, Task assembleTask, String buildType, PublicationOption publication, boolean isRelease) {
        Task bundleTask = assembleTask
        if (publication.impNeedPack) {
            bundleTask = getBundleAarTask(currentProject, buildType.capitalize())
            bundleTask.getTaskDependencies().getDependencies(bundleTask).each {
                if (it != assembleTask && assembleTask instanceof ComponentPublishTask) {
                    //找到bundleAar的所有task，让他们都依赖ComponentPublishTask
                    it.dependsOn(assembleTask)
                }
            }
            bundleTask.dependsOn(assembleTask)
            bundleTask.mustRunAfter(assembleTask)
            assembleTask.finalizedBy(bundleTask)
        }
        AarLocalPublishTask aarLocalPublishTask = currentProject.tasks.maybeCreate("uploadLocalMaven" + buildType.capitalize(), AarLocalPublishTask.class)
        aarLocalPublishTask.publication = publication
        aarLocalPublishTask.dependsOn(bundleTask)
        bundleTask.finalizedBy(aarLocalPublishTask)
        aarLocalPublishTask.doFirst {
            Logger.buildOutput("${aarLocalPublishTask.name} start")
        }
        aarLocalPublishTask.doLast {
            Logger.buildOutput("${aarLocalPublishTask.name} finish")
        }

        if (publication.impNeedPublish) {
            def publicationName = 'Component-' + currentProject.name + buildType.capitalize() + 'Imp'
            String publishTaskNamePrefix = "publish${publicationName}PublicationTo"
            currentProject.tasks.whenTaskAdded {
                if (it.name.startsWith(publishTaskNamePrefix)) {
                    it.dependsOn aarLocalPublishTask
                    it.doLast {
                        PublicationManager.getInstance().addImpPublication(publication)
                    }
                }
            }

            PublicationUtil.createImpPublishingPublication(currentProject, publication, publicationName, isRelease)
            TaskProvider<Task> publishTask = currentProject.tasks.named("${publishTaskNamePrefix}MavenRepository")
            aarLocalPublishTask.finalizedBy(publishTask.get())
        }
    }

    /**
     * 获取到每个模块的bundleXXXAar task,用于打包每个module的aar
     * @param currentProject
     * @param variantName
     */
    private static Task getBundleAarTask(Project currentProject, String variantName) {
        String taskName = "bundle${variantName}Aar"
        TaskProvider<Task> bundleAarTask = currentProject.tasks.named(taskName)
        return bundleAarTask.get()
    }

}