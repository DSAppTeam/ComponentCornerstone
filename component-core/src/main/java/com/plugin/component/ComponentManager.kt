package com.plugin.component

import android.app.Application
import android.util.ArrayMap
import android.util.Log

/**
 * 组件管理
 * 一个component 可对应多个 sdk
 * Email yummyl.lau@gmail.com
 * Created by yummylau on 2018/01/25.
 */
object ComponentManager {

    private const val TAG = "component-core"

    lateinit var application: Application
    lateinit var sComponentInfoArrayMap: ArrayMap<Class<*>, ComponentInfo>
    private var sInit = false

    /**
     * 注入代码入口
     *
     * @param application
     */
    @JvmStatic
    fun init(application: Application) {
        if (sInit) {
            return
        }
        ComponentManager.application = application
        sComponentInfoArrayMap = ArrayMap()
        sInit = true
        injectComponentWithSdk(application)
    }

    /**
     * 仅仅用于 asm 注入
     */
    private fun injectComponentWithSdk(application: Application){

    }

    private fun checkInit() {
        if (!sInit) {
            throw RuntimeException("you should invoke ComponentManager#init() before calling apis")
        }
    }

    fun findComponentInfoBySdk(sdkKey: Class<*>): ComponentInfo? {
        checkInit()
        val classes = sComponentInfoArrayMap.keys
        for (clazz in classes) {
            val componentInfo = sComponentInfoArrayMap[clazz]
            if (componentInfo!!.hasSdk(sdkKey)) {
                return componentInfo
            }
        }
        return null
    }


    /**
     * 是否存在注册的组件
     * @param componentImplObjectOrClass 支持 class 类型 或者 object 类型
     */
    fun hasRegister(componentObjectOrClass: Any): ComponentInfo? {
        checkInit()
        val isComponentImplClass = componentObjectOrClass is Class<*>
        val realComponentClass = if (isComponentImplClass) componentObjectOrClass as Class<*> else componentObjectOrClass.javaClass
        return sComponentInfoArrayMap[realComponentClass]
    }


    /**
     * 注册组件实现累必须实现 IComponent
     *
     * @param componentImplObjectOrClass 支持 class 类型 或者 object 类型
     *
     */
    @JvmStatic
    fun registerComponent(componentImplObjectOrClass: Any): ComponentInfo {
        checkInit()

        val componentClass = IComponent::class.java

        require(!componentImplObjectOrClass.javaClass.isInterface) { "register component object must not be interface." }

        val isComponentImplClass = componentImplObjectOrClass is Class<*>

        val realClass = if (isComponentImplClass) componentImplObjectOrClass as Class<*> else componentImplObjectOrClass.javaClass

        require(componentClass.isAssignableFrom(realClass)) { String.format("register component object must implement interface %s.", componentClass) }

        var componentInfo = sComponentInfoArrayMap[realClass]

        if (componentInfo == null) {
            if (isComponentImplClass) {
                componentInfo = ComponentInfo(componentImplObjectOrClass as Class<*>)
            } else {
                componentInfo = ComponentInfo(componentImplObjectOrClass as IComponent)
                componentInfo.initComponent(application)
            }
            sComponentInfoArrayMap[realClass] = componentInfo
            Log.d(TAG, String.format("register component[ %s ] success, with class", realClass))
        }

        return componentInfo
    }

    /**
     * 组件解除绑定
     *
     * @param component
     * @param <T>
     * @return
    </T> */
    @JvmStatic
    fun <T : IComponent> unregisterComponent(component: Class<T>): Boolean {
        checkInit()
        var componentInfo = sComponentInfoArrayMap[component]
        return if (componentInfo == null) {
            false
        } else {
            return if (!componentInfo.isComponentReady()) {
                false
            } else {
                (componentInfo.component as IComponent).detachComponent()
                sComponentInfoArrayMap.remove(component)
                false
            }
        }
    }


    /**
     * 获取组件，如果组件未初始化，则需要初始化组件
     *
     * @param component
     * @param <T>
     * @return
    </T> */
    @JvmStatic
    fun <T : IComponent> getComponent(component: Class<T>): IComponent? {
        checkInit()
        val componentInfo = sComponentInfoArrayMap[component] ?: return null
        return if (componentInfo.isComponentReady()) {
            componentInfo.component as IComponent
        } else {
            componentInfo.initComponent(application)
        }
    }

}
