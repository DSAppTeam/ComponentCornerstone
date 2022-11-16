package com.plugin.component

/**
 *  常量定义
 *  created by yummylau 2019/08/09
 */
class Constants {

    //业务无关
    public static String TAG = "> ComponentPlugin :"
    public static String ANDROID = 'android'
    public static String ANDROID_HOME = 'ANDROID_HOME'
    public static String SDK = "sdk"
    public static String MAIN = 'main'
    public static String MODULE = "module"
    public static String COMPONENT = "component"
    public static String JAVA = "java"
    public static String JAVA_FILE_END = ".java"
    public static String KOTLIN_FILE_END = ".kt"
    public static String PLUGIN_APPLICATION = "com.android.application"
    public static String PLUGIN_LIBRARY = "com.android.library"
    public static String PLUGIN_COMPONENT = "com.android.component"
    public static String PLUGIN_MAVEN_PUBLISH = 'maven-publish'
    public static String PUBLISHING = 'publishing'
    public static String LOCAL_PROPERTIES = "local.properties"
    public static String CLEAN = 'clean'
    public static String PLUGIN_CACHE = "scan_cache"

    //file
    public static String DEFAULT_MAIN_MODULE_NAME = "main"
    //默认工程主项目名称
    public static String SDK_DIR = '.gradle/component/sdk'
    //sdk存放目录
    public static String IMPL_DIR = '.gradle/component/impl'
    //impl存放目录
    public static String BUILD_SDK_DIR = 'build/component/sdk'
    //生成的sdk文件的build目录
    public static String BUILD_IMPL_DIR = 'build/component/impl'
    //生成的impl的build目录
    //发布日志路径
    public static String PUBLISH_LOG_FILE = "componentpublish.log"
    public static String BUILD_SOURCE_DIR = 'source'
    public static String BUILD_CLASSES_DIR = 'classes'
    public static String BUILD_OUTPUT_DIR = 'outputs'
    public static String SDK_PRE = 'sdk-'
    public static String IMPL_PRE = 'impl-'
    public static String COMPONENT_PRE = 'component-'
    public static String DEBUG_COMPONENT_PRE = 'component(Debug)-'
    public static String CORE_DEPENDENCY = "com.effective.android:component-core:1.0.5"

    //sourceSet - default
    public static String JAVA_PATH = "src/main/java"
    public static String RES_PATH = "src/main/res"
    public static String ASSETS_PATH = "src/main/assets"
    public static String JNILIBS_PATH = "src/main/jniLibs"
    public static String MANIFEST = "src/main/AndroidManifest.xml"

    //git hooks
    public static String HOOKS_DIR = "hooks"
    public static String HOOKS_CONTENT =
"""#!/bin/sh

remote="\$1"
url="\$2"
branch=\$(git symbolic-ref --short -q HEAD)

strB="BUILD SUCCESSFUL"
echo "current branch:\$branch"
tempLog=/tmp/\$(date +%s).log && touch \$tempLog
if [[ "\$branch" =~ (^develop\$)|(^release/*) ]]; then
   ./gradlew ComponentPublishDebug | tee \$tempLog
   result=\$(cat \$tempLog)
   checkStr=\$(echo "\$result" | grep "\${strB}")
   if [[ "\$checkStr" != ""  ]]; then
     exit 0;
   else
     exit 1;
   fi
fi
exit 0;
"""
}