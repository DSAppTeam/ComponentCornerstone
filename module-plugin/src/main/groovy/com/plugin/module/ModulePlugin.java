package com.plugin.module;

import com.plugin.module.alone.AlonePlugin;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class ModulePlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        new AlonePlugin().apply(project);
    }
}
