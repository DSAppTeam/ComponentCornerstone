package com.plugin.module

class Logger {

    static void buildOutput(String log) {
        System.out.println(Constants.TAG + log)
    }

    static void buildOutput(Object start, Object end) {
        System.out.println(Constants.TAG + start + " = " + end)
    }
}
