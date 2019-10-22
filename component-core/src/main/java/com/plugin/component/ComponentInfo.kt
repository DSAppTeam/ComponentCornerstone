package com.plugin.component

import android.app.Application
import android.util.ArrayMap

/**
 * 运行时完成收集
 */
class ComponentInfo {

    var component: Any

    private val sdkMap = ArrayMap<Class<*>, Any>()

    constructor(componentClass: Class<*>) {
        component = componentClass
    }

    constructor(impl: IComponent) {
        component = impl
    }

    fun isComponentReady(): Boolean {
        return component is IComponent
    }

    fun initComponent(application: Application): IComponent? {
        try {
            val component = (component as Class<*>).newInstance() as IComponent
            component.attachComponent(application)
            this.component = component
            return component
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
        } catch (e: InstantiationException) {
            e.printStackTrace()
        }
        return null
    }


    fun registerSdk(sdkClass: Class<*>, sdkImpl: Any) {
        sdkMap[sdkClass] = sdkImpl
    }

    fun unregisterSdk(sdkKey: Class<*>): Boolean {
        if (sdkMap.containsKey(sdkKey)) {
            sdkMap.remove(sdkKey)
            return true
        }
        return false
    }

    fun hasSdk(sdkKey: Class<*>): Boolean {
        return sdkMap.containsKey(sdkKey)
    }

    fun isSdkReady(sdkKey: Class<*>): Boolean {
        return sdkMap[sdkKey] != null && sdkMap[sdkKey] !is Class<*>
    }

    fun <T> initSdk(sdkKey: Class<*>): T? {
        try {
            val impl = (sdkMap[sdkKey] as Class<*>).newInstance() as T
            sdkMap[sdkKey] = impl
            return impl
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
        } catch (e: InstantiationException) {
            e.printStackTrace()
        }
        return null
    }

    fun <T> getImpl(sdkKey: Class<*>): T? {
        return sdkMap[sdkKey] as T
    }
}
