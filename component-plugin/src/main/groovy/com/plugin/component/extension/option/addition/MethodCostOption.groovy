package com.plugin.component.extension.option.addition


/**
 * 方法耗时选项
 */
class MethodCostOption {

    boolean enable = false

    void enable(boolean open) {
        this.enable = open
    }


    @Override
    public String toString() {
        return "MethodCostOption{ " +
                "enable=" + enable +
                ' }';
    }
}