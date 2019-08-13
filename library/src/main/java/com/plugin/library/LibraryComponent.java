package com.plugin.library;

import android.app.Application;

import com.plugin.component.anno.AutoInjectComponent;
import com.plugin.component.IComponent;


@AutoInjectComponent
public class LibraryComponent implements IComponent {

    @Override
    public void attachComponent(Application application) {
    }

    @Override
    public void detachComponent() {
    }
}
