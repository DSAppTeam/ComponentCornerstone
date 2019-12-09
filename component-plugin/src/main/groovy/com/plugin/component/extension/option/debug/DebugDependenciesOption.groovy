package com.plugin.component.extension.option.debug

/**
 * debug 组件的以来
 */
class DebugDependenciesOption {

    List<Object> implementation = new ArrayList<>()

    void implementation(Object value) {
        implementation.add(value)
    }

}
