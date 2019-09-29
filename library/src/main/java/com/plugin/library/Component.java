package com.plugin.library;

import android.app.Application;
import android.util.Log;

import com.plugin.component.anno.AutoInjectComponent;
import com.plugin.component.IComponent;


@AutoInjectComponent(impl = {SdkShareImpl.class, ProvideFromLibraryImpl.class})
public class Component implements IComponent {

    @Override
    public void attachComponent(Application application) {
        Log.d("component-plugin", "Component#attachComponent");
    }

    @Override
    public void detachComponent() {
        Log.d("component-plugin", "Component#detachComponent");
    }
}
