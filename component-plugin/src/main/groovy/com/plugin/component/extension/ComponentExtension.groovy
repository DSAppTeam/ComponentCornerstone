package com.plugin.component.extension

import com.plugin.component.extension.option.CompileOption
import com.plugin.component.extension.option.PublicationOption
import com.plugin.component.extension.option.RunAloneOption
import com.plugin.component.listener.OnModuleExtensionListener
import org.gradle.api.Action

import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.util.ConfigureUtil

/**
 * 插件配置
 * created by yummylau 2019/08/09
 */
class ComponentExtension {

    String mainModuleName
    int compileSdkVersion                           //编译版本
    CompileOption compileOptions                    //编译选项
    Action<? super RepositoryHandler> configure     //仓库配置
    OnModuleExtensionListener listener              //发布监听器

    public RunAloneOption runAloneOption
    public PublicationOption publicationOption
    public PublicationOption implPublicationOption

    Project currentChildProject                      //子项目

    ComponentExtension(OnModuleExtensionListener listener) {
        this.listener = listener
        compileOptions = new CompileOption()
        runAloneOption = new RunAloneOption()
        publicationOption = new PublicationOption()
        publicationOption.isSdk = true
        implPublicationOption = new PublicationOption()
        implPublicationOption.isSdk = false
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
     * 独立运行配置
     * @param action
     */
    void runalone(Action<RunAloneOption> action) {
        action.execute(this.runAloneOption)
        listener.onRunAloneOptionAdded(currentChildProject, runAloneOption)
    }

    /**
     * sdk配置
     * @param action
     */
    void sdk(Action<PublicationOption> action) {
        action.execute(this.publicationOption)
        listener.onPublicationOptionAdded(currentChildProject, publicationOption)
    }

    /**
     * impl配置
     * @param action
     */
    void impl(Action<PublicationOption> action) {
        action.execute(this.implPublicationOption)
        listener.onPublicationOptionAdded(currentChildProject, publicationOption)
    }
}
