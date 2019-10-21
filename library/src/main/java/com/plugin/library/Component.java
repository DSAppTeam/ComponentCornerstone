package com.plugin.library;

import android.app.Application;
import android.util.Log;

import com.plugin.component.SdkManager;
import com.plugin.component.anno.AutoInjectComponent;
import com.plugin.component.IComponent;


@AutoInjectComponent(impl = {SdkShareImpl22.class, ProvideFromLibraryImpl.class})
public class Component implements IComponent {

    @Override
    public void attachComponent(Application application) {
        Log.d("component-plugin", "Component#attachComponent");
        SdkManager.register(this, ISdk.class, SdkShareImpl22.class);
        SdkManager.register(this, ISdk2.class, SdkShareImpl22.class);
        SdkManager.register(this, IProvideFromLibrary.class, ProvideFromLibraryImpl.class);
    }

    @Override
    public void detachComponent() {
        Log.d("component-plugin", "Component#detachComponent");
        SdkManager.unregister(ISdk.class);
        SdkManager.unregister(ISdk2.class);
        SdkManager.unregister(IProvideFromLibrary.class);
    }
}
