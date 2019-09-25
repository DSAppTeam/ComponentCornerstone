package com.plugin.librarykotlin2

import android.app.Application
import android.util.Log
import com.plugin.component.ComponentManager

import com.plugin.component.IComponent
import com.plugin.component.SdkManager
import com.plugin.component.anno.AutoInjectComponent
import com.plugin.library.ISdk


@AutoInjectComponent(impl = [ProvideFromLibraryKotlin2Impl::class])
class Kotlin2Component : IComponent {

    companion object {
        lateinit var sdk: ISdk
    }

    override fun attachComponent(application: Application) {
        Log.d("component-plugin", "KotlinComponent#attachComponent")
        ComponentManager.init(application)
        sdk = SdkManager.getSdk(ISdk::class.java)!!
    }

    override fun detachComponent() {
        Log.d("component-plugin", "KotlinComponent#detachComponent")
    }
}