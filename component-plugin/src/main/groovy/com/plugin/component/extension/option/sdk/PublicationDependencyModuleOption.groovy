package com.plugin.component.extension.option.sdk

/**
 * 依赖实现处理
 */
class PublicationDependencyModuleOption {
    String path
    Map<String, String> exclude

    void path(String value) {
        this.path = value
    }

    void exclude(Map<String, String> value) {
        this.exclude = value
    }
}