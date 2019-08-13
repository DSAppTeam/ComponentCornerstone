package com.plugin.library;

import android.app.Application;

import com.plugin.component.AutoInjectComponent;
import com.plugin.component.IComponent;


@AutoInjectComponent(
        sdk = {IAction.class},
        impl = {LibraryAction.class})
public class LibraryComponent implements IComponent {

    @Override
    public void attachComponent(Application application) {
    }

    @Override
    public void detachComponent() {
    }
}
