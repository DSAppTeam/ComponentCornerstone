package com.plugin.component.anno;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 用于注解 sdk 接口，自动注入
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface AutoInjectSdk {

    String name() default "DEFAULT";
}
