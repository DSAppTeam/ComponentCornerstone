package com.plugin.component

import android.app.Application

/**
 * 基础组件实现接口
 * Email yummyl.lau@gmail.com
 * Created by yummylau on 2018/01/25.
 */
interface IComponent {

    fun attachComponent(application: Application)

    fun detachComponent()
}
