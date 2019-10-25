package com.plugin.component.extension.option.pin

import com.plugin.component.extension.module.Digraph
import com.plugin.component.extension.module.PinInfo
import com.plugin.component.extension.module.ProductFlavorInfo
import com.plugin.component.utils.PinUtils
import org.gradle.api.GradleException
import org.gradle.api.Project


class PinConfiguration {

    String name
    boolean codeCheckEnabled = true
    Set<String> include = new HashSet<>()
    Set<String> export = new HashSet<>()
    String mainPath

    Project project
    PinInfo mainPin
    Map<String, PinInfo> includePins
    Map<String, String> exportPins
    Digraph<String> dependencyGraph
    ProductFlavorInfo productFlavorInfo

    PinConfiguration(String name) {
        this.name = name
    }

    void initMainPin(Project project) {
        this.project = project
        this.includePins = new HashMap<>()
        this.exportPins = new HashMap<>()
        dependencyGraph = new Digraph<String>()
        PinInfo mainPin = PinUtils.buildPin(project, ':main')
        if (mainPin != null) {
            setMainPin(mainPin)
        }
    }

    void initProductFlavor() {
        productFlavorInfo = new ProductFlavorInfo(project)
        PinUtils.clearOriginSourceSet(project, productFlavorInfo)

        for (String name : include) {
            PinInfo pin = PinUtils.buildPin(project, name)
            if (pin == null) {
                throw new GradleException("PinInfo with path '${name}' could not be found in ${project.getDisplayName()}.")
            }
            addIncludePin(pin)
            PinUtils.addMicroModuleSourceSet(project, pin, productFlavorInfo)
        }

        for (String name : export) {
            PinInfo pin = PinUtils.buildPin(project, name)
            if (pin == null) {
                throw new GradleException("PinInfo with path '${name}' could not be found in ${project.getDisplayName()}.")
            }
            addExportPin(pin)
            PinUtils.addMicroModuleSourceSet(project, pin, productFlavorInfo)
        }

        if (mainPath != null && !mainPath.isEmpty()) {
            PinInfo pin = PinUtils.buildPin(project, mainPath)
            setMainPin(pin)
            PinUtils.addMicroModuleSourceSet(project, pin, productFlavorInfo)
        }
    }

    void setMainPin(PinInfo pin) {
        if (pin == null) {
            throw new GradleException("main PinInfo cannot be null.")
        }
        this.mainPin = pin
        addIncludePin(pin)
    }

    void addIncludePin(PinInfo pin) {
        includePins.put(pin.name, pin)
    }

    void addExportPin(PinInfo pin) {
        exportPins.put(pin.name, null)
    }

    PinInfo getIncludePin(String name) {
        return includePins.get(name)
    }

    void setPinDependency(String target, String name) {
        PinInfo pin = getIncludePin(name)
        if (pin == null) {
            if (PinUtils.buildPin(project, name) != null) {
                throw new GradleException("PinInfo '${target}' name MicroModle '${name}', but its not included.")
            } else {
                throw new GradleException("PinInfo with path '${path}' could not be found in ${project.getDisplayName()}.")
            }
        }
        dependencyGraph.add(target, name)
        if (!dependencyGraph.isDag()) {
            throw new GradleException("Circular name between PinInfo '${target}' and '${name}'.")
        }
    }

    boolean hasDependency(String target, String name) {
        Map<String, Integer> bfsDistance = dependencyGraph.bfsDistance(target)
        for (String key : bfsDistance.keySet()) {
            if (key == name) {
                return bfsDistance.get(key) != null
            }
        }
        return false
    }


    void codeCheckEnabled(boolean enabled) {
        this.codeCheckEnabled = enabled
    }

    void export(String... pinPaths) {
        int size = pinPaths.size()
        for (int i = 0; i < size; i++) {
            export.add(pinPaths[i])
        }
    }

    void include(String... pinPaths) {
        int size = pinPaths.size()
        for (int i = 0; i < size; i++) {
            include.add(pinPaths[i])
        }
    }

    void includeMain(String pinPath) {
        this.mainPath = pinPath
    }

}
