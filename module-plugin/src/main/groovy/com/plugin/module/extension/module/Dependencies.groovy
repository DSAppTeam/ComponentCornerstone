package com.plugin.module.extension.module

/**
 * 依赖记录
 */
class Dependencies {

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
