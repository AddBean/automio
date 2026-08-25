// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.utils.global;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

/**
 * sharedPreference help tools
 * <p/>
 */
public class SPTools {

    // key start =============================================================
    /**
     * 设备唯一id
     */
    public static final String UUID = "uuid";
    public static final String UUID_miaoPai = "mpuuid";
    /**
     * 设备唯一id 辅助-随机数
     */
    public static final String UUID_RANDOM = "uuid_random";
    public static String HardWareInfo = "HardWareInfo";

    public final static String SP_KEY_YOUNG_MODE = "sp_key_young_mode";

    private String SP_NAME = "base_sp";
    private SharedPreferences mSP;
    private SharedPreferences.Editor mEditor;



    private SPTools() {

    }

    public SPTools(Context context, String spName) {
        if (null == context || TextUtils.isEmpty(spName)) {
            throw new IllegalArgumentException("context and spName must be not null");
        }
        SP_NAME = spName;
    }

    private static class SingleHolder {
        static SPTools instance = new SPTools();
    }

    public static SPTools getInstance() {
        if (null == SingleHolder.instance) {
            synchronized (SPTools.class) {
                if (null == SingleHolder.instance) {
                    SingleHolder.instance = new SPTools();
                }
            }
        }

        return SingleHolder.instance;
    }

    public SharedPreferences getSP() {
        if (null == mSP) {
            Context ctx = VolleyGlobal.getGlobalContext();
            if (null != ctx) {
                mSP = ctx.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
            }
        }
        return mSP;
    }

    private SharedPreferences.Editor getEditor() {
        if (null == mEditor) {
            SharedPreferences sp = getSP();
            if (null != sp) {
                mEditor = sp.edit();
            }
        }
        return mEditor;
    }

    public boolean getBoolean(String key, boolean defaultVal) {
        if (key == null) {
            return defaultVal;
        }

        SharedPreferences sp = getSP();
        if (null != sp) {
            return sp.getBoolean(key, defaultVal);
        }

        return defaultVal;
    }

    public int getInt(String key, int defaultVal) {
        if (key == null) {
            return defaultVal;
        }

        SharedPreferences sp = getSP();
        if (null != sp) {
            return sp.getInt(key, defaultVal);
        }

        return defaultVal;
    }

    public long getLong(String key, long defaultVal) {
        if (key == null) {
            return defaultVal;
        }

        SharedPreferences sp = getSP();
        if (null != sp) {
            return sp.getLong(key, defaultVal);
        }

        return defaultVal;
    }

    public float getFloat(String key, float defaultVal) {
        if (key == null) {
            return defaultVal;
        }

        SharedPreferences sp = getSP();
        if (null != sp) {
            return sp.getFloat(key, defaultVal);
        }

        return defaultVal;
    }

    public String getString(String key, String defaultVal) {
        if (key == null) {
            return defaultVal;
        }

        SharedPreferences sp = getSP();
        if (null != sp) {
            return sp.getString(key, defaultVal);
        }

        return defaultVal;
    }

    public void clear() {
        SharedPreferences.Editor editor = getEditor();
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

        SharedPreferences.Editor editor = getEditor();
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

        SharedPreferences.Editor editor = getEditor();
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

        SharedPreferences.Editor editor = getEditor();
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

        SharedPreferences.Editor editor = getEditor();
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

        SharedPreferences.Editor editor = getEditor();
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

        SharedPreferences.Editor editor = getEditor();
        if (null != editor) {
            editor.putString(key, value);

            if (immediately) {
                editor.commit();
            } else {
                editor.apply();
            }
        }
    }
}
