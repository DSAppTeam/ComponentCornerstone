package com.plugin.component.extension.option

import org.gradle.api.JavaVersion

/**
 * 编译选项
 * created by yummylau 2019/08/09
 */
class CompileOptions {

    JavaVersion sourceCompatibility = JavaVersion.current().isJava8Compatible()
            ? JavaVersion.VERSION_1_8 : JavaVersion.VERSION_1_6

    JavaVersion targetCompatibility = JavaVersion.current().isJava8Compatible()
            ? JavaVersion.VERSION_1_8 : JavaVersion.VERSION_1_6


    void sourceCompatibility(Object value) {
        sourceCompatibility = value
    }

    void targetCompatibility(Object value) {
        targetCompatibility = value
    }
}