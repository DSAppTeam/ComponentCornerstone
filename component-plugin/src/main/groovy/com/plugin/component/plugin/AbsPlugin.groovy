package com.plugin.component.plugin

import com.plugin.component.extension.ComponentExtension
import org.gradle.api.Project

abstract class AbsPlugin {

    abstract void initExtension(ComponentExtension componentExtension)

    abstract void evaluate(Project project, boolean isRoot)

    abstract void afterEvaluate(Project project, boolean isRoot)

    abstract void afterAllEvaluate()
}