package com.plugin.module.listener

import com.plugin.module.publication.Publication
import org.gradle.api.Project

interface OnMisExtensionListener {
    void onPublicationAdded(Project childProject, Publication publication)
}