package com.plugin.module.mis.extension

import org.gradle.api.Project

/**
 * 监听发布
 */
interface OnMisExtensionListener {

    void onPublicationAdded(Project childProject, Publication publication)
}