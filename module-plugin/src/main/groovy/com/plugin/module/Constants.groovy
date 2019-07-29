package com.plugin.module

class Constants{

    public static String TAG = "AlonePlugin"
    public static String DEFAULT_APP_NAME = "app"       //默认是app，直接运行assembleRelease的时候，等同于运行app:assembleRelease

    //自定义plugin参数
    public static String EXTENSION_NAME = "componentBuild"
    public static String EXTENSION_APPLICATION_KEY = "applicationName"
    public static String EXTENSION_REGISTER_AUTO_KEY = "isRegisterCompoAuto"

    //newDirs
    public static String newDirs = "runalone";

    //sourceSets
    public static String AFTER_MANIFEST_PATH = "src/main/runalone/AndroidManifest.xml"
    public static String AFTER_JAVA_PATH = "src/main/runalone/java"
    public static String AFTER_RES_PATH = "src/main/runalone/res"
    public static String AFTER_ASSETS_PATH = "src/main/runalone/assets"
    public static String AFTER_JNILIBS_PATH = "src/main/runalone/jniLibs"

    public static String JAVA_PATH = "src/main/java"
    public static String RES_PATH = "src/main/res"
    public static String ASSETS_PATH = "src/main/assets"
    public static String JNILIBS_PATH = "src/main/jniLibs"

    public static String MIS_JAVA_PATH = "src/main/mis/java"

    //plugin
    public static String PLUGIN_APPLICATION = "com.android.application"
    public static String PLUGIN_LIBRARY = "com.android.library"

    //gradle.properties 需要配置的属性
    public static String PROPERTIES_MAIN_MODULE_NAME = "mainmodulename"
    public static String PROPERTIES_ISRUNALONE = "isRunAlone"

}