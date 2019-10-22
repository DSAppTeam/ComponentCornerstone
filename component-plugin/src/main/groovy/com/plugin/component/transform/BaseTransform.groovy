package com.plugin.component.transform


import com.quinn.hunter.transform.HunterTransform
import org.gradle.api.Project


class BaseTransform extends HunterTransform {

    BaseTransform(Project project) {
        super(project)
    }

    @Override
    boolean isIncremental() {
        return false
    }
}
