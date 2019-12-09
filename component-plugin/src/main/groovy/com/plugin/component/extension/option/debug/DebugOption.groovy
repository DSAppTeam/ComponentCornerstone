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
        this.targetModuleName = ""
        this.targetDebugName = "defaultDebugName"
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
    String toString() {
        StringBuilder stringBuilder = new StringBuilder("\n")
        stringBuilder.append("               ------------------------------------------------------------------" + "\n")
        stringBuilder.append("              | targetModuleName =  " +  targetModuleName + "\n" )
        stringBuilder.append("              | targetDebugName =  " +  targetDebugName + "\n" )
        stringBuilder.append("              | configuration = [ " +  "\n" )
        for(DebugConfiguration configuration: configurationList){
            stringBuilder.append("              |       name = " + configuration.name +  ", dependencies = " + configuration.dependencies.implementation.toString()  + "\n" )
        }
        stringBuilder.append("              | ] " +  "\n" )
        stringBuilder.append("               ------------------------------------------------------------------" + "\n")
        return stringBuilder.toString()
    }
}