package com.plugin.component.extension.option.addition

import org.gradle.util.ConfigureUtil

/**
 * 扩展模块选项
 */
class AdditionOption {

    MethodCostOption methodCostOption = new MethodCostOption()

    /**
     * 配置方法耗时
     * @param closure
     */
    void methodCost(Closure closure) {
        ConfigureUtil.configure(closure, methodCostOption)
    }


    @Override
    public String toString() {
        return "AdditionOption{" + '\n' +
                "       methodCostOption  =  " + methodCostOption + '\n' +
                '       }';
    }
}