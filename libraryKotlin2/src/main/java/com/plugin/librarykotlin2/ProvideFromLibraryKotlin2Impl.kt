package com.plugin.librarykotlin2

import com.plugin.component.anno.AutoInjectImpl

@AutoInjectImpl(sdk = [IProvideFromLibraryKotlin2::class])
class ProvideFromLibraryKotlin2Impl : IProvideFromLibraryKotlin2 {

//    override fun provideString(): String = "[I'am libraryKotlin2] add by libraryKotlin2"

//    override fun provideString(): String = "[I'am libraryKotlin2,breaking loop component，'" +
//            Kotlin2Component.sdk.sdkName + "' from library " +
//            "] add by libraryKotlin2"
//
    override fun provideString(): String = "[I'am libraryKotlin2" +
            "] add by libraryKotlin2"
}