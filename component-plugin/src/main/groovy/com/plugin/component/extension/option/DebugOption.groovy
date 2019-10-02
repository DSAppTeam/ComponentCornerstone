package com.plugin.component.extension.option

import org.gradle.util.ConfigureUtil

/**
 * debug模块选项
 */
class DebugOption{

    String name                              //测试模块src目录名
    DebugDependenciesOption dependencies     //所持有的依赖信息

    DebugOption(String name) {
        this.name = name
    }

    void dependencies(Closure closure) {
        dependencies = new DebugDependenciesOption()
        ConfigureUtil.configure(closure, dependencies)
    }

}