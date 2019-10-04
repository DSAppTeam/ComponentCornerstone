package com.plugin.component.extension.option.debug

/**
 * debug 组件的以来
 */
class DebugDependenciesOption {

    List<Object> implementation

    void implementation(Object value) {
        if (implementation == null) {
            implementation = new ArrayList<>()
        }
        implementation.add(value)
    }

}
