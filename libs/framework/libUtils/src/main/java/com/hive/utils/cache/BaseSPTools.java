// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.utils.cache;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

/**
 * Created by kuaigeng01 on 2017/11/16.
 */
public class BaseSPTools {

    private String SP_NAME = "common_ps_bb";
    private SharedPreferences mSP;
    private SharedPreferences.Editor mEditor;
    private Context mApplicationContext;

    public BaseSPTools(Context context, String spName) {
        if (null == context || TextUtils.isEmpty(spName)) {
            throw new IllegalArgumentException("context and spName must be not null");
        }

        mApplicationContext = context.getApplicationContext();
        SP_NAME = spName;
    }


    public SharedPreferences getSP() {
        if (null == mSP) {
            if (null != mApplicationContext) {
                mSP = mApplicationContext.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
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

    public boolean contains(String key) {
        if (key == null) {
            return false;
        }

        SharedPreferences sp = getSP();
        if (null != sp) {
            return sp.contains(key);
        }

        return false;
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

            commit(immediately, editor);
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

            commit(immediately, editor);
        }
    }

    private void putInt(String key, int value, boolean immediately) {
        if (null == key) {
            return;
        }

        SharedPreferences.Editor editor = getEditor();
        if (null != editor) {
            editor.putInt(key, value);

            commit(immediately, editor);
        }
    }

    private void putLong(String key, long value, boolean immediately) {
        if (null == key) {
            return;
        }

        SharedPreferences.Editor editor = getEditor();
        if (null != editor) {
            editor.putLong(key, value);

            commit(immediately, editor);
        }
    }

    private void putFloat(String key, float value, boolean immediately) {
        if (null == key) {
            return;
        }

        SharedPreferences.Editor editor = getEditor();
        if (null != editor) {
            editor.putFloat(key, value);

            commit(immediately, editor);
        }
    }

    private void putString(String key, String value, boolean immediately) {
        if (null == key) {
            return;
        }

        SharedPreferences.Editor editor = getEditor();
        if (null != editor) {
            editor.putString(key, value);

            commit(immediately, editor);
        }
    }

    private void commit(boolean immediately, SharedPreferences.Editor editor) {
        if (immediately) {
            editor.commit();
        } else {
            try {
                editor.apply();
            } catch (AbstractMethodError unused) {
                editor.commit();
            }
        }
    }

}
