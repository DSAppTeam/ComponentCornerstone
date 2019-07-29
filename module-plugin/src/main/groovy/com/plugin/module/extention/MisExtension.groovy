package com.plugin.module.extention

import com.plugin.module.mis.extension.CompileOptions
import com.plugin.module.mis.extension.OnMisExtensionListener
import com.plugin.module.mis.extension.Publication
import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.util.ConfigureUtil

import javax.inject.Inject

class MisExtension {

    int compileSdkVersion                           //编译版本
    CompileOptions compileOptions                   //编译选项
    Action<? super RepositoryHandler> configure     //仓库配置

    Project childProject                            //子项目
    OnMisExtensionListener listener                 //发布监听器
    Map<String, Publication> publicationMap         //发布信息

    @Inject
    MisExtension() {
    }

    MisExtension(OnMisExtensionListener listener) {
        this.listener = listener
        this.publicationMap = new HashMap<>()
        compileOptions = new CompileOptions()
    }

    void compileSdkVersion(int version) {
        compileSdkVersion = version
    }

    void publications(Closure closure) {
        NamedDomainObjectContainer<Publication> publications = childProject.container(Publication)
        ConfigureUtil.configure(closure, publications)
        publications.each {
            listener.onPublicationAdded(childProject, it)
        }
    }

    void compileOptions(Closure closure) {
        ConfigureUtil.configure(closure, compileOptions)
    }

    void repositories(Action<? super RepositoryHandler> configure) {
        this.configure = configure
    }
}
