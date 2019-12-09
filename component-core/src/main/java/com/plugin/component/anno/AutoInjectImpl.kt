package com.plugin.component.anno


import kotlin.reflect.KClass

/**
 * 自动注入sdk实现
 */
@Target(AnnotationTarget.CLASS)
@kotlin.annotation.Retention(AnnotationRetention.BINARY)
annotation class AutoInjectImpl(val sdk: Array<KClass<*>>)
