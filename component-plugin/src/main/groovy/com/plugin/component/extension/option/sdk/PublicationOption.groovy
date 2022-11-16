package com.plugin.component.extension.option.sdk


import com.plugin.component.extension.module.SourceSet
import org.gradle.util.ConfigureUtil

/**
 * jar 信息，兼容 sdk / impl
 * created by yummylau 2019/08/09
 */
class PublicationOption {

    String name                         //模块名
    String scrName = "main"             //资源目录名
    String sourceSetName                //sourceSet名
    File buildDir                       //构建目录
    File impDir                         //imp实现目录
    String project                      //模块project路径
    SourceSet sdkSourceSet              //sdk资源集，如果当前maven不可用，则需要记录源码及最后修改时间信息
    SourceSet impSourceSet              //imp资源集
    String versionNew                   //新版本
    boolean isSdk                       //是否时sdk
    boolean invalid                     //是否非法
    boolean hit                         //是否隐藏
    boolean useLocal                    //是否使用本地jar
    boolean forceLocal                  //强制使用本地打包
    boolean useUserSdkVersion           //使用用户指定Sdk版本
    boolean useUserImplVersion          //使用用户指定Impl版本
    boolean impNeedPack                 //实现aar是否需要重新打包
    boolean sdkNeedPublish = true       //SDK是否需要发布
    boolean impNeedPublish = true       //imp是否需要发布
    boolean useLocalImp                 //是否使用本地aar
    boolean online = false              //是否正式环境
    /**
     * option 用于配置sdk资源
     */
    String groupId                      //依赖分组id
    String artifactId                   //依赖id
    String sdkVersion                   //依赖版本
    String implVersion                   //impl版本
    String localProject                 //依赖版本
    Closure sourceFilter                //资源过滤
    PublicationDependenciesOption dependencies     //所持有的依赖信息


    PublicationOption(String name) {
        this.name = name
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

    void sdkVersion(String version) {
        this.sdkVersion = version
        this.useUserSdkVersion = true
    }

    void implVersion(String version) {
        this.implVersion = version
        this.useUserImplVersion = true
    }

    void forceUseLocal(boolean forceUseLocal) {
        this.forceLocal = forceUseLocal
        this.useLocal = true
    }

    void dependencies(Closure closure) {
        dependencies = new PublicationDependenciesOption()
        ConfigureUtil.configure(closure, dependencies)
    }

    void sourceFilter(Closure closure) {
        this.sourceFilter = closure
    }

    String getArtifactIdString() {
        return "${artifactId}${online ? '' : "-test"}"
    }

    @Override
    public String toString() {
        def sdkQuickVerify = ""
        if (sdkSourceSet != null) {
            sdkQuickVerify = sdkSourceSet.quickVerifyModified
        }
        def implQuickVerify = ""
        if (impSourceSet != null) {
            implQuickVerify = impSourceSet.quickVerifyModified
        }
        return "PublicationOption{" +
                "name='" + name + '\'' +
                ", scrName='" + scrName + '\'' +
                ", sourceSetName='" + sourceSetName + '\'' +
                ", buildDir=" + buildDir +
                ", impDir=" + impDir +
                ", project='" + project + '\'' +
                ", versionNew='" + versionNew + '\'' +
                ", isSdk=" + isSdk +
                ", invalid=" + invalid +
                ", hit=" + hit +
                ", useLocal=" + useLocal +
                ", impNeedPack=" + impNeedPack +
                ", groupId='" + groupId + '\'' +
                ", artifactId='" + artifactId + '\'' +
                ", sdkVersion='" + sdkVersion + '\'' +
                ", localProject='" + localProject + '\'' +
                ", sdkQuickVerify='" + sdkQuickVerify + '\'' +
                ", implQuickVerify='" + implQuickVerify + '\'' +
                '}';
    }
}
