// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.net.interceptor;

import java.util.HashMap;

public class BaseParamsMap {
    private static HashMap<String, String> sMap = new HashMap<>();

    public static HashMap<String, String> get() {
        return sMap;
    }

    public static String get(String key) {
        if (sMap == null) return null;
        return sMap.get(key);
    }

    public static void put(String key, String value) {
        if (sMap == null) sMap = new HashMap<>();
        sMap.put(key, value);
    }

    public static void remove(String key) {
        if (sMap == null) return;
        sMap.remove(key);
    }
}
