package com.plugin.core;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

/**
 * 组件注册管理
 * Email yummyl.lau@gmail.com
 * Created by yummylau on 2018/01/25.
 */

public class ComponentManager {

    private static final String TAG = ComponentManager.class.getSimpleName();

    private static Map<String, IComponent> sComponentMap = new HashMap<>();

    public static <T extends IComponent> void bind(@NonNull Application application, @NonNull Class<T> service) {
        IComponent registerService = null;
        try {
            Constructor constructor = service.getConstructor();
            registerService = (IComponent) constructor.newInstance();
        } catch (Exception e) {
            Log.e(TAG, "bind fail!:" + e.getMessage());
        }
        if (registerService != null) {
            sComponentMap.put(service.getSimpleName(), registerService);
            registerService.attachComponent(application);
        }

        Class IComponentImpl = ifClassImplementsIComponent(service.getInterfaces());
        if (IComponentImpl != null) {
            sComponentMap.put(IComponentImpl.getSimpleName(), registerService);
            registerService.attachComponent(application);
        } else {
            Log.e(TAG, "IComponent is not component's grandfather!");
        }
    }


    public static Class ifClassImplementsIComponent(Class[] interfaces) {
        int length = interfaces.length;
        int i = 0;
        while (i < length) {
            Class result = ifClassImplementsIComponent(interfaces[i]);
            if (result != null) {
                return result;
            }
            if (ifClassHadImplementsInterface(interfaces[i])) {
                return ifClassImplementsIComponent(interfaces[i].getInterfaces());
            }
            i++;
        }
        return null;
    }

    public static boolean ifClassHadImplementsInterface(Class clazz) {
        boolean result = false;
        if (clazz != null && clazz.getInterfaces() != null && clazz.getInterfaces().length > 0) {
            result = true;
        }
        return result;
    }

    public static Class ifClassImplementsIComponent(Class interfaces) {
        Class[] classes = interfaces.getInterfaces();
        if (classes != null && classes.length > 0) {
            for (int i = 0; i < classes.length; i++) {
                if (classes[0].getSimpleName().equals(IComponent.class.getSimpleName())) {
                    return interfaces;
                }
            }
        }
        return null;
    }


    @Nullable
    public static <T extends IComponent> T getComponent(@NonNull Class<T> service) {
        IComponent result = null;
        if (service != null) {
            result = sComponentMap.get(service.getSimpleName());
        }
        if (result == null) {
            Log.w(TAG, "has not component which match a servicePath key!");
        }
        return (T) result;
    }

    public static <T extends IComponent> void unbind(@NonNull Class<T> service) {
        if (service == null) {
            Log.w(TAG, "component class can't be null");
            return;
        }
        T toUnRegister = (T) sComponentMap.remove(service.getSimpleName());
        toUnRegister.detachComponent();
    }
}
