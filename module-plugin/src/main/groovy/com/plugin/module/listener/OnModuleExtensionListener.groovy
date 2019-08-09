package com.plugin.module.listener

import com.plugin.module.extension.option.RunAloneOption
import com.plugin.module.extension.option.PublicationOption
import org.gradle.api.Project

interface OnModuleExtensionListener {
    void onPublicationAdded(Project childProject, PublicationOption publication)
    
    void onAloneConfigAdded(Project childProject, RunAloneOption aloneConfiguration)
}