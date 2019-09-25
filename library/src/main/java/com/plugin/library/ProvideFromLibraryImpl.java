package com.plugin.library;

import com.plugin.component.anno.AutoInjectImpl;

@AutoInjectImpl(sdk = {IProvideFromLibrary.class})
public class ProvideFromLibraryImpl implements IProvideFromLibrary {

    @Override
    public String provideString() {
        return Component.sdk.provideString() + "\n" + "[I'am library] add by library";
    }

}
