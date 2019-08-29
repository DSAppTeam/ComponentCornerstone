package com.plugin.component.support.extension
/**
 * 插件配置
 * created by yummylau 2019/08/09
 */
class ComponentSupportExtension {

    public boolean methodCostEnable = true
    public String includes = ""
    public String excludes = ""

    /**
     * 编译sdk
     * @param version
     */
    void methodCostEnable(boolean methodCostEnable) {
        this.methodCostEnable = methodCostEnable
    }

    /**
     * 过滤或者包含功能说明
     * 如果只存在include，则插件只作用incilude
     * 如果只存在exclude，则插件默认作用的模块为（all modules - exclude）
     * 如果两者都存在，则只取 include 
     */

    /**
     * 过滤哪些模块，格式为 ':library' 或者 'library' ，多个使用 "," 隔开  比如 "library,:libraryKotlin"
     * @param modules
     */
    void include(String modules) {
        this.includes = modules
    }

    /**
     * 包含哪些模块，格式为 ':library' 或者 'library' ，多个使用 "," 隔开  比如 "library,:libraryKotlin"
     * @param modules
     */
    void exclude(String modules){
        this.excludes = modules
    }
}
