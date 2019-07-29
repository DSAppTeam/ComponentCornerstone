package com.plugin.module.extention

import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory


class ModuleExtension {

    final MisExtension mis

    final RunaloneExtension runalone

    @javax.inject.Inject
    ModuleExtension(ObjectFactory objectFactory) {
        // Create a Person instance
        mis = objectFactory.newInstance(MisExtension)
        runalone = objectFactory.newInstance(RunaloneExtension)
    }

    void mis(Action<? super MisExtension> action) {
        action.execute(mis)
    }

    void runalone(Action<? super RunaloneExtension> action) {
        action.execute(runalone)
    }

}
