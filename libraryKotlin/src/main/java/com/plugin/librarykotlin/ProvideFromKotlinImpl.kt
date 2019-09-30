package com.plugin.librarykotlin

import com.plugin.component.SdkManager
import com.plugin.component.anno.AutoInjectImpl
import com.plugin.librarykotlin2.IProvideFromLibraryKotlin2

@AutoInjectImpl(sdk = [IProvideFromKotlin::class])
class ProvideFromKotlinImpl : IProvideFromKotlin {

    override fun provideString(): String = SdkManager.getSdk(IProvideFromLibraryKotlin2::class.java)!!.provideString() + "\n" + "[I'am libraryKotlin] add by libraryKotlin"
}
