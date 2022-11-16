package com.plugin.component.extension

import com.plugin.component.extension.option.pin.PinOption
import com.plugin.component.extension.option.debug.DebugOption
import com.plugin.component.extension.option.sdk.SdkOption
import com.plugin.component.utils.ProjectUtil
import org.gradle.api.Project
import org.gradle.util.ConfigureUtil

/**
 * 插件配置
 * created by yummylau 2019/08/09
 */
class ComponentExtension {

    DebugOption debugOption                         //调试选项
    SdkOption sdkOption                             //sdk选项
    PinOption pinOption                             //pin选项

    Set<String> includeModules = new HashSet<>()
    Set<String> excludeModules = new HashSet<>()
    Set<String> validModules
    Project project

    String appModule //app模块

    ComponentExtension(Project project) {
        this.project = project
        debugOption = new DebugOption(project)
        sdkOption = new SdkOption(project)
        pinOption = new PinOption(project)
    }

    /**
     * 过滤或者包含功能说明
     * 如果只存在include，则插件只作用incilude
     * 如果只存在exclude，则插件默认作用的模块为（all modules - exclude）
     * 如果两者都存在，则取 include
     * 如果两则都不存在，则取 exclude 即所有
     */

    /**
     * 过滤哪些模块，格式为 ':library' 或者 'library' ，多个使用 "," 隔开  比如 "library",":libraryKotlin"
     * @param modules
     */
    void include(String... modules) {
        if(modules != null && modules.size() > 0){
            for(String module: modules){
                includeModules.add(ProjectUtil.getProjectName(module))
            }
        }
    }

    /**
     * 包含哪些模块，格式为 ':library' 或者 'library' ，多个使用 "," 隔开  比如 "library",":libraryKotlin"
     * @param modules
     */
    void exclude(String... modules) {
        if(modules != null && modules.size() > 0){
            for(String module: modules){
                excludeModules.add(ProjectUtil.getProjectName(module))
            }
        }
    }

    /**
     * 配置app主模块名称
     * @param moduleName
     */
    void app(String... modules) {
        if(modules != null && modules.size() > 0){
            this.appModule = modules[0]
        }

    }

    /**
     * sdk模块
     * @param closure
     */
    void sdk(Closure closure) {
        ConfigureUtil.configure(closure, sdkOption)
    }

    /**
     * sdk模块
     * @param closure
     */
    void pin(Closure closure) {
        ConfigureUtil.configure(closure, pinOption)
    }

    /**
     * 调试模块
     * @param closure
     */
    void debug(Closure closure) {
        ConfigureUtil.configure(closure, debugOption)
    }

    boolean shouldApplyComponentPlugin(Project project) {
        getValidModules().contains(ProjectUtil.getProjectName(project))
    }

    Set<String> getValidComponents(Project root, Set<String> includeModules, Set<String> excludeModules, boolean includeModel) {
        Set<String> result = new HashSet<>()
        root.allprojects.each {
            if (includeModel) {
                if (includeModules.contains(ProjectUtil.getProjectName(it))) {
                    result.add(it.name)
                }
            } else {
                if (!excludeModules.contains(ProjectUtil.getProjectName(it))) {
                    result.add(it.name)
                }
            }
        }
        return result
    }

    private Set<String> getValidModules(){
        if(validModules == null){
            validModules = new HashSet<>()
            validModules =  getValidComponents(project, includeModules, excludeModules, !includeModules.isEmpty())
        }
        return validModules
    }

    @Override
    String toString() {
        StringBuilder stringBuilder = new StringBuilder("\n")
        stringBuilder.append("               ------------------------------------------------------------------" + "\n")
        stringBuilder.append("              | appModule = " + appModule.toString() +  "\n")
        stringBuilder.append("              | include = " + includeModules.toList().toString() +  "\n")
        stringBuilder.append("              | exclude = " + excludeModules.toList().toString() +  "\n")
        stringBuilder.append("              | Select by " + (!includeModules.isEmpty() ? "includeModel" : "excludeModel") +  "\n")
        stringBuilder.append("              | 插件作用模块 =  " + getValidModules().toList().toString() +  "\n")
        stringBuilder.append("               ------------------------------------------------------------------" + "\n")
        return stringBuilder.toString()
    }
}
