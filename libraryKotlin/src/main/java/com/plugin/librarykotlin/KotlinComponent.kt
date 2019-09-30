package com.plugin.librarykotlin

import android.app.Application
import android.util.Log
import com.plugin.component.ComponentManager

import com.plugin.component.IComponent
import com.plugin.component.SdkManager
import com.plugin.component.anno.AutoInjectComponent
import com.plugin.librarykotlin2.IProvideFromLibraryKotlin2


@AutoInjectComponent(impl = [ProvideFromKotlinImpl::class])
class KotlinComponent : IComponent {

    override fun attachComponent(application: Application) {
        Log.d("component-plugin", "KotlinComponent#attachComponent")
    }

    override fun detachComponent() {
        Log.d("component-plugin", "KotlinComponent#detachComponent")
    }
}
