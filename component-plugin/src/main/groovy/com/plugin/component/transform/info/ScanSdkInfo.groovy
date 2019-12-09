package com.plugin.component.transform.info

class ScanSdkInfo {

    public String className
    public List<String> sdk

    ScanSdkInfo(String className) {
        this.className = className
        this.sdk = new ArrayList<>()
    }

    @Override
    String toString() {
        return "SdkInject ==> " + className + " sdk[" + sdk.join(" ") + "]"
    }
}
