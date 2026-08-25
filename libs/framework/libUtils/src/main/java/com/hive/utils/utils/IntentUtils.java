// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.utils.utils;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;

import com.hive.utils.debug.DLog;

import java.io.Serializable;
import java.util.ArrayList;

/***
 * 获取Intent  数据，防止getXXXExtra漏洞
 * 漏洞可参看：http://blogs.360.cn/blog/android-app%E9%80%9A%E7%94%A8%E5%9E%8B%E6%8B%92%E7%BB%9D%E6%9C%8D%E5%8A%A1%E6%BC%8F%E6%B4%9E%E5%88%86%E6%9E%90%E6%8A%A5%E5%91%8A/
 */
public class IntentUtils {

    public static void sendMediaScanFile(Context context, String path) {
        DLog.d("IntentUtils", "sendMediaScanFile : " + path);
        //微信并没有读取出来这个数据，看来要写进媒体数据库
        Uri localUri = Uri.parse("file://" + path);
        Intent localIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        localIntent.setData(localUri);
        context.sendBroadcast(localIntent);
    }

    public static boolean callPhone(Context context,Uri uri) {
        Intent intent = new Intent(Intent.ACTION_DIAL, uri);
        return IntentUtils.safeStartActivity(context,intent);
    }

    public static void openSystemSetting(Context context) {
        Intent intent = new Intent(android.provider.Settings.ACTION_SETTINGS);
        safeStartActivity(context, intent);
    }

    /***
     * 获取boolean类型的值
     * @param intent
     * @param name
     * @param defaultValue
     * @return
     */
    public static final boolean getBooleanExtra(Intent intent, String name, boolean defaultValue) {
        boolean val = defaultValue;
        try {
            val = intent.getBooleanExtra(name, defaultValue);
        } catch (Exception e) {
        }
        return val;
    }

    /**
     * 获取String类型的值
     *
     * @param intent
     * @param name
     * @return
     */
    public static final String getStringExtra(Intent intent, String name) {
        String val = null;
        try {
            val = intent.getStringExtra(name);
        } catch (Exception e) {
        }
        return val;
    }

    /**
     * 获取String类型的值
     *
     * @param bundle
     * @param name
     * @return
     */
    public static final String getStringExtra(Bundle bundle, String name) {
        String val = null;
        try {
            val = bundle.getString(name);
        } catch (Exception e) {
        }
        return val;
    }

    /**
     * 获取String类型的值
     *
     * @param bundle
     * @param key
     * @param defaultValue
     * @return
     */
    public static String getStringExtra(Bundle bundle, String key, String defaultValue) {
        String val;
        try {
            val = bundle.getString(key);
        } catch (Exception e) {
            val = defaultValue;
        }

        if (val == null) {
            val = defaultValue;
        }

        return val;
    }


    public static boolean getBooleanExtra(Bundle bundle, String key, boolean defaultValue) {
        boolean result = defaultValue;
        try {
            return (result = bundle.getBoolean(key, defaultValue));
        } catch (Exception e) {
        }

        return result;
    }

    /***
     * 获取int类型的值
     * @param intent
     * @param name
     * @param defaultValue
     * @return
     */
    public static final int getIntExtra(Intent intent, String name, int defaultValue) {
        int val = defaultValue;
        try {
            val = intent.getIntExtra(name, defaultValue);
        } catch (Exception e) {
        }
        return val;
    }

    /***
     * 获取int类型的值
     * @param bundle
     * @param name
     * @param defaultValue
     * @return
     */
    public static final int getIntExtra(Bundle bundle, String name, int defaultValue) {
        int val = defaultValue;
        try {
            val = bundle.getInt(name, defaultValue);
        } catch (Exception e) {
        }
        return val;
    }

    public static final Serializable getSerializableExtra(Bundle bundle, String name) {
        Serializable val = null;
        try {
            val = bundle.getSerializable(name);
        } catch (Exception e) {
        }
        return val;
    }

    /**
     * 获取Serializable类型的值
     *
     * @param intent
     * @param name
     * @return
     */
    public static final Serializable getSerializableExtra(Intent intent, String name) {
        Serializable val = null;
        try {
            val = intent.getSerializableExtra(name);
        } catch (Exception e) {
        }
        return val;
    }

    /**
     * 获取Parcelable类型的值
     *
     * @param intent
     * @param name
     * @return
     */
    public static Parcelable getParcelableExtra(Intent intent, String name) {
        Parcelable val = null;
        try {
            val = intent.getParcelableExtra(name);
        } catch (Exception e) {
        }
        return val;
    }

    /***
     * 获取 long 类型的值
     * @param intent
     * @param name
     * @param defaultValue
     * @return
     */
    public static final long getLongExtra(Intent intent, String name, long defaultValue) {
        long val = defaultValue;
        try {
            val = intent.getLongExtra(name, defaultValue);
        } catch (Exception e) {
        }
        return val;
    }

    /***
     * 获取 byte[] 类型的值
     * @param intent
     * @param name
     * @return
     */
    public static final byte[] getByteArrayExtra(Intent intent, String name) {
        byte[] val = null;
        try {
            val = intent.getByteArrayExtra(name);
        } catch (Exception e) {
        }
        return val;
    }

    /**
     * 获取ArrayList<String>值
     *
     * @param intent
     * @param name
     * @return
     */
    public static final ArrayList<String> getStringArrayListExtra(Intent intent, String name) {
        ArrayList<String> val = null;
        try {
            val = intent.getStringArrayListExtra(name);
        } catch (Exception e) {
        }
        return val;
    }

    /**
     * 获取 Bundle 类型的值
     *
     * @param intent
     * @param name
     * @return
     */
    public static final Bundle getBundleExtra(Intent intent, String name) {
        Bundle bundle = null;
        try {
            bundle = intent.getBundleExtra(name);
        } catch (Exception e) {
        }
        return bundle;
    }

    public static final Bundle getBundleExtra(Bundle intent, String name) {
        Bundle bundle = null;
        try {
            bundle = intent.getBundle(name);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return bundle;
    }

    public static  boolean safeStartService(final Context context, final Intent intent) {
        if (context == null || intent == null) {
            return false;
        }
        try {
//            if (!(context instanceof Activity)) {
//                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//            }
            context.startService(intent);
            return true;
        } catch (ActivityNotFoundException | SecurityException e) {
            DLog.d("locks", e.toString());
        }
        return false;
    }

    public static  boolean safeStartActivity(final Context context, final Intent intent) {
        if (context == null || intent == null) {
            return false;
        }
        try {
            if (!(context instanceof Activity)) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            }
            context.startActivity(intent);
            return true;
        } catch (ActivityNotFoundException | SecurityException e) {
            DLog.d("locks", e.toString());
        }
        return false;
    }

    public static  boolean safeStartActivityForResult(final Context context, final Intent intent,int requestCode) {
        if (context == null || intent == null) {
            return false;
        }
        try {
            if (!(context instanceof Activity)) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            }else{
                ((Activity)context).startActivityForResult(intent,requestCode);
            }

            return true;
        } catch (ActivityNotFoundException | SecurityException e) {
            DLog.d("locks", e.toString());
        }
        return false;
    }

}