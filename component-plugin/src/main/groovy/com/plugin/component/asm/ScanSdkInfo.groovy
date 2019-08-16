package com.plugin.component.asm

class ScanSdkInfo {

    public String className
    public List<Object> sdk

    ScanSdkInfo(String className) {
        this.className = className
        this.sdk = new ArrayList<>()
    }

    @Override
    String toString() {
        return "SdkInject ==> " + className + " sdk[" + sdk.join(",") + "]"
    }
}
