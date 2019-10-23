package com.plugin.component.plugin

import com.plugin.component.Logger
import com.plugin.component.Runtimes
import com.plugin.component.extension.ComponentExtension
import com.plugin.component.extension.module.ProjectInfo
import com.plugin.component.utils.ProjectUtil
import org.gradle.api.Project

class DebugPlugin implements BasePlugin {

    private ComponentExtension componentExtension;

    @Override
    void initExtension(ComponentExtension componentExtension) {
        this.componentExtension = componentExtension
    }

    @Override
    void evaluateChild(Project child) {

    }

    @Override
    void afterEvaluateChild(Project child) {
        ProjectInfo projectInfo = Runtimes.getProjectInfo(child.name)
        //调整debugModule结构
        if (ProjectUtil.isProjectSame(projectInfo.name, Runtimes.getDebugModuleName())) {
            Logger.buildOutput("")
            Logger.buildOutput(" =====> project[" + projectInfo.name + "] is debugModel,modifying DebugSets <=====")
            ProjectUtil.modifyDebugSets(projectInfo.project.rootProject, projectInfo)
            Logger.buildOutput(" =====> project[" + projectInfo.name + "] is debugModel,modifying DebugSets <=====")
            Logger.buildOutput("")
        }
    }

    @Override
    void evaluateRoot(Project root) {

    }

    @Override
    void afterEvaluateRoot(Project root) {

    }

    @Override
    void afterAllEvaluate() {

    }
}
