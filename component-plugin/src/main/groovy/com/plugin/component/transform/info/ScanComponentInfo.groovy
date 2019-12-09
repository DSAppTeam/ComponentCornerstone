package com.plugin.component.transform.info

class ScanComponentInfo {

    public String className
    public List<String> impl

    ScanComponentInfo(String className) {
        this.className = className
        this.impl = new ArrayList<>()
    }

    @Override
    String toString() {
        return "ComponentInject ==> " + className + "impl[" + impl.join(" ") + "]"
    }

}
