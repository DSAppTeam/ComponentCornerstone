package com.plugin.component.transform.info

import com.android.annotations.NonNull
import com.plugin.component.log.Logger

class ScanRuntime {


    static List<ScanComponentInfo> sComponentInfo = new ArrayList<>()
    static List<ScanSdkInfo> sSdkInfo = new ArrayList<>()
    static List<ComponentSdkInfo> componentSdkInfoList = new ArrayList<>()

    static ScanSummaryInfo sSummaryInfo = new ScanSummaryInfo()


    static void addComponentInfo(@NonNull ScanComponentInfo scanComponentInfo) {
        sComponentInfo.add(scanComponentInfo)
    }

    static void addSdkInfo(@NonNull ScanSdkInfo scanSdkInfo) {
        sSdkInfo.add(scanSdkInfo)
    }

    static void logScanInfo() {
        Logger.buildOutput("Dodge sdk info size = " + sSdkInfo.size())
        for (ScanSdkInfo sdkInfo : sSdkInfo) {
            Logger.buildOutput(sdkInfo.toString())
        }

        Logger.buildOutput("Dodge  Component Info size = " + sComponentInfo.size())
        for (ScanComponentInfo scanComponentInfo : sComponentInfo) {
            Logger.buildOutput(scanComponentInfo.toString())
        }
    }

    static void logInjectInfo() {
        for (ComponentSdkInfo componentSdkInfo : componentSdkInfoList) {
            Logger.buildOutput(componentSdkInfo.toString())
        }
    }

    static void clearScanInfo() {
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


    static void addSdkInfo(String filePath, @NonNull ScanSdkInfo scanSdkInfo) {
        Set<ScanSdkInfo> set = sSummaryInfo.updateSdkMap.get(filePath)
        if (set == null) {
            set = new HashSet<>()
            sSummaryInfo.updateSdkMap.put(filePath, set)
        }
        set.add(scanSdkInfo)
    }

    static void addComponentInfo(String filePath, @NonNull ScanComponentInfo componentInfo) {
        Set<ScanComponentInfo> set = sSummaryInfo.updateComponentMap.get(filePath)
        if (set == null) {
            set = new HashSet<>()
            sSummaryInfo.updateComponentMap.put(filePath, set)
        }
        set.add(componentInfo)
    }


    static void removedFile(String path) {
        sSummaryInfo.removedFileSet.add(path)
    }

    static ScanSummaryInfo updateSummaryInfo(ScanSummaryInfo cacheSummary) {
        if (sSummaryInfo.inputFilePath != null) {
            cacheSummary.inputFilePath = sSummaryInfo.inputFilePath
        }

        if (sSummaryInfo.outputFilePath != null) {
            cacheSummary.outputFilePath = sSummaryInfo.outputFilePath
        }

        sSummaryInfo.removedFileSet.each {
            cacheSummary.updateSdkMap.remove(it)
            cacheSummary.updateComponentMap.remove(it)
        }

        sSummaryInfo.updateSdkMap.each {
            cacheSummary.updateSdkMap.put(it.key, it.value)
        }

        sSummaryInfo.updateComponentMap.each {
            cacheSummary.updateComponentMap.put(it.key, it.value)
        }

        cacheSummary.updateSdkMap.each {
            if (it != null && !it.value.isEmpty()) {
                sSdkInfo.addAll(it.value)
            }
        }

        cacheSummary.updateComponentMap.each {
            if (it != null && !it.value.isEmpty()) {
                sComponentInfo.addAll(it.value)
            }
        }

        return cacheSummary

    }

    static void clearSummaryInfo() {
        // TODO clear data
        sSummaryInfo = new ScanSummaryInfo()

    }


}
