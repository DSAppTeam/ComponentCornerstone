package com.plugin.component.anno

import kotlin.reflect.KClass

/**
 * 自动注入组件实现
 */
@Target(AnnotationTarget.CLASS)
@kotlin.annotation.Retention(AnnotationRetention.BINARY)
annotation class AutoInjectComponent(val impl: Array<KClass<*>>)
