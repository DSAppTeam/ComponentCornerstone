package com.plugin.component

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 *   ./gradlew --no-daemon ComponentPlugin  -Dorg.gradle.debug=true
 *   ./gradlew --no-daemon [clean, :app:generateDebugSources, :library:generateDebugSources, :module_lib:generateDebugSources]  -Dorg.gradle.debug=true
 *    ./gradlew --no-daemon :app:assemble  -Dorg.gradle.debug=true
 *  组件插件入口
 *  created by yummylau 2019/08/09
 */
class ComponentPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        boolean isRoot = project == project.rootProject
        if (isRoot) {
            handleRootProject(project)
        } else {
            handleProject(project)
        }
    }
}
