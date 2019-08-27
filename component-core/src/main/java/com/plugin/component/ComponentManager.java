package com.plugin.component;

import android.app.Application;
import android.content.Context;
import android.util.ArrayMap;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Set;

/**
 * 组件管理
 * 一个component 可对应多个 sdk
 * Email yummyl.lau@gmail.com
 * Created by yummylau on 2018/01/25.
 */
public class ComponentManager {

    private static final String TAG = "component-core";

    private static Application sApplication;
    private static ArrayMap<Class, ComponentInfo> sComponentInfoArrayMap;
    private static boolean sInit = false;

    /**
     * 注入代码入口
     *
     * @param application
     */
    public static void init(Application application) {
        if (sInit) {
            return;
        }
        ComponentManager.sApplication = application;
        sComponentInfoArrayMap = new ArrayMap<>();
        sInit = true;
    }

    static Application getApplication() {
        return sApplication;
    }

    private static void checkInit() {
        if (!sInit) {
            throw new RuntimeException("you should invoke ComponentManager#init() before calling apis");
        }
    }


    public static ComponentInfo findComponentInfoBySdk(Class sdkKey) {
        checkInit();
        Set<Class> classes = sComponentInfoArrayMap.keySet();
        for (Class clazz : classes) {
            ComponentInfo componentInfo = sComponentInfoArrayMap.get(clazz);
            if (componentInfo.hasSdk(sdkKey)) {
                return componentInfo;
            }
        }
        return null;
    }


    public static ComponentInfo hasRegister(@NonNull Object componentObjectOrClass) {
        checkInit();
        boolean isComponentImplClass = componentObjectOrClass instanceof Class;
        Class realComponentClass = isComponentImplClass ? (Class) componentObjectOrClass : componentObjectOrClass.getClass();
        return sComponentInfoArrayMap.get(realComponentClass);
    }


    /**
     * 注册组件实现累必须实现 IComponent
     *
     * @param componentImplObjectOrClass 支持 class 类型 或者 object 类型
     *                                   class 类型用于自动注册或者获取sdk时懒初始化
     *                                   object 类型用于手动注册，不支持 sdk 加载时再懒初始化
     */
    public static ComponentInfo registerComponent(@NonNull Object componentImplObjectOrClass) {
        checkInit();
        Class componentClass = IComponent.class;

        if (componentImplObjectOrClass.getClass().isInterface()) {
            throw new IllegalArgumentException("register component object must not be interface.");
        }

        boolean isComponentImplClass = componentImplObjectOrClass instanceof Class;

        Class realClass = isComponentImplClass ? (Class) componentImplObjectOrClass : componentImplObjectOrClass.getClass();

        if (!componentClass.isAssignableFrom(realClass)) {
            throw new IllegalArgumentException(String.format("register component object must implement interface %s.", componentClass));
        }
        ComponentInfo componentInfo = sComponentInfoArrayMap.get(realClass);
        if (componentInfo != null) {
            //如果已经存在当时未初始化且当前覆盖的为实现类
            if (componentInfo.componentImpl == null && !isComponentImplClass) {
                componentInfo.componentImpl = (IComponent) componentImplObjectOrClass;
                componentInfo.componentImpl.attachComponent(sApplication);
                sComponentInfoArrayMap.put(realClass, componentInfo);
                Log.d(TAG, String.format("register component[ %s ] success, with object that overriding class, doing attachComponent ", realClass));
            }
        } else {
            if (isComponentImplClass) {
                componentInfo = new ComponentInfo((Class) componentImplObjectOrClass);
                sComponentInfoArrayMap.put(realClass, componentInfo);
                Log.d(TAG, String.format("register component[ %s ] success, with class", realClass));
            } else {
                componentInfo = new ComponentInfo((IComponent) componentImplObjectOrClass);
                componentInfo.componentImpl.attachComponent(sApplication);
                sComponentInfoArrayMap.put(realClass, componentInfo);
                Log.d(TAG, String.format("register component[ %s ] success, with object", realClass));
            }
        }
        return componentInfo;
    }

    /**
     * 组件解除绑定
     *
     * @param component
     * @param <T>
     * @return
     */
    public static <T extends IComponent> boolean unregisterComponent(@NonNull Class<T> component) {
        checkInit();
        ComponentInfo componentInfo = sComponentInfoArrayMap.get(component);
        if (componentInfo == null) {
            Log.d(TAG, String.format("unregister component[ %s ] fail, didn't register %s.", component, component));
            return false;
        }
        if (componentInfo.componentImpl != null) {
            componentInfo.componentImpl.detachComponent();
            Log.d(TAG, String.format("unregister component[ %s ] success, doing detachComponent", component));
        } else {
            Log.d(TAG, String.format("unregister component[ %s ] success", component));
        }
        sComponentInfoArrayMap.remove(component);
        return false;
    }


    /**
     * 获取组件，如果组件未初始化，则需要初始化组件
     *
     * @param component
     * @param <T>
     * @return
     */
    @Nullable
    public static <T extends IComponent> IComponent getComponent(@NonNull Class<T> component) {
        checkInit();
        ComponentInfo componentInfo = sComponentInfoArrayMap.get(component);
        if (componentInfo == null) {
            return null;
        }
        IComponent result = componentInfo.componentImpl;
        //如果已经初始化，则直接返回
        if (result != null) {
            return result;
        } else {
            try {
                result = (IComponent) componentInfo.componentClass.newInstance();
                result.attachComponent(sApplication);
                componentInfo.componentImpl = result;
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
