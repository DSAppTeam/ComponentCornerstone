package com.plugin.library;

import com.plugin.component.anno.AutoInjectImpl;

@AutoInjectImpl(sdk = {ISdk2.class, ISdk3.class})
public class SdkImpl2 implements ISdk2, ISdk3 {
    @Override
    public String getSdk2Name() {
        return "from sdk2";
    }

    @Override
    public String getSdk3Name() {
        return "from sdk3";
    }
}
