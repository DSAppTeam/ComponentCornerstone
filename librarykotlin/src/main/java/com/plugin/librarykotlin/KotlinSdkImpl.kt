package com.plugin.librarykotlin

import com.plugin.component.anno.AutoInjectImpl

@AutoInjectImpl(sdk = [IKotlinSdk::class])
class KotlinSdkImpl : IKotlinSdk {

    override fun getKotlinSdkName(): String = "KotlinSdkImpl"
}
