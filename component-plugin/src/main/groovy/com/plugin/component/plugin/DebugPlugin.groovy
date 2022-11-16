package com.plugin.component.plugin

import com.plugin.component.log.Logger
import com.plugin.component.Runtimes
import com.plugin.component.extension.module.ProjectInfo
import com.plugin.component.utils.ProjectUtil
import org.gradle.api.Project

class DebugPlugin extends BasePlugin {

    @Override
    void afterEvaluateAfterAndroidPlugin(Project project, Project androidProject) {
        ProjectInfo projectInfo = Runtimes.getProjectInfo(project.name)
        if (projectInfo != null && ProjectUtil.isProjectSame(projectInfo.name, Runtimes.getDebugModuleName())) {
            ProjectUtil.modifyDebugSets(projectInfo.project.rootProject, projectInfo)
        }
    }
}
