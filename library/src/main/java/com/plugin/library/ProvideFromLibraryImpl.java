package com.plugin.library;

import com.plugin.component.ComponentManager;
import com.plugin.component.SdkManager;
import com.plugin.component.anno.AutoInjectImpl;
import com.plugin.librarykotlin.IProvideFromKotlin;

@AutoInjectImpl(sdk = {IProvideFromLibrary.class})
public class ProvideFromLibraryImpl implements IProvideFromLibrary {

    @Override
    public String provideString() {
        return SdkManager.getSdk(IProvideFromKotlin.class).provideString() + "\n" + "[I'am library] add by library";
    }
}
