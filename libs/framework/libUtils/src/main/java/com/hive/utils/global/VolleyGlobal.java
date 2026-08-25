// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.utils.global;

import android.content.Context;

import java.lang.reflect.Method;

/**
 * Created by lxl on 2016/10/25 0025.
 */

public class VolleyGlobal {
    public static String PACKAGE_NAME = "";
    public static String CHANNEL_ID = "";
    public static String API_KEY = "ANDROID";
    public static String TOKEN;
    public static String USER_ID;


    private static Context mGlobalContext;

//    public static Context getGlobalContext() {
//        return mGlobalContext;
//    }

    public static Context getGlobalContext() {
        if (mGlobalContext == null) {
            try {
                final Class<?> activityThread = Class.forName("android.app.ActivityThread");
                final Method currentApplicationMethod = activityThread.getDeclaredMethod("currentApplication");
                mGlobalContext = (Context) currentApplicationMethod.invoke(null);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return mGlobalContext;
    }


    public static void setGlobalContext(Context mGlobalContext) {
        VolleyGlobal.mGlobalContext = mGlobalContext;
    }

    private static boolean isBaoFengSdk = false;

    public static boolean isBaofengSDK() {
        return isBaoFengSdk;
    }

    public static void setIsBaofengSDK(boolean baoFengSdk) {
        isBaoFengSdk = baoFengSdk;
    }
}
