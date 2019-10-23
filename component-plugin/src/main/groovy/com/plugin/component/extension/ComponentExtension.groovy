package com.plugin.component.extension

import com.plugin.component.extension.option.sdk.CompileOptions
import com.plugin.component.extension.option.debug.DebugOption
import com.plugin.component.extension.option.sdk.SdkOption
import org.gradle.api.Project
import org.gradle.util.ConfigureUtil

/**
 * 插件配置
 * created by yummylau 2019/08/09
 */
class ComponentExtension {

    DebugOption debugOption                         //调试选项
    SdkOption sdkOption                             //sdk选项

    String includes = ""
    String excludes = ""
    Project project

    ComponentExtension(Project project) {
        this.project = project
        debugOption = new DebugOption(project)
        sdkOption = new SdkOption(project)
    }

    /**
     * 过滤或者包含功能说明
     * 如果只存在include，则插件只作用incilude
     * 如果只存在exclude，则插件默认作用的模块为（all modules - exclude）
     * 如果两者都存在，则取 include
     * 如果两则都不存在，则取 exclude 即所有
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
    void exclude(String modules) {
        this.excludes = modules
    }


    /**
     * sdk模块
     * @param closure
     */
    void componentSdks(Closure closure) {
        ConfigureUtil.configure(closure, sdkOption)
    }

    /**
     * 调试模块
     * @param closure
     */
    void debug(Closure closure) {
        ConfigureUtil.configure(closure, debugOption)
    }
}
