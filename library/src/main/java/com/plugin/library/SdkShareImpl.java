package com.plugin.library;

import com.plugin.component.anno.AutoInjectImpl;

@AutoInjectImpl(sdk = {ISdk.class, ISdk2.class})
public class SdkShareImpl implements ISdk, ISdk2 {

    @Override
    public String getSdkName() {
        return "from sdk";
    }

    @Override
    public String getSdk2Name() {
        return "from sdk2";
    }

}
