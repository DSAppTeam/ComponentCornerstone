package com.plugin.component.plugin

import com.plugin.component.extension.ComponentExtension
import org.gradle.api.Project

abstract interface BasePlugin {

    void initExtension(ComponentExtension componentExtension)

    void evaluateChild(Project child)

    void afterEvaluateChild(Project child)

    void evaluateRoot(Project root)

    void afterEvaluateRoot(Project root)

    void afterAllEvaluate()
}