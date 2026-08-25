// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.net;

import android.text.TextUtils;

import com.hive.net.engineer.EngineerConfig;
import com.hive.utils.utils.GsonHelper;

import java.util.HashMap;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.RequestBody;

public class NetHelper {
    public static String covertRes(String url) {
        if (TextUtils.isEmpty(url)) return null;
        if (url.contains("http")||url.contains("https")) {
            return url;
        }
        EngineerConfig config = EngineerConfig.read();
        if (config.engineerOn) {
            return config.resUrl + url;
        }
        return ApiDnsManager.getResDomain() + url;
    }

    public static String covertData(String url) {
        if (TextUtils.isEmpty(url)) return null;
        if (url.contains("http") || url.contains("https") || url.contains("file://")) {
            return url;
        }
        EngineerConfig config = EngineerConfig.read();
        if (config.engineerOn) {
            return config.dataUrl + url;
        }
        return ApiDnsManager.getDataDomain() + url;
    }

    public static String covertOther(String url) {
        if (TextUtils.isEmpty(url)) return null;
        if (url.contains("http")||url.contains("https")) {
            return url;
        }
        EngineerConfig config = EngineerConfig.read();
        if (config.engineerOn) {
            return config.otherUrl + url;
        }
        return ApiDnsManager.getOtherDomain() + url;
    }

    public static RequestBody getRequestBody(String paramName, String paramValue) {
        Map<String, String> map = new HashMap<>();
        map.put(paramName, paramValue);
        RequestBody body = RequestBody.create(MediaType.parse("text"), GsonHelper.getInstance().toJson(map));
        return body;
    }

    public static RequestBody getRequestBody(Map<String, String> map) {
        RequestBody body = RequestBody.create(MediaType.parse("text"), GsonHelper.getInstance().toJson(map));
        return body;
    }

    public static RequestBody getRequestBody(String[] args) {
        RequestBody body = RequestBody.create(MediaType.parse("text"), GsonHelper.getInstance().toJson(args));
        return body;
    }

    public static RequestBody getRequestBody(int[] args) {
        RequestBody body = RequestBody.create(MediaType.parse("text"), GsonHelper.getInstance().toJson(args));
        return body;
    }
    public static RequestBody getRequestBody(Object args) {
        RequestBody body = RequestBody.create(MediaType.parse("text"), GsonHelper.getInstance().toJson(args));
        return body;
    }
}
