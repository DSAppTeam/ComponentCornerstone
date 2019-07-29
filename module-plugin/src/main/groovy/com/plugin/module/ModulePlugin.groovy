package com.plugin.module

import com.plugin.module.alone.AloneExtension
import com.plugin.module.alone.AlonePlugin
import org.gradle.api.Plugin
import org.gradle.api.Project

class ModulePlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {

        new AlonePlugin().apply(project)
    }

}
