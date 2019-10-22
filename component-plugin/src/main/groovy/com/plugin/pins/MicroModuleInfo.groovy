package com.plugin.pins

import org.gradle.api.GradleException
import org.gradle.api.Project

class MicroModuleInfo {

    Project project
    MicroModule mainMicroModule
    Map<String, MicroModule> includeMicroModules
    Map<String, String> exportMicroModules

    Digraph<String> dependencyGraph

    MicroModuleInfo(Project project) {
        this.project = project
        this.includeMicroModules = new HashMap<>()
        this.exportMicroModules = new HashMap<>()
        dependencyGraph = new Digraph<String>()

        MicroModule microModule = Utils.buildMicroModule(project, ':main')
        if (microModule != null) {
            setMainMicroModule(microModule)
        }
    }

    void setMainMicroModule(MicroModule microModule) {
        if (microModule == null) {
            throw new GradleException("main MicroModule cannot be null.")
        }
        this.mainMicroModule = microModule
        addIncludeMicroModule(microModule)
    }

    void addIncludeMicroModule(MicroModule microModule) {
        includeMicroModules.put(microModule.name, microModule)
    }

    void addExportMicroModule(String name) {
        MicroModule microModule = Utils.buildMicroModule(project, name)
        if (microModule == null) {
            throw new GradleException("MicroModule with path '${name}' could not be found in ${project.getDisplayName()}.")
        }
        exportMicroModules.put(name, null)
    }

    MicroModule getMicroModule(String name) {
        return includeMicroModules.get(name)
    }

    void setMicroModuleDependency(String target, String dependency) {
        MicroModule dependencyMicroModule = getMicroModule(dependency)
        if(dependencyMicroModule == null) {
            if(Utils.buildMicroModule(project, dependency) != null) {
                throw new GradleException("MicroModule '${target}' dependency MicroModle '${dependency}', but its not included.")
            } else {
                throw new GradleException("MicroModule with path '${path}' could not be found in ${project.getDisplayName()}.")
            }
        }

        dependencyGraph.add(target, dependency)
        if(!dependencyGraph.isDag()) {
            throw new GradleException("Circular dependency between MicroModule '${target}' and '${dependency}'.")
        }
    }

    boolean hasDependency(String target, String dependency) {
        Map<String, Integer> bfsDistance = dependencyGraph.bfsDistance(target)
        for(String key: bfsDistance.keySet()) {
            if(key == dependency) {
                return bfsDistance.get(key) != null
            }
        }
        return false
    }

}