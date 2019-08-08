package com.plugin.module.extension;

import com.plugin.module.extension.module.AloneConfiguration;
import com.plugin.module.extension.module.ProjectInfo;
import com.plugin.module.extension.publication.Publication;
import com.plugin.module.extension.publication.PublicationManager;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ModuleRuntime {

    public static Map<String, Publication> publicationMap = new HashMap<>();               //发布信息
    public static Map<String, AloneConfiguration> aloneRunMap = new HashMap<>();           //配置信息
    public static String androidJarPath;                                                   //本地 android jar 路径
    public static PublicationManager publicationManager;
    public static ModuleExtension sModuleExtension;
    public static File misDir;
    public static Map<String, ProjectInfo> projectInfos = new HashMap<>();

    public static void resetProjectInfoScript() {
        Set<String> keys = projectInfos.keySet();
        for (String key : keys) {
            ProjectInfo projectInfo = projectInfos.get(key);
            if (projectInfo != null && projectInfo.isVailModulePluginTarget) {
                projectInfo.afterEvaluateHandlerBuildScript();
            }
        }
    }
}
