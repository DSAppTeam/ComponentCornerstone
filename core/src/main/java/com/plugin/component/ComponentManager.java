package com.plugin.component;

import android.app.Application;
import android.util.ArrayMap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.ref.WeakReference;

/**
 * 组件管理
 * 一个component 可对应多个 sdk
 * Email yummyl.lau@gmail.com
 * Created by yummylau on 2018/01/25.
 */
public class ComponentManager {

    private static Application sApplication;
    private static ArrayMap<Class, ComponentInfo> sComponentInfoArrayMap;

    public ComponentInfo findSdk() {

    }


    /**
     * 注册组件实现累必须实现 IComponent
     *
     * @param componentObjectOrClass 支持 class 类型 或者 object 类型
     *                               class 类型用于自动注册或者获取sdk时懒初始化
     *                               object 类型用于手动注册，不支持 sdk 加载时再懒初始化
     */
    public static void registerComponent(@NonNull Object componentObjectOrClass) {
        Class componentClass = IComponent.class;

        if (componentObjectOrClass.getClass().isInterface()) {
            throw new IllegalArgumentException("register component object must not be interface.");
        }

        boolean isComponentImplClass = componentObjectOrClass instanceof Class;

        Class realClass = isComponentImplClass ? (Class) componentObjectOrClass : componentObjectOrClass.getClass();

        if (!componentClass.isAssignableFrom(realClass)) {
            throw new IllegalArgumentException(String.format("register service object must implement interface %s.", componentClass));
        }

        if (sComponentInfoArrayMap == null) {
            sComponentInfoArrayMap = new ArrayMap<>();
        }

        if (isComponentImplClass) {
            ComponentInfo componentInfo = new ComponentInfo((Class) componentObjectOrClass);
            sComponentInfoArrayMap.put(realClass, componentInfo);
        } else {
            ComponentInfo componentInfo = new ComponentInfo((IComponent) componentObjectOrClass);
            componentInfo.componentObject.attachComponent(sApplication);
            sComponentInfoArrayMap.put(realClass, componentInfo);
        }
    }

    /**
     * 组件解除绑定
     *
     * @param component
     * @param <T>
     * @return
     */
    public static <T extends IComponent> boolean unregisterComponent(@NonNull Class<T> component) {
        if (sComponentInfoArrayMap == null) {
            return false;
        }
        ComponentInfo componentInfo = sComponentInfoArrayMap.get(component);
        if (componentInfo == null) {
            return false;
        }
        if (componentInfo.componentObject != null) {
            componentInfo.componentObject.detachComponent();
        }
        sComponentInfoArrayMap.remove(component);
        return false;
    }


    @Nullable
    public static <T extends IComponent> IComponent getComponent(@NonNull Class<T> component) {
        if (sComponentInfoArrayMap == null) {
            return null;
        }
        ComponentInfo componentInfo = sComponentInfoArrayMap.get(component);
        if (componentInfo == null) {
            return null;
        }
        IComponent result = componentInfo.componentObject;
        //如果已经初始化，则直接返回
        if (result != null) {
            return result;
        } else {
            try {
                result = (IComponent) componentInfo.componentClass.newInstance();
                result.attachComponent(sApplication);
                componentInfo.componentObject = result;
                return result;
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InstantiationException e) {
                e.printStackTrace();
            }
            return null;
        }
    }

}
