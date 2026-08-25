// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.utils.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.google.gson.Gson;
import com.hive.utils.encrypt.AESCrypt;

import java.security.GeneralSecurityException;

/**
 * 2016年1月6日添加了对数据保存和读取加密操作
 */

public class PreferencesUtils {


    public static <T> boolean saveObj(Context context, String savingName, T entity, String key) {
        try {
            SharedPreferences share = context.getSharedPreferences(savingName, 0);
            SharedPreferences.Editor editor = share.edit();
            String objString = GsonHelper.getInstance().toJson(entity);
            if (!TextUtils.isEmpty(key)) {
                String encryptedMsg = AESCrypt.encrypt(key, objString);
                editor.putString(entity.getClass().getName(), encryptedMsg);
            } else {
                editor.putString(entity.getClass().getName(), objString);
            }

            editor.commit();
            return true;
        } catch (GeneralSecurityException var10) {
            var10.printStackTrace();
            return false;
        } catch (Exception var11) {
            return false;
        }
    }

    public static <T> T getList(Context context, String savingName, Class<T> classType, String key) {
        T list = null;

        try {
            SharedPreferences share = context.getSharedPreferences(savingName, 0);
            String obj = share.getString(classType.getName(), (String) null);
            if (!TextUtils.isEmpty(obj)) {
                if (!TextUtils.isEmpty(key)) {
                    String messageAfterDecrypt = AESCrypt.decrypt(key, obj);
                    list = GsonHelper.getInstance().getGson().fromJson(messageAfterDecrypt, classType);
                } else {
                    list = GsonHelper.getInstance().getGson().fromJson(obj, classType);
                }
            }
        } catch (Exception var9) {
            var9.printStackTrace();
        }

        return list;
    }

    public static <T> T getObj(Context context, String savingName, Class<T> classType, String key) {
        T obj = null;

        try {
            SharedPreferences share = context.getSharedPreferences(savingName, 0);
            String data = share.getString(classType.getName(), (String) null);
            if (!TextUtils.isEmpty(data)) {
                if (!TextUtils.isEmpty(key)) {
                    String json = AESCrypt.decrypt(key, data);
                    obj = GsonHelper.getInstance().getGson().fromJson(json, classType);
                } else {
                    obj = GsonHelper.getInstance().getGson().fromJson(data, classType);
                }
            }
        } catch (Exception var10) {
            var10.printStackTrace();
        }

        return obj;
    }

    public static boolean clean(Context context, String savingName) {
        try {
            SharedPreferences settings = context.getSharedPreferences(savingName, 0);
            settings.edit().clear().commit();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

}