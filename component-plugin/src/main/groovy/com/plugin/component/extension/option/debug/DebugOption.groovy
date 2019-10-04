package com.plugin.component.extension.option.debug

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.util.ConfigureUtil

/**
 * debug模块选项
 */
class DebugOption {

    Project project
    String targetModuleName
    //调试模块，随意在当前项目新建一个module用于调试即可
    String targetDebugName
    //申明调试模块运行时链接组件的资源，表明运行 debugModule 时使用目录资源
    List<DebugConfiguration> configurationList = new ArrayList<>()         //配置信息

    DebugOption(Project project) {
        this.project = project
    }

    void targetModuleName(String name) {
        targetModuleName = name
    }

    void targetDebugName(String name) {
        targetDebugName = name
    }

    void configuration(Closure closure) {
        NamedDomainObjectContainer<DebugConfiguration> configurations = project.container(DebugConfiguration)
        ConfigureUtil.configure(closure, configurations)
        configurations.each {
            configurationList.add(it)
        }
    }

    boolean hasConfigurations() {
        return !configurationList.isEmpty()
    }


    @Override
    public String toString() {
        return "DebugOption{" +
                "targetModuleName='" + targetModuleName + '\'' +
                ", targetDebugName='" + targetDebugName + '\'' +
                ", configurationList=" + configurationList +
                '}';
    }
}