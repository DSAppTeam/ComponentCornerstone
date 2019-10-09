package com.plugin.librarykotlin

import com.plugin.component.SdkManager
import com.plugin.component.anno.AutoInjectImpl
import com.plugin.library.ISdk2


@AutoInjectImpl(sdk = [IGetFromLibrary::class])
class GetFromLibraryImpl : IGetFromLibrary {

    override fun provideString(): String = "I am libraryKotlin, library's ISdk2.name is " + SdkManager.getSdk(ISdk2::class.java)!!.sdk2Name

}