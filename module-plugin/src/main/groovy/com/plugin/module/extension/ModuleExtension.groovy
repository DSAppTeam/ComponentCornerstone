package com.plugin.module.extension

import com.plugin.module.extension.module.AloneConfiguration
import com.plugin.module.extension.module.CompileOptions
import com.plugin.module.listener.OnModuleExtensionListener
import com.plugin.module.extension.publication.Publication
import org.gradle.api.Action

import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.util.ConfigureUtil

class ModuleExtension {

    String mainModuleName
    int compileSdkVersion                           //编译版本
    CompileOptions compileOptions                   //编译选项
    Action<? super RepositoryHandler> configure     //仓库配置
    Project currentChildProject                     //子项目
    OnModuleExtensionListener listener              //发布监听器
    Map<String, Publication> publicationMap         //发布信息

    public AloneConfiguration aloneConfiguration
    public Publication serviceConfiguration


    void runalone(Action<AloneConfiguration> action) {
        action.execute(this.aloneConfiguration)
        listener.onAloneConfigAdded(currentChildProject, aloneConfiguration)
    }

    void service(Action<Publication> action) {
        action.execute(this.serviceConfiguration)
        listener.onPublicationAdded(currentChildProject, serviceConfiguration)
    }


    ModuleExtension(OnModuleExtensionListener listener) {
        this.listener = listener
        this.publicationMap = new HashMap<>()
        compileOptions = new CompileOptions()
        aloneConfiguration = new AloneConfiguration()
        serviceConfiguration = new Publication("library")
    }


    void compileSdkVersion(int version) {
        compileSdkVersion = version
    }

    void mainModuleName(String name) {
        mainModuleName = name
    }


    void compileOptions(Closure closure) {
        ConfigureUtil.configure(closure, compileOptions)
    }

    void repositories(Action<? super RepositoryHandler> configure) {
        this.configure = configure
    }
}
