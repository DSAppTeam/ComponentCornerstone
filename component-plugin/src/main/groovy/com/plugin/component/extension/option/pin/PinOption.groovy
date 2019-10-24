package com.plugin.component.extension.option.pin

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.util.ConfigureUtil

class PinOption {

    Project project
    List<PinConfiguration> configurationList = new ArrayList<>()         //配置信息

    PinOption(Project project) {
        this.project = project
    }

    void configuration(Closure closure) {
        NamedDomainObjectContainer<PinConfiguration> configurations = project.container(PinConfiguration)
        ConfigureUtil.configure(closure, configurations)
        configurations.each {
            configurationList.add(it)
        }
    }

    @Override
    String toString() {
        StringBuilder stringBuilder = new StringBuilder("\n")
        stringBuilder.append("               ------------------------------------------------------------------" + "\n")
        stringBuilder.append("              | configuration = [ " +  "\n" )
        for(PinConfiguration configuration: configurationList){
            stringBuilder.append("              |       name = " + configuration.name + ", codeCheckEnabled = " + configuration.codeCheckEnabled  + ", mathPath = " + configuration.mainPath
                    + ", include = " + configuration.includePins.toString() + ", export = " + configuration.export.toString()  + "\n" )
        }
        stringBuilder.append("              | ] " +  "\n" )
        stringBuilder.append("               ------------------------------------------------------------------" + "\n")
        return stringBuilder.toString()
    }
}
