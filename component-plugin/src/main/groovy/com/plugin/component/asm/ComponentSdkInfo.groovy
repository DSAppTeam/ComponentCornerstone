package com.plugin.component.asm

class ComponentSdkInfo {

    public String componentClassName
    public Object sdk
    public Object impl

    boolean isVaild() {
        return componentClassName != null && !componentClassName.isEmpty() && sdk instanceof Class && impl instanceof Class
    }

    String toString() {
        return "componentClassName(" + componentClassName + ") " +"sdk(" + sdk + ") " + "impl(" + impl + ")"
    }
}
