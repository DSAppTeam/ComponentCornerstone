package com.plugin.component.asm;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.plugin.component.Logger;

import org.gradle.internal.impldep.com.google.api.client.util.ArrayMap;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class ScanRuntime {

    static Set<String> sMethodCostInfo = new HashSet<>();
    static List<ScanComponentInfo> sComponentInfo = new ArrayList<>();
    static List<ScanSdkInfo> sSdkInfo = new ArrayList<>();

    public static void addCostMethod(String className, String methodName, String descriptor) {
        if (descriptor == null) {
            descriptor = "";
        }
        sMethodCostInfo.add(className + "#" + methodName + "(" + descriptor + ")");
    }

    @Nullable
    public static boolean isCostMethod(String className, String methodName, String descriptor) {
        if (descriptor == null) {
            descriptor = "";
        }
        return sMethodCostInfo.contains(className + "#" + methodName + "(" + descriptor + ")");
    }

    public static void addComponentInfo(@NonNull ScanComponentInfo scanComponentInfo) {
        sComponentInfo.add(scanComponentInfo);
    }

    public static void addSdkInfo(@NonNull ScanSdkInfo scanSdkInfo) {
        sSdkInfo.add(scanSdkInfo);
    }
    
    public static void logScanInfo() {
        for (String string : sMethodCostInfo) {
            Logger.buildOutput("MethodCost ==> " + string);
        }
        for (ScanComponentInfo scanComponentInfo : sComponentInfo) {
            Logger.buildOutput(scanComponentInfo.toString());
        }
    }

}
