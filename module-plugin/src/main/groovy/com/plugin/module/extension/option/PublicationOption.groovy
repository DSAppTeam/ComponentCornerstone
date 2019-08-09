package com.plugin.module.extension.option

import com.plugin.module.extension.module.SourceSet
import org.gradle.util.ConfigureUtil

class PublicationOption {

    String name = "main"
    String sourceSetName
    File buildDir                       //{peoject}/build/mis

    String project                      //模块project路径
    SourceSet misSourceSet              //资源集，如果当前maven不可用，则需要记录源码及最后修改时间信息

    String groupId                      //依赖分组id
    String artifactId                   //依赖id
    String version                      //依赖版本
    String localProject                 //依赖版本

    String versionNew

    DependenciesOption dependencies           //所持有的依赖信息

    Closure sourceFilter                //资源过滤block

    boolean invalid                     //是否非法
    boolean hit                         //是否隐藏
    boolean useLocal                    //是否使用本地jar


    PublicationOption() {
    }


    void groupId(String groupId) {
        this.groupId = groupId
    }

    void localProject(String name) {
        this.localProject = name
    }

    void artifactId(String artifactId) {
        this.artifactId = artifactId
    }

    void version(String version) {
        this.version = version
    }

    void dependencies(Closure closure) {
        dependencies = new DependenciesOption()
        ConfigureUtil.configure(closure, dependencies)
    }

    void sourceFilter(Closure closure) {
        this.sourceFilter = closure
    }
}
