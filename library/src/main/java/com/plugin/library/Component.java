package com.plugin.library;

import android.app.Application;
import android.util.Log;

import com.plugin.component.ComponentManager;
import com.plugin.component.SdkManager;
import com.plugin.component.anno.AutoInjectComponent;
import com.plugin.component.IComponent;
import com.plugin.librarykotlin.IProvideFromKotlin;

@AutoInjectComponent(impl = {SdkShareImpl.class, ProvideFromLibraryImpl.class})
public class Component implements IComponent {

    public static IProvideFromKotlin sdk;

    @Override
    public void attachComponent(Application application) {
        Log.d("component-plugin", "Component#attachComponent");
        ComponentManager.init(application);
        sdk = SdkManager.getSdk(IProvideFromKotlin.class);
    }

    @Override
    public void detachComponent() {
        Log.d("component-plugin", "Component#detachComponent");
    }
}
