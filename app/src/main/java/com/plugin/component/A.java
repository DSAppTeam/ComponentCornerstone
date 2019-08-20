package com.plugin.component;

import java.net.CacheRequest;

public class A {

    public void test() {
        String name = "aaaa";
        CostCache.start(name, System.currentTimeMillis());
        System.out.println( CostCache.cost(name));
    }
}
