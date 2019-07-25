package com.plugin.module.mis.extension

import com.plugin.module.mis.SourceSet
import org.gradle.util.ConfigureUtil

/**
 * 发布实体封装
 */
class Publication {

    String name
    String sourceSetName
    File buildDir                       //{peoject}/build/mis

    String project
    SourceSet misSourceSet              //资源集

    String groupId                      //依赖分组id
    String artifactId                   //依赖id
    String version                      //依赖版本

    String versionNew

    Dependencies dependencies           //所持有的依赖信息

    Closure sourceFilter                //资源过滤block

    boolean invalid                     //是否合法
    boolean hit                         //是否隐藏
    boolean useLocal                    //是否使用本地jar

    Publication(final String name) {
        this.name = name
    }

    void groupId(String groupId) {
        this.groupId = groupId
    }

    void artifactId(String artifactId) {
        this.artifactId = artifactId
    }

    void version(String version) {
        this.version = version
    }

    void dependencies(Closure closure) {
        dependencies = new Dependencies()
        ConfigureUtil.configure(closure, dependencies)
    }

    void sourceFilter(Closure closure) {
        this.sourceFilter = closure
    }
}
