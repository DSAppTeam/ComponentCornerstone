package com.plugin.pins.extension

import com.plugin.pins.MicroModule


interface OnMicroModuleListener {

    void addIncludeMicroModule(MicroModule microModule, boolean mainMicroModule)

    void addExportMicroModule(String... microModulePaths)

}
