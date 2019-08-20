package com.plugin.component.asm

import com.android.annotations.NonNull
import com.android.annotations.Nullable
import com.android.builder.sdk.SdkInfo
import com.plugin.component.Logger

class ScanRuntime {


    static Set<String> sMethodCostInfo = new HashSet<>()
    static List<ScanComponentInfo> sComponentInfo = new ArrayList<>()
    static List<ScanSdkInfo> sSdkInfo = new ArrayList<>()
    static List<ComponentSdkInfo> componentSdkInfoList = new ArrayList<>()

    static void addCostMethod(String className, String methodName, String descriptor) {
        if (descriptor == null) {
            descriptor = ""
        }
        sMethodCostInfo.add(className + "#" + methodName + "(" + descriptor + ")")
    }

    @Nullable
    static boolean isCostMethod(String className, String methodName, String descriptor) {
        if (descriptor == null) {
            descriptor = ""
        }
        return sMethodCostInfo.contains(className + "#" + methodName + "(" + descriptor + ")")
    }

    static void addComponentInfo(@NonNull ScanComponentInfo scanComponentInfo) {
        sComponentInfo.add(scanComponentInfo)
    }

    static void addSdkInfo(@NonNull ScanSdkInfo scanSdkInfo) {
        sSdkInfo.add(scanSdkInfo)
    }

    static void logScanInfo() {
        for (String string : sMethodCostInfo) {
            Logger.buildOutput("MethodCost ==> " + string)
        }

        for (ScanSdkInfo sdkInfo : sSdkInfo) {
            Logger.buildOutput(sdkInfo.toString())
        }

        for (ScanComponentInfo scanComponentInfo : sComponentInfo) {
            Logger.buildOutput(scanComponentInfo.toString())
        }
    }

    static void loadInjectInfo() {
        for (ComponentSdkInfo componentSdkInfo : componentSdkInfoList) {
            Logger.buildOutput(componentSdkInfo.toString())
        }
    }

    static void clearScanInfo() {
        sMethodCostInfo.clear()
        sSdkInfo.clear()
        sComponentInfo.clear()
    }

    static void buildComponentSdkInfo() {
        componentSdkInfoList.clear()
        for (ScanSdkInfo scanSdkInfo : sSdkInfo) {
            if (scanSdkInfo.sdk != null && !scanSdkInfo.sdk.isEmpty()) {
                for (String sdk : scanSdkInfo.sdk) {
                    ComponentSdkInfo componentSdkInfo = new ComponentSdkInfo()
                    componentSdkInfo.sdk = sdk
                    componentSdkInfo.impl = scanSdkInfo.className
                    componentSdkInfoList.add(componentSdkInfo)
                }
            }
        }
        for (ScanComponentInfo scanComponentInfo : sComponentInfo) {
            if (scanComponentInfo.impl != null && !scanComponentInfo.impl.isEmpty()) {
                for (ComponentSdkInfo componentSdkInfo : componentSdkInfoList) {
                    for (String impl : scanComponentInfo.impl) {
                        if (impl.contains(componentSdkInfo.impl)) {
                            componentSdkInfo.impl = impl
                            componentSdkInfo.componentClassName = "L" + scanComponentInfo.className + ";"
                        }
                    }
                }
            }
        }
    }

    static List<ComponentSdkInfo> getComponentSdkInfoList() {
        return componentSdkInfoList
    }
}
