package com.plugin.component;

import com.plugin.component.extension.ComponentExtension;
import com.plugin.component.extension.PublicationManager;
import com.plugin.component.extension.option.PublicationOption;
import com.plugin.component.extension.option.DebugOption;
import com.plugin.component.extension.module.ProjectInfo;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 运行时数据
 * created by yummylau 2019/08/09
 */
public class PluginRuntime {

    public static Map<String, PublicationOption> sSdkPublicationMap = new HashMap<>();             //sdk 发布信息
    public static Map<String, PublicationOption> sImplPublicationMap = new HashMap<>();            //impl 发布信息
    public static Map<String, DebugOption> sDebugMap = new HashMap<>();                      //配置信息
    public static Map<String, ProjectInfo> sProjectInfoMap = new HashMap<>();                      //项目信息

    public static String sAndroidJarPath;                                                         //本地 android jar 路径
    public static PublicationManager sPublicationManager;
    public static ComponentExtension sModuleExtension;
    public static File sSdkDir;
    public static File sImplDir;


    public static void resetProjectInfoScript() {
        Set<String> keys = sProjectInfoMap.keySet();
        for (String key : keys) {
            ProjectInfo projectInfo = sProjectInfoMap.get(key);
            if (projectInfo != null && projectInfo.isVailModulePluginTarget) {
                projectInfo.afterEvaluateHandlerBuildScript();
            }
        }
    }
}
