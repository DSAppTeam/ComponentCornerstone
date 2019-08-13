package com.plugin.component;

import android.util.ArrayMap;

/**
 * 运行时完成收集
 */
public class ComponentInfo {

    public ComponentInfo(Class componentClass) {
        this.componentClass = componentClass;
    }

    public ComponentInfo(IComponent componentObject) {
        this.componentClass = componentObject.getClass();
        this.componentObject = componentObject;
    }

    public Class componentClass;
    public IComponent componentObject;
    public ArrayMap<Class, Class> sdkMap;


}
