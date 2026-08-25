// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.utils.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class GsonHelper {
    private static GsonHelper sInstance;
    private Gson mGson;

    public static GsonHelper getInstance() {
        synchronized (GsonHelper.class) {
            if (null == sInstance) {
                synchronized (GsonHelper.class) {
                    if (null == sInstance) {
                        sInstance = new GsonHelper();
                    }
                }
            }
        }
        return sInstance;
    }

    public GsonHelper() {
        mGson = GsonWrapper.buildGson();
    }

    public String toJson(Object object) {
        return mGson.toJson(object);
    }

    public String toJson(Object object, Type typeOfT) {
        return mGson.toJson(object, typeOfT);
    }

    public Gson getGson() {
        return mGson;
    }

    public <T> List<T> fromListJson(String json, Class<T> tClass) {
        List<T> list = new ArrayList<T>();
        try {
            Gson gson = new Gson();
            JsonArray arry = new JsonParser().parse(json).getAsJsonArray();
            for (JsonElement jsonElement : arry) {
                list.add(gson.fromJson(jsonElement, tClass));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public <T> T fromJson(String json, Class<T> tClass) {
        return mGson.fromJson(json, tClass);
    }

    public <T> T fromJson(String json, Type typeOfT) {
        return mGson.fromJson(json, typeOfT);
    }

    public String toFormatJson(Object obj) {
        if (obj == null) return "";
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(obj);
    }

    public static String toFormatJsonString(String json) {
        try {
            Gson gson = new GsonBuilder()
                    .setPrettyPrinting()
                    .disableHtmlEscaping()
                    .create();
            return gson.toJson(new JsonParser().parse(json));
        } catch (Exception e) {
            return json;
        }
    }
}
