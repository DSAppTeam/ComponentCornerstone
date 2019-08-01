package com.plugin.module.listener

import com.plugin.module.extension.module.AloneConfiguration
import com.plugin.module.extension.publication.Publication
import org.gradle.api.Project

interface OnModuleExtensionListener {
    void onPublicationAdded(Project childProject, Publication publication)
    
    void onAloneConfigAdded(Project childProject,AloneConfiguration aloneConfiguration)
}