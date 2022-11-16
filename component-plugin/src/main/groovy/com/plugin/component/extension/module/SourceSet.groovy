package com.plugin.component.extension.module

/**
 * 资源组
 * created by yummylau 2019/08/09
 */
class SourceSet {
    String path                                             // 路径
    Map<String, SourceFile> lastModifiedSourceFile          // 源码修改的资源集
    Long quickVerifyModified                                // 文件是否快速校验
    String gitVersion                                         //当前git版本
    String gitCommitInfo                                    //git提交信息
    String commitUser                                      //git提交者
    String commitTime                                      //git提交时间
}
