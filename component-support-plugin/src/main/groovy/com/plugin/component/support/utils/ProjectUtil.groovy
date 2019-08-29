package com.plugin.component.support.utils

import org.gradle.api.Project

class ProjectUtil {

    static String getModuleName(Project project) {
        return project.path.replace(":", "")
    }

    static Set<String> getModuleName(String modules) {
        Set<String> result = new HashSet<>()
        if (modules != null && !modules.isEmpty()) {
            String[] strings = modules.split(",")
            if (strings != null && strings.length > 0) {
                for (String string : strings) {
                    if (string.startsWith(":")) {
                        result.add(string.substring(1, string.length()))
                    } else {
                        result.add(string)
                    }
                }
            }
        }
        return result
    }
}
