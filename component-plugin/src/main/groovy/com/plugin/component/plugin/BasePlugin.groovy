package com.plugin.component.plugin

import com.plugin.component.extension.ComponentExtension
import org.gradle.api.Project

class BasePlugin extends AbsPlugin {

    ComponentExtension componentExtension

    @Override
    void initExtension(ComponentExtension componentExtension) {
        this.componentExtension = componentExtension
    }

    @Override
    void evaluate(Project project, boolean isRoot) {

    }

    @Override
    void afterEvaluate(Project project, boolean isRoot) {

    }

    @Override
    void afterAllEvaluate() {

    }
}
