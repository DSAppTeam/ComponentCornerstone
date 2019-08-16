package com.plugin.library;

import com.plugin.component.ComponentManager;
import com.plugin.component.SdkManager;

public class A {

    public void init() {
        ComponentManager.registerComponent(Component.class);
        SdkManager.register(Component.class, ISdk.class, SdkImpl.class);
        SdkManager.register(Component.class, ISdk2.class, SdkImpl2.class);
        SdkManager.register(Component.class, ISdk3.class, SdkImpl2.class);
    }
}
