package com.plugin.library;

import android.app.Application;


@AutoInjectComponent
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
