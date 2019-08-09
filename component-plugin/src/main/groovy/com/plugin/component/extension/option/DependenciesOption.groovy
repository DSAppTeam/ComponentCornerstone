package com.plugin.component.extension.option

/**
 * 依赖记录
 * created by yummylau 2019/08/09
 */
class DependenciesOption {

    List<Object> implementation
    List<Object> compileOnly

    void implementation(Object value) {
        if (implementation == null) {
            implementation = new ArrayList<>()
        }
        implementation.add(value)
    }

    void compileOnly(Object value) {
        if (compileOnly == null) {
            compileOnly = new ArrayList<>()
        }
        compileOnly.add(value)
    }
}
