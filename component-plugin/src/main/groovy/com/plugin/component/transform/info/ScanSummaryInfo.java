package com.plugin.component.transform.info;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * author : linzheng
 * e-mail : linzheng@corp.netease.com
 * time   : 2019/10/22
 * desc   :
 * version: 1.0
 */
public class ScanSummaryInfo {


    public String inputFilePath;

    public String outputFilePath;

    Map<String, Set<ScanSdkInfo>> updateSdkMap = new HashMap<>();

    Map<String, Set<ScanComponentInfo>> updateComponentMap = new HashMap<>();


    Set<String> removedFileSet = new HashSet<>();







}