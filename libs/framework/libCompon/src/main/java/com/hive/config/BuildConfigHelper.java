// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.config;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.util.HashMap;
import java.util.Map;

public class BuildConfigHelper {
    private static HashMap<String, String> sCacheMap;

    private static String BUILD_ORIGIN_JSON = "";

    public static String getMapString(String key) {
        Map<String, String> map = getMapList();
        if (map.get(key) == null) return null;
        return map.get(key);
    }

    public static String getMapString(String key,String defaultValue) {
        Map<String, String> map = getMapList();
        if (map.get(key) == null) return defaultValue;
        return map.get(key);
    }

    public static String getMapNoNullString(String key) {
        Map<String, String> map = getMapList();
        if (map.get(key) == null) return "";
        return map.get(key);
    }

    public static Integer getMapInteger(String key) {
        Map<String, String> map = getMapList();
        if (map.get(key) == null) return null;
        return Integer.parseInt(map.get(key));
    }

    public static Boolean getMapBoolean(String key) {
        Map<String, String> map = getMapList();
        if (map.get(key) == null) return null;
        return Boolean.parseBoolean(map.get(key));
    }

    public static Integer getMapInteger(String key,Integer defaultValue) {
        Map<String, String> map = getMapList();
        if (map.get(key) == null) return defaultValue;
        return Integer.parseInt(map.get(key));
    }

    public static Boolean getMapBoolean(String key,Boolean defaultValue) {
        Map<String, String> map = getMapList();
        if (map.get(key) == null) return defaultValue;
        return Boolean.parseBoolean(map.get(key));
    }

    /**
     * 使用 Gson 解析 JSON 格式的配置
     */
    public static Map<String, String> getMapList() {
        if (sCacheMap != null) return sCacheMap;

        sCacheMap = new HashMap<>();
        if (BUILD_ORIGIN_JSON == null || BUILD_ORIGIN_JSON.isEmpty()) {
            return sCacheMap;
        }

        try {
            // 使用 Gson 解析 JSON
            Gson gson = new Gson();
            Map<String, Object> rawMap = gson.fromJson(BUILD_ORIGIN_JSON, new TypeToken<Map<String, Object>>(){}.getType());
            if (rawMap != null) {
                for (Map.Entry<String, Object> entry : rawMap.entrySet()) {
                    Object value = entry.getValue();
                    // 将所有值转换为字符串（包括嵌套的 List/Map）
                    sCacheMap.put(entry.getKey(), value != null ? String.valueOf(value) : "");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            // 如果 JSON 解析失败，尝试旧的解析方式（兼容旧格式）
            parseLegacyFormat();
        }

        return sCacheMap;
    }

    /**
     * 旧格式解析（兼容 Groovy Map toString 格式）
     */
    private static void parseLegacyFormat() {
        try {
            String content = BUILD_ORIGIN_JSON;
            // 去掉开头的 [ 或 { 和结尾的 ] 或 }
            if (content.startsWith("[") || content.startsWith("{")) {
                content = content.substring(1, content.length() - 1);
            }

            String[] configArr = content.split(",");
            for (String item : configArr) {
                try {
                    int index = item.indexOf(":");
                    if (index > 0) {
                        String key = item.substring(0, index).replace(" ", "").trim();
                        String value = index < item.length() - 1 ? item.substring(index + 1).trim() : "";
                        sCacheMap.put(key, value);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void injectBuildConfig(String json) {
        BUILD_ORIGIN_JSON = json;
        sCacheMap = null; // 清除缓存，强制重新解析
    }
}
