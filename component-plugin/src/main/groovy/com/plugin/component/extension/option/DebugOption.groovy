package com.plugin.component.extension.option

/**
 * 单独运行配置信息
 * created by yummylau 2019/08/09
 */
class DebugOption {

    String name
    String applicationName

    boolean enable = false

    DebugOption(String name) {
        this.name = name
    }

    void applicationName(String applicationName) {
        this.applicationName = applicationName
    }

    void enable(boolean enable) {
        this.enable = enable
    }
}