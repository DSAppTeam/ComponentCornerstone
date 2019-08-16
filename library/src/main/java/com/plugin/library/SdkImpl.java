package com.plugin.library;

import com.plugin.component.anno.AutoInjectImpl;

@AutoInjectImpl(sdk = ISdk.class)
public class SdkImpl implements ISdk {

    @Override
    public String getSdkName() {
        return "from sdk";
    }
}
