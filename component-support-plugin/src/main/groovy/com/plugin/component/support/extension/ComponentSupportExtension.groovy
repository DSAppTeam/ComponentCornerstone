package com.plugin.component.extension
/**
 * 插件配置
 * created by yummylau 2019/08/09
 */
class ComponentSupportExtension {

    public boolean openMethodCost = true

    /**
     * 编译sdk
     * @param version
     */
    void openMethodCost(boolean openMethodCost) {
        this.openMethodCost = openMethodCost
    }
}
