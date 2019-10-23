package com.plugin.component.extension.option.pin

import com.plugin.component.extension.module.MicroModule
import org.gradle.api.GradleException
import org.gradle.api.Project

class DefaultMicroModuleExtension implements MicroModuleExtension {

    Project project
    OnMicroModuleListener onMicroModuleListener

    boolean codeCheckEnabled = true

    DefaultMicroModuleExtension(Project project) {
        this.project = project
    }

    @Override
    void codeCheckEnabled(boolean enabled) {
        this.codeCheckEnabled = enabled
    }

    @Override
    void export(String... microModulePaths) {
        if(onMicroModuleListener == null) return

        onMicroModuleListener.addExportMicroModule(microModulePaths)
    }

    @Override
    void include(String... microModulePaths) {
        if(onMicroModuleListener == null) return

        int size = microModulePaths.size()
        for (int i = 0; i < size; i++) {
            MicroModule microModule = Utils.buildMicroModule(project, microModulePaths[i])
            if (microModule == null) {
                throw new GradleException("MicroModule with path '${microModulePaths[i]}' could not be found in ${project.getDisplayName()}.")
            }
            onMicroModuleListener.addIncludeMicroModule(microModule, false)
        }
    }

    @Override
    void includeMain(String microModulePath) {
        if(onMicroModuleListener == null) return

        MicroModule microModule = Utils.buildMicroModule(project, microModulePath)
        if (microModule == null) {
            throw new GradleException("MicroModule with path '${microModulePath}' could not be found in ${project.getDisplayName()}.")
        }
        onMicroModuleListener.addIncludeMicroModule(microModule, true)
    }

}
