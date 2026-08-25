// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.utils.global;

import android.text.TextUtils;

import com.google.gson.Gson;
import com.hive.utils.GlobalApp;
import com.hive.utils.encrypt.AESCrypt;
import com.hive.utils.utils.GsonHelper;
import com.tencent.mmkv.MMKV;

import java.security.GeneralSecurityException;

/**
 * mmkv help tools
 * <p/>
 */
public class MMKVTools {

    private MMKV mmkv;
    private MMKV.Editor editor;


    private MMKVTools() {
        MMKV.initialize(GlobalApp.getContext());
    }

    private static class SingleHolder {
        static MMKVTools instance = new MMKVTools();
    }

    public static MMKVTools getInstance() {
        if (null == SingleHolder.instance) {
            synchronized (MMKVTools.class) {
                if (null == SingleHolder.instance) {
                    SingleHolder.instance = new MMKVTools();
                }
            }
        }

        return SingleHolder.instance;
    }

    public MMKV getSP() {
        if (null == mmkv) {
            mmkv = MMKV.defaultMMKV();
        }
        return mmkv;
    }

    /**
     * 脚本参数持久化：读取（使用默认 MMKV，与 chat_context 同存储，确保持久化生效）
     */
    public static String getScriptParamString(String key, String defaultValue) {
        return getInstance().getString(key, defaultValue);
    }

    /**
     * 脚本参数持久化：写入（使用默认 MMKV，立即提交确保重启不丢失）
     */
    public static void putScriptParamString(String key, String value) {
        getInstance().putStringImmediately(key, value);
    }

    /**
     * 获取所有以指定前缀开头的 key
     */
    public static String[] getKeysWithPrefix(String prefix) {
        MMKV sp = getInstance().getSP();
        if (sp == null || prefix == null) return new String[0];
        String[] all = sp.allKeys();
        if (all == null) return new String[0];
        java.util.ArrayList<String> result = new java.util.ArrayList<>();
        for (String k : all) {
            if (k != null && k.startsWith(prefix)) {
                result.add(k);
            }
        }
        return result.toArray(new String[0]);
    }

    private MMKV.Editor getEditor() {
        if (null == editor) {
            MMKV sp = getSP();
            if (null != sp) {
                editor = sp.edit();
            }
        }
        return editor;
    }

    public boolean getBoolean(String key, boolean defaultVal) {
        if (key == null) {
            return defaultVal;
        }

        MMKV sp = getSP();
        if (null != sp) {
            return sp.getBoolean(key, defaultVal);
        }

        return defaultVal;
    }

    public int getInt(String key, int defaultVal) {
        if (key == null) {
            return defaultVal;
        }

        MMKV sp = getSP();
        if (null != sp) {
            return sp.getInt(key, defaultVal);
        }

        return defaultVal;
    }

    public long getLong(String key, long defaultVal) {
        if (key == null) {
            return defaultVal;
        }

        MMKV sp = getSP();
        if (null != sp) {
            return sp.getLong(key, defaultVal);
        }

        return defaultVal;
    }

    public float getFloat(String key, float defaultVal) {
        if (key == null) {
            return defaultVal;
        }

        MMKV sp = getSP();
        if (null != sp) {
            return sp.getFloat(key, defaultVal);
        }

        return defaultVal;
    }

    public String getString(String key, String defaultVal) {
        if (key == null) {
            return defaultVal;
        }

        MMKV sp = getSP();
        if (null != sp) {
            return sp.getString(key, defaultVal);
        }

        return defaultVal;
    }

    public void clear() {
        MMKV.Editor editor = getEditor();
        if (null != editor) {
            editor.clear();
            editor.commit();
        }
    }

    public void remove(String key) {
        remove(key, false);
    }

    public void remove(String key, boolean immediately) {
        if (null == key) {
            return;
        }

        MMKV.Editor editor = getEditor();
        if (null != editor) {
            editor.remove(key);

            if (immediately) {
                editor.commit();
            } else {
                editor.apply();
            }
        }
    }

    public void putBooleanImmediately(String key, boolean value) {
        putBoolean(key, value, true);
    }

    public void putIntImmediately(String key, int value) {
        putInt(key, value, true);
    }

    public void putLongImmediately(String key, long value) {
        putLong(key, value, true);
    }

    public void putFloatImmediately(String key, float value) {
        putFloat(key, value, true);
    }

    public void putStringImmediately(String key, String value) {
        putString(key, value, true);
    }


    public void putBoolean(String key, boolean value) {
        putBoolean(key, value, false);
    }

    public void putInt(String key, int value) {
        putInt(key, value, false);
    }

    public void putLong(String key, long value) {
        putLong(key, value, false);
    }

    public void putFloat(String key, float value) {
        putFloat(key, value, false);
    }

    public void putString(String key, String value) {
        putString(key, value, false);
    }

    //---------------------------------------------------------------------

    private void putBoolean(String key, boolean value, boolean immediately) {
        if (null == key) {
            return;
        }

        MMKV.Editor editor = getEditor();
        if (null != editor) {
            editor.putBoolean(key, value);

            if (immediately) {
                editor.commit();
            } else {
                editor.apply();
            }
        }
    }

    private void putInt(String key, int value, boolean immediately) {
        if (null == key) {
            return;
        }

        MMKV.Editor editor = getEditor();
        if (null != editor) {
            editor.putInt(key, value);

            if (immediately) {
                editor.commit();
            } else {
                editor.apply();
            }
        }
    }

    private void putLong(String key, long value, boolean immediately) {
        if (null == key) {
            return;
        }

        MMKV.Editor editor = getEditor();
        if (null != editor) {
            editor.putLong(key, value);

            if (immediately) {
                editor.commit();
            } else {
                editor.apply();
            }
        }
    }

    private void putFloat(String key, float value, boolean immediately) {
        if (null == key) {
            return;
        }

        MMKV.Editor editor = getEditor();
        if (null != editor) {
            editor.putFloat(key, value);

            if (immediately) {
                editor.commit();
            } else {
                editor.apply();
            }
        }
    }

    private void putString(String key, String value, boolean immediately) {
        if (null == key) {
            return;
        }

        MMKV.Editor editor = getEditor();
        if (null != editor) {
            editor.putString(key, value);

            if (immediately) {
                editor.commit();
            } else {
                editor.apply();
            }
        }
    }


    public <T> boolean putObj(T entity) {
        return putObj(entity, null, null);
    }

    public <T> boolean putObj(T entity, String saveName) {
        return putObj(entity, saveName, null);
    }

    public <T> boolean putObj(T entity, String saveName, String encryptKey) {
        try {
            MMKV.Editor editor = getEditor();
            String objString = GsonHelper.getInstance().toJson(entity);
            if (!TextUtils.isEmpty(encryptKey)) {
                String encryptedMsg = AESCrypt.encrypt(encryptKey, objString);
                editor.putString(saveName != null ? saveName : entity.getClass().getName(), encryptedMsg);
            } else {
                editor.putString(saveName != null ? saveName : entity.getClass().getName(), objString);
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

    public <T> T getList(Class<T> classType) {
        return getList(classType, null, null);
    }


    public <T> T getList(Class<T> classType, String saveName) {
        return getList(classType, saveName, null);
    }

    public <T> T getList(Class<T> classType, String saveName, String encryptKey) {
        T list = null;
        try {
            String obj = getString(saveName != null ? saveName : classType.getName(), null);
            if (!TextUtils.isEmpty(obj)) {
                if (!TextUtils.isEmpty(encryptKey)) {
                    String messageAfterDecrypt = AESCrypt.decrypt(encryptKey, obj);
                    list = (new Gson()).fromJson(messageAfterDecrypt, classType);
                } else {
                    list = (new Gson()).fromJson(obj, classType);
                }
            }
        } catch (Exception var9) {
            var9.printStackTrace();
        }

        return list;
    }

    public <T> T getObj(Class<T> classType) {
        return getObj(classType, null, null);
    }


    public <T> T getObj(Class<T> classType, String saveName) {
        return getObj(classType, saveName, null);
    }

    public <T> T getObj(Class<T> classType, String saveName, String encryptKey) {
        T obj = null;
        try {
            String data = getString(saveName != null ? saveName : classType.getName(), null);
            if (!TextUtils.isEmpty(data)) {
                if (!TextUtils.isEmpty(encryptKey)) {
                    String json = AESCrypt.decrypt(encryptKey, data);
                    obj = new Gson().fromJson(json, classType);
                } else {
                    Gson gson2 = new Gson();
                    obj = gson2.fromJson(data, classType);
                }
            }
        } catch (Exception var10) {
            var10.printStackTrace();
        }
        return obj;
    }
}
