package com.plugin.component.extension.option.debug

import org.gradle.util.ConfigureUtil

/**
 * debug 组件的以来
 */
class DebugConfiguration {

    String name                              //测试模块src目录名
    DebugDependenciesOption dependencies     //所持有的依赖信息

    DebugConfiguration(String name) {
        this.name = name
    }

    void dependencies(Closure closure) {
        dependencies = new DebugDependenciesOption()
        ConfigureUtil.configure(closure, dependencies)
    }
}
