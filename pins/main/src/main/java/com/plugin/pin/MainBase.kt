package com.plugin.pin

import com.plugin.pin.base.PBase
import com.plugin.pin.common.PCommon
import com.plugin.pin.home.PHome

class MainBase {

    fun getString() = PBase().getString() + PCommon().getString() +  PHome().getString()
}