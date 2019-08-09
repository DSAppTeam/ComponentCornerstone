package com.plugin.library;

import android.app.Application;

import com.plugin.core.IComponent;
import com.plugin.core.SdkManager;
import com.plugin.core.anno.AutoInject;

@AutoInject
public class LibraryComponent implements IComponent {

    @Override
    public void attachComponent(Application application) {
        SdkManager.register(IAction.class, new LibraryAction());
    }

    @Override
    public void detachComponent() {
        SdkManager.unregister(IAction.class);
    }
}
