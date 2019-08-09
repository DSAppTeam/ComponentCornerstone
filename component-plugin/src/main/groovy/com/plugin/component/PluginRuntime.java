package com.plugin.component;

import com.plugin.component.extension.ComponentExtension;
import com.plugin.component.extension.PublicationManager;
import com.plugin.component.extension.option.PublicationOption;
import com.plugin.component.extension.option.RunAloneOption;
import com.plugin.component.extension.module.ProjectInfo;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class PluginRuntime {

    public static Map<String, PublicationOption> sPublicationMap = new HashMap<>();               //发布信息
    public static Map<String, RunAloneOption> sRunAloneMap = new HashMap<>();                     //配置信息
    public static String sAndroidJarPath;                                                         //本地 android jar 路径
    public static PublicationManager sPublicationManager;
    public static ComponentExtension sModuleExtension;
    public static File sSdkDir;
    public static Map<String, ProjectInfo> sProjectInfoMap = new HashMap<>();

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
