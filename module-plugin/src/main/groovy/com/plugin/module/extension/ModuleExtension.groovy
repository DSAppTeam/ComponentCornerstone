package com.plugin.module.extension

import com.plugin.module.extension.module.AloneConfiguration
import com.plugin.module.extension.module.CompileOptions
import com.plugin.module.listener.OnModuleExtensionListener
import com.plugin.module.extension.publication.Publication
import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.util.ConfigureUtil

class ModuleExtension {

    int compileSdkVersion                           //编译版本
    CompileOptions compileOptions                   //编译选项
    Action<? super RepositoryHandler> configure     //仓库配置

    Project currentChildProject                      //子项目
    OnModuleExtensionListener listener               //发布监听器
    Map<String, Publication> publicationMap         //发布信息
    AloneConfiguration aloneConfiguration
    Map<String, AloneConfiguration> aloneRunMap           //配置信息

    ModuleExtension(OnModuleExtensionListener listener) {
        this.listener = listener
        this.publicationMap = new HashMap<>()
        this.aloneRunMap = new HashMap<>()
        compileOptions = new CompileOptions()
    }


    void runalone(Closure closure) {
        aloneConfiguration = new AloneConfiguration()
        ConfigureUtil.configure(closure, aloneConfiguration)
        aloneRunMap.put(currentChildProject.name, aloneConfiguration)
        listener.onAloneConfigAdded(currentChildProject, aloneConfiguration)
    }

    void compileSdkVersion(int version) {
        compileSdkVersion = version
    }

    void publications(Closure closure) {
        NamedDomainObjectContainer<Publication> publications = currentChildProject.container(Publication)
        ConfigureUtil.configure(closure, publications)
        publications.each {
            listener.onPublicationAdded(currentChildProject, it)
        }
    }

    void compileOptions(Closure closure) {
        ConfigureUtil.configure(closure, compileOptions)
    }

    void repositories(Action<? super RepositoryHandler> configure) {
        this.configure = configure
    }
}
