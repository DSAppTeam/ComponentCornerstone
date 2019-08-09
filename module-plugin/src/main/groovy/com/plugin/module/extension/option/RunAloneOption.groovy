package com.plugin.module.extension.option

/**
 * 单独运行配置信息
 * created by yummylau 2019/08/09
 */
class RunAloneOption {

    String name

    boolean isRegisterComponentAuto

    String applicationName

    boolean runAlone

    void isRegisterComponentAuto(boolean auto) {
        this.isRegisterComponentAuto = auto
    }

    void applicationName(String applicationName) {
        this.applicationName = applicationName
    }

    void runAlone(boolean runAlone) {
        this.runAlone = runAlone
    }

}