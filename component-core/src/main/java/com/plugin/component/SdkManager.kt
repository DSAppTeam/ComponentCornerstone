package com.plugin.component
object SdkManager {

    private const val TAG = "component-core"

    @JvmStatic
    fun register(componentObjectOrClass: Any, sdkKey: Class<*>, sdkImplObjectOrClass: Any) {
        var componentInfo = ComponentManager.hasRegister(componentObjectOrClass)
        if (componentInfo == null) {
            componentInfo = ComponentManager.registerComponent(componentObjectOrClass)
        }
        require(sdkKey.isInterface) { "register service key must be interface class." }
        require(!sdkImplObjectOrClass.javaClass.isInterface) { "register service object must not be interface." }

        val realImplClass = if (sdkImplObjectOrClass is Class<*>) sdkImplObjectOrClass else sdkImplObjectOrClass.javaClass

        require(sdkKey.isAssignableFrom(realImplClass)) { String.format("register service object must implement interface %s.", sdkKey) }

        componentInfo.registerSdk(sdkKey, sdkImplObjectOrClass)
    }

    @JvmStatic
    fun unregister(sdkKey: Class<*>) {
        val componentInfo = ComponentManager.findComponentInfoBySdk(sdkKey)
        if (componentInfo != null) {
            componentInfo.unregisterSdk(sdkKey)
            return
        }
    }

    @JvmStatic
    fun <T> getSdk(sdkKey: Class<T>): T? {
        val componentInfo = ComponentManager.findComponentInfoBySdk(sdkKey)
        if (componentInfo != null) {
            if (!componentInfo.isComponentReady()) {
                componentInfo.initComponent(ComponentManager.application)
            }
            require(componentInfo.isComponentReady()) { "getSdk initComponent fail!" }

            return if (componentInfo.isSdkReady(sdkKey)) {
                componentInfo.getImpl<T>(sdkKey)
            } else {
                componentInfo.initSdk<T>(sdkKey)
            }
        }
        return null
    }
}
