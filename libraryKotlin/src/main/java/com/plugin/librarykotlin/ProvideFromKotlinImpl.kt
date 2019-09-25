package com.plugin.librarykotlin

import com.plugin.component.anno.AutoInjectImpl

@AutoInjectImpl(sdk = [IProvideFromKotlin::class])
class ProvideFromKotlinImpl : IProvideFromKotlin {

    override fun provideString(): String = KotlinComponent.sdk.provideString() + "\n" + "[I'am libraryKotlin] add by libraryKotlin"

}
