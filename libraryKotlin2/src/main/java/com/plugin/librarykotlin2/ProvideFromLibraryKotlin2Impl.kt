package com.plugin.librarykotlin2

import com.plugin.component.SdkManager
import com.plugin.component.anno.AutoInjectImpl
import com.plugin.library.ISdk

@AutoInjectImpl(sdk = [IProvideFromLibraryKotlin2::class])
class ProvideFromLibraryKotlin2Impl : IProvideFromLibraryKotlin2 {

    /**
     * 依赖library
     * 测试循环依赖 library -> libraryKotlin -> libraryKotlin2 -> library
     */
    override fun provideString(): String = "[I'am libraryKotlin2,breaking loop component，'" +
            SdkManager.getSdk(ISdk::class.java)!!.sdkName + "' from library " +
            "] add by libraryKotlin2"
}