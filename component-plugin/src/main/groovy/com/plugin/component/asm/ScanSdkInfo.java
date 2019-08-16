package com.plugin.component.asm;

import java.util.ArrayList;
import java.util.List;

public class ScanSdkInfo {

    public String className;
    public List<Object> sdk;

    public ScanSdkInfo(String className) {
        this.className = className;
        this.sdk = new ArrayList<>();
    }

    @Override
    public String toString() {
        return "SdkInject ==> " + className + "sdk" + getString(sdk);
    }

    public String getString(List<Object> objects) {
        StringBuilder stringBuilder = new StringBuilder("[");
        for (Object object : objects) {
            stringBuilder.append(object);
            stringBuilder.append(",");
        }
        if (stringBuilder.toString().length() > 1) {
            stringBuilder.delete(stringBuilder.length() - 1, stringBuilder.length());
        }
        stringBuilder.append("]");
        return stringBuilder.toString();
    }
}
