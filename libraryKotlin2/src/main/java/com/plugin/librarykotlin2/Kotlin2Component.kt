package com.plugin.librarykotlin2

import android.app.Application
import android.util.Log
import com.plugin.component.ComponentManager

import com.plugin.component.IComponent
import com.plugin.component.anno.AutoInjectComponent


@AutoInjectComponent(impl = [ProvideFromLibraryKotlin2Impl::class])
class Kotlin2Component : IComponent {

    override fun attachComponent(application: Application) {
        Log.d("component-plugin", "KotlinComponent#attachComponent")
    }

    override fun detachComponent() {
        Log.d("component-plugin", "KotlinComponent#detachComponent")
    }
}