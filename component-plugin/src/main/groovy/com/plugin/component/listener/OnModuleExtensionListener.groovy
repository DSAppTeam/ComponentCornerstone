package com.plugin.component.listener

import com.plugin.component.extension.option.PublicationOption
import com.plugin.component.extension.option.DebugOption
import org.gradle.api.Project

/**
 * 监听配置读取
 * created by yummylau 2019/08/09
 */
interface OnModuleExtensionListener {

    void onPublicationOptionAdded(Project childProject, PublicationOption publication)
    
    void onDebugOptionAdded(Project childProject, DebugOption aloneConfiguration)
}