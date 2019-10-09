package com.plugin.component.extension.option.debug

import com.plugin.component.Constants
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
    public String toString() {
        return "DebugOption{" + '\n' +
                "       targetModuleName = '" + targetModuleName + "," + "\n" +
                "       targetDebugNam = '" + targetDebugName + "," + "\n" +
                "       configurationLis = [" + configurationListToString() + '\n' +
                "       ]} "
    }

    String configurationListToString() {
        StringBuffer stringBuffer = new StringBuffer("\n")
        for (DebugConfiguration debugConfiguration : configurationList) {
            stringBuffer.append("               { name=")
            stringBuffer.append(debugConfiguration.name)
            if (debugConfiguration.dependencies != null && !debugConfiguration.dependencies.implementation.isEmpty()) {
                stringBuffer.append(" dependencies=[")
                for (Object obj : debugConfiguration.dependencies.implementation) {
                    if (obj instanceof String) {
                        stringBuffer.append(" " + obj.toString().replace(Constants.DEBUG_COMPONENT_PRE, ""))
                    }
                }
                stringBuffer.append(" ]")
            }
            stringBuffer.append(" }\n")
        }
        return stringBuffer.toString()
    }
}