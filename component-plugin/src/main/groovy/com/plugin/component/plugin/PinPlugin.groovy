package com.plugin.component.plugin

import com.plugin.component.extension.ComponentExtension
import org.gradle.api.Project

class PinPlugin implements BasePlugin {

    private final static String NORMAL = 'normal'
    private final static String ASSEMBLE_OR_GENERATE = 'assemble_or_generate'

    private final static String APPLY_NORMAL_MICRO_MODULE_SCRIPT = 'apply_normal_micro_module_script'
    private final static String APPLY_INCLUDE_MICRO_MODULE_SCRIPT = 'apply_include_micro_module_script'
    private final static String APPLY_EXPORT_MICRO_MODULE_SCRIPT = 'apply_export_micro_module_script'

    @Override
    void initExtension(ComponentExtension componentExtension) {

    }

    @Override
    void evaluateChild(Project child) {

    }

    @Override
    void afterEvaluateChild(Project child) {

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
