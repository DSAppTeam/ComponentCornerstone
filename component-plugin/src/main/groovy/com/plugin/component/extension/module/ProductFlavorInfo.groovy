package com.plugin.component.extension.module

import com.android.build.gradle.BaseExtension
import com.plugin.component.utils.PinUtils
import org.gradle.api.Project

class ProductFlavorInfo {

    List<String> flavorDimensions
    List<String> productFlavors
    List<String> buildTypes
    List<String> combinedProductFlavors
    Map<String, List<String>> combinedProductFlavorsMap
    boolean singleDimension

    private List<List<String>> flavorGroups

    ProductFlavorInfo(Project project) {
        BaseExtension extension = (BaseExtension) project.extensions.getByName("android")
        buildTypes = new ArrayList<>()
        if(extension.buildTypes != null) {
            extension.buildTypes.each {
                buildTypes.add(it.name)
            }
        }

        flavorDimensions = extension.flavorDimensionList
        if (flavorDimensions == null) {
            flavorDimensions = new ArrayList<>()
        }

        productFlavors = new ArrayList<>()
        flavorGroups = new ArrayList<>()
        for (int i = 0; i < flavorDimensions.size(); i++) {
            flavorGroups.add(new ArrayList<>())
        }
        extension.productFlavors.each {
            productFlavors.add(it.name)
            def position = flavorDimensions.indexOf(it.dimension)
            flavorGroups.get(position).add(it.name)
        }
        List<List<String>> flavorGroupTemp = new ArrayList<>()
        flavorGroups.each {
            if (it.size() != 0) {
                flavorGroupTemp.add(it)
            }
        }
        flavorGroups = flavorGroupTemp

        calculateFlavorCombination()
        if (combinedProductFlavors.size() == extension.productFlavors.size()) {
            singleDimension = true
        }
    }

    private void calculateFlavorCombination() {
        combinedProductFlavors = new ArrayList<>()
        combinedProductFlavorsMap = new HashMap<>()

        if (flavorGroups.size() == 0) {
            return
        }

        List<Integer> combination = new ArrayList<Integer>()
        int n = flavorGroups.size();
        for (int i = 0; i < n; i++) {
            combination.add(0);
        }
        int i = 0;
        boolean isContinue = true;
        while (isContinue) {
            List<String> items = new ArrayList<>()
            String item = flavorGroups.get(0).get(combination.get(0))
            items.add(item)
            String combined = item
            for (int j = 1; j < n; j++) {
                item = flavorGroups.get(j).get(combination.get(j))
                combined += PinUtils.upperCase(item)
                items.add(item)
            }
            combinedProductFlavors.add(combined)
            combinedProductFlavorsMap.put(combined, items)
            i++;
            combination.set(n - 1, i);
            for (int j = n - 1; j >= 0; j--) {
                if (combination.get(j) >= flavorGroups.get(j).size()) {
                    combination.set(j, 0);
                    i = 0;
                    if (j - 1 >= 0) {
                        combination.set(j - 1, combination.get(j - 1) + 1);
                    }
                }
            }
            isContinue = false;
            for (Integer integer : combination) {
                if (integer != 0) {
                    isContinue = true;
                }
            }
        }
    }

}