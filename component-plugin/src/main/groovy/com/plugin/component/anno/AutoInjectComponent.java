package com.plugin.component.anno;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 用于注解 IComponent接口，自动注入
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface AutoInjectComponent {

    String name() default "";

    Class[] sdk() default {};

    Class[] impl() default {};
}
