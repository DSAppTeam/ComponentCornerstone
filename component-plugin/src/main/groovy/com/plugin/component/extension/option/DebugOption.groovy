package com.plugin.component.extension.option

/**
 * 单独运行配置信息
 * created by yummylau 2019/08/09
 */
class DebugOption {

    String applicationName

    boolean enable = true

    void applicationName(String applicationName) {
        this.applicationName = applicationName
    }
}