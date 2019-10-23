package com.plugin.component.extension.option.pin
import com.plugin.component.Logger

class PinConfiguration {

    String name
    boolean codeCheckEnabled = true
    Set<String> includePins = new HashSet<>()
    Set<String> export = new HashSet<>()
    String mainPath

    PinConfiguration(String name) {
        this.name = name
    }

    void codeCheckEnabled(boolean enabled) {
        this.codeCheckEnabled = enabled
    }

    void export(String... pinPaths) {
        int size = pinPaths.size()
        for (int i = 0; i < size; i++) {
            includePins.add(pinPaths[i])
        }
    }

    void include(String... pinPaths) {
        int size = pinPaths.size()
        for (int i = 0; i < size; i++) {
            export.add(pinPaths[i])
        }
    }

    void includeMain(String pinPath) {
        this.mainPath = pinPath
    }

}
