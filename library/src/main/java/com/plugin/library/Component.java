package com.plugin.library;

import android.app.Application;

import com.plugin.component.anno.AutoInjectComponent;
import com.plugin.component.IComponent;


@AutoInjectComponent(impl = {SdkImpl.class, SdkImpl2.class})
public class Component implements IComponent {

    @Override
    public void attachComponent(Application application) {
        //todo init code
    }

    @Override
    public void detachComponent() {
        //todo release code
    }
}
