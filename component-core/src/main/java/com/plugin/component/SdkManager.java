package com.plugin.component;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class SdkManager {

    private static final String TAG = "component-core";

    public static void register(@NonNull Object componentObjectOrClass, @NonNull Class sdkKey, @NonNull Object sdkImplObjectOrClass) {
        ComponentInfo componentInfo = ComponentManager.hasRegister(componentObjectOrClass);
        if (componentInfo == null) {
            componentInfo = ComponentManager.registerComponent(componentObjectOrClass);
        }
        if (!sdkKey.isInterface()) {
            throw new IllegalArgumentException("register service key must be interface class.");
        }
        if (sdkImplObjectOrClass.getClass().isInterface()) {
            throw new IllegalArgumentException("register service object must not be interface.");
        }
        Class realComponentClass = componentObjectOrClass instanceof Class ? (Class) componentObjectOrClass : componentObjectOrClass.getClass();
        Class realImplClass = sdkImplObjectOrClass instanceof Class ? (Class) sdkImplObjectOrClass : sdkImplObjectOrClass.getClass();
        if (!sdkKey.isAssignableFrom(realImplClass)) {
            throw new IllegalArgumentException(String.format("register service object must implement interface %s.", sdkKey));
        }
        componentInfo.registerSdk(sdkKey, sdkImplObjectOrClass);
        if (sdkImplObjectOrClass instanceof Class) {
            Log.d(TAG, String.format("register sdk[ %s ] in component[ %s ] success, with [ %s ]", sdkKey, realComponentClass, realImplClass));
        } else {
            Log.d(TAG, String.format("register sdk[ %s ] in component[ %s ] success, with object[ %s ]", sdkKey, realComponentClass, sdkImplObjectOrClass.toString()));
        }
    }

    public static void unregister(Class sdkKey) {
        ComponentInfo componentInfo = ComponentManager.findComponentInfoBySdk(sdkKey);
        if (componentInfo != null) {
            componentInfo.unregisterSdk(sdkKey);
            Log.d(TAG, String.format("unregister sdk[ %s ] success, component[ %s ] remove it.", sdkKey, componentInfo.componentClass));
            return;
        }
        Log.d(TAG, String.format("unregister sdk[ %s ] fail, no component has it.", sdkKey));
    }

    @Nullable
    public static <T> T getSdk(Class<T> sdkKey) {
        ComponentInfo componentInfo = ComponentManager.findComponentInfoBySdk(sdkKey);
        if (componentInfo != null) {
            Object result;
            if (componentInfo.componentImpl == null) {
                try {
                    componentInfo.componentImpl = (IComponent) componentInfo.componentClass.newInstance();
                    componentInfo.componentImpl.attachComponent(ComponentManager.getApplication());
                    Log.d(TAG, String.format("before getting sdk[ %s ] , component[ %s ] doing attachComponent.", sdkKey, componentInfo.componentClass));
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                    Log.d(TAG, String.format("before getting sdk[ %s ] , component[ %s ] init fail." + e.getMessage(), sdkKey, componentInfo.componentClass));
                } catch (InstantiationException e) {
                    e.printStackTrace();
                    Log.d(TAG, String.format("before getting sdk[ %s ] , component[ %s ] init fail.", sdkKey, componentInfo.componentClass));
                }
            }

            if (componentInfo.componentImpl == null) {
                Log.d(TAG, String.format("before getting sdk[ %s ] , component[ %s ] init fail.", sdkKey, componentInfo.componentClass));
                return null;
            }

            result = componentInfo.getSdk(sdkKey);
            if (result instanceof Class) {
                try {
                    Log.d(TAG, String.format("getting sdk[ %s ] can't find any impl object, new an impl [ %s ] object for sdk.", sdkKey, result));
                    result = ((Class) result).newInstance();
                    componentInfo.registerSdk(sdkKey, result);
                    Log.d(TAG, String.format("new impl [ %s ] success, object is [ %s ]. ", result.getClass(), result.toString()));
                    return (T) result;
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                    Log.d(TAG, String.format("new impl [ %s ] fail, reason is %s. ", result.getClass(), e.getMessage()));
                } catch (InstantiationException e) {
                    e.printStackTrace();
                    Log.d(TAG, String.format("new impl [ %s ] fail, reason is %s . ", result.getClass(), e.getMessage()));
                }
            }
            if (result == null || result instanceof Class) {
                Log.d(TAG, String.format("get sdk[ %s ] fail. ", sdkKey));
            } else {
                Log.d(TAG, String.format("get sdk[ %s ] success, impl's object is [ %s ] ", sdkKey, result.toString()));
                return (T) result;
            }
        }
        Log.d(TAG, String.format("get sdk[ %s ] fail, no component has it.", sdkKey));
        return null;
    }
}
