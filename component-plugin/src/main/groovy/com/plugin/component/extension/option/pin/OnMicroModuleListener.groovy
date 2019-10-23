package com.plugin.component.extension.option.pin

import com.plugin.component.extension.module.MicroModule


interface OnMicroModuleListener {

    void addIncludeMicroModule(MicroModule microModule, boolean mainMicroModule)

    void addExportMicroModule(String... microModulePaths)

}
