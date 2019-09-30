package com.plugin.component.extension

import com.plugin.component.extension.option.CompileOption
import com.plugin.component.extension.option.PublicationOption
import com.plugin.component.listener.OnModuleExtensionListener
import com.plugin.component.utils.ProjectUtil
import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.util.ConfigureUtil

/**
 * 插件配置
 * created by yummylau 2019/08/09
 */
class ComponentExtension {

    String mainModuleName
    String debugModuleName
    String debugComponentName
    int compileSdkVersion                           //编译版本
    CompileOption compileOptions                    //编译选项
    Action<? super RepositoryHandler> configure     //仓库配置
    OnModuleExtensionListener listener              //发布监听器
    String includes = ""
    String excludes = ""
    Project project

    ComponentExtension(Project project, OnModuleExtensionListener listener) {
        this.listener = listener
        this.project = project
        compileOptions = new CompileOption()
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
     * 编译sdk
     * @param version
     */
    void compileSdkVersion(int version) {
        compileSdkVersion = version
    }

    /**
     * 工程主项目名称
     * @param name
     */
    void mainModuleName(String name) {
        mainModuleName = name
    }

    /**
     * 调试项目名称
     * @param name
     */
    void debugModuleName(String name) {
        debugModuleName = name
    }

    /**
     * 调试组件名称
     * @param name
     */
    void debugComponentName(String name) {
        debugComponentName = name
    }

    /**
     * 配置选项
     * @param closure
     */
    void compileOptions(Closure closure) {
        ConfigureUtil.configure(closure, compileOptions)
    }

    /**
     * 仓库配置
     * @param configure
     */
    void repositories(Action<? super RepositoryHandler> configure) {
        this.configure = configure
    }

    /**
     * 申明组件sdk
     * @param closure
     */
    void componentSdks(Closure closure) {
        NamedDomainObjectContainer<PublicationOption> publications = project.container(PublicationOption)
        ConfigureUtil.configure(closure, publications)
        publications.each {
            it.isSdk = true
            it.name = ProjectUtil.getProjectName(it.name)
            listener.onPublicationOptionAdd(it)
        }
    }

    /**
     * 未开放
     * @param closure
     */
    void componentImpls(Closure closure) {
        NamedDomainObjectContainer<PublicationOption> publications = project.container(PublicationOption)
        ConfigureUtil.configure(closure, publications)
        publications.each {
            it.isSdk = false
            it.name = ProjectUtil.getProjectName(it.name)
            listener.onPublicationOptionAdd(it)
        }
    }
}
