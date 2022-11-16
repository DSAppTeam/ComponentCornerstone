package com.plugin.component.task

import com.plugin.component.Runtimes
import com.plugin.component.log.Logger
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

/**
 * 组件发布
 * create by linyue 2022/08/22
 */
class ComponentPublishTask extends DefaultTask {

    ComponentPublishTask() {
        setGroup("component plugin")
    }

    @TaskAction
    void publish() {
        Logger.buildOutput("startPublish")
        Runtimes.checkPublishEnable(project)
    }

}