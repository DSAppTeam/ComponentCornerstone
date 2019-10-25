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
    void evaluateBeforeAndroidPlugin(Project project) {

    }

    @Override
    void afterEvaluateBeforeAndroidPlugin(Project project) {

    }

    @Override
    void evaluateAfterAndroidPlugin(Project project) {

    }

    @Override
    void afterEvaluateAfterAndroidPlugin(Project project) {

    }

    @Override
    void afterAllEvaluate(Project root) {

    }
}



