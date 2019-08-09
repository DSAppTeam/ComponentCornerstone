package com.plugin.component.listener

import com.plugin.component.extension.option.PublicationOption
import com.plugin.component.extension.option.RunAloneOption
import org.gradle.api.Project

interface OnModuleExtensionListener {

    void onPublicationOptionAdded(Project childProject, PublicationOption publication)
    
    void onRunAloneOptionAdded(Project childProject, RunAloneOption aloneConfiguration)
}