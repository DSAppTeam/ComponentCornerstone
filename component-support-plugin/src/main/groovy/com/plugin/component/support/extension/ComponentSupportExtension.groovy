package com.plugin.component.support.extension
/**
 * 插件配置
 * created by yummylau 2019/08/09
 */
class ComponentSupportExtension {

    public boolean methodCostEnable = true
    public String filterModule = ""

    /**
     * 编译sdk
     * @param version
     */
    void methodCostEnable(boolean methodCostEnable) {
        this.methodCostEnable = methodCostEnable
    }

    /**
     * 过滤哪些模块，格式为 ':library,:libraryKotlin' 或者 ':library,libraryKotlin' 不带 ":"
     * @param filterModule
     */
    void filterModule(String filterModule) {
        this.filterModule = filterModule
    }
}
