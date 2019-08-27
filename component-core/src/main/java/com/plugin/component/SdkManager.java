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
        Class realComponentClass = sdkImplObjectOrClass instanceof Class ? (Class) sdkImplObjectOrClass : sdkImplObjectOrClass.getClass();
        Class realImplClass = sdkImplObjectOrClass instanceof Class ? (Class) sdkImplObjectOrClass : sdkImplObjectOrClass.getClass();
        if (!sdkKey.isAssignableFrom(realImplClass)) {
            throw new IllegalArgumentException(String.format("register service object must implement interface %s.", sdkKey));
        }
        Log.d(TAG, String.format("register sdk[ %s ] in component[ %s ] success, with %s", sdkKey, realComponentClass, realImplClass));
        componentInfo.registerSdk(sdkKey, sdkImplObjectOrClass);
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
                    result = ((Class) result).newInstance();
                    componentInfo.registerSdk(sdkKey, result);
                    Log.d(TAG, String.format("before getting sdk[ %s ] , newInstance for sdk success.", sdkKey));
                    Log.d(TAG, String.format("get sdk[ %s ] success. ", result.getClass()));
                    return (T) result;
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (InstantiationException e) {
                    e.printStackTrace();
                }
            }
            if (result == null || result instanceof Class) {
                Log.d(TAG, String.format("get sdk[ %s ] fail. ", sdkKey));
            } else {
                Log.d(TAG, String.format("get sdk[ %s ] success. ", result.getClass()));
            }
        }
        Log.d(TAG, String.format("get sdk[ %s ] fail, no component has it.", sdkKey));
        return null;
    }
}
