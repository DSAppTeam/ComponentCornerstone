package com.plugin.component.plugin

import com.plugin.component.extension.ComponentExtension
import org.gradle.api.Project

abstract class AbsPlugin {

    abstract void initExtension(ComponentExtension componentExtension)

    abstract void evaluateBeforeAndroidPlugin(Project project)

    abstract void afterEvaluateBeforeAndroidPlugin(Project project)

    abstract void evaluateAfterAndroidPlugin(Project project)

    abstract void afterEvaluateAfterAndroidPlugin(Project project,Project androidProject)

    abstract void afterAllEvaluate(Project root)
}