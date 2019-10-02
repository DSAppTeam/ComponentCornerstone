package com.plugin.component.listener

import com.plugin.component.extension.option.DebugOption
import com.plugin.component.extension.option.PublicationOption

/**
 * 监听配置读取
 * created by yummylau 2019/08/09
 */
interface OnModuleExtensionListener {

    void onPublicationOptionAdd(PublicationOption publicationOption)

    void onDebugOptionAdd(DebugOption debugOption)
}