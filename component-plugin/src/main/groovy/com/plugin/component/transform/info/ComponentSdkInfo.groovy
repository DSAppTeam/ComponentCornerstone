package com.plugin.component.transform.info

class ComponentSdkInfo {

    public String componentClassName
    public String sdk = ""
    public String impl = ""

    boolean isValid() {
        return componentClassName != null && !componentClassName.isEmpty() && !sdk.isEmpty() && !impl.isEmpty()
    }

    String toString() {
        return "componentClassName(" + componentClassName + ") " + "sdk(" + sdk + ") " + "impl(" + impl + ")"
    }
}
