package com.plugin.library;

import com.plugin.component.SdkManager;
import com.plugin.component.anno.AutoInjectImpl;
import com.plugin.librarykotlin.IProvideFromKotlin;

@AutoInjectImpl(sdk = {ISdk.class, ISdk2.class})
public class SdkShareImpl implements ISdk, ISdk2 {

    @Override
    public String getSdkName() {
        return "I am library, libraryKotlin's ProvideFromKotlinImpl.provideString is " + SdkManager.getSdk(IProvideFromKotlin.class).provideString();
    }

    @Override
    public String getSdk2Name() {
        return "from sdk2";
    }

}
