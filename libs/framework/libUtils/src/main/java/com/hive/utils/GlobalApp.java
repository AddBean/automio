// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.utils;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.WindowManager;

import com.hive.utils.system.SystemProperty;
import com.hive.utils.system.UIUtils;
import com.hive.utils.utils.DESCrypt;


public class GlobalApp {
    public static final long THROTTLE_TIME_DEFAULT = 200;

    public static Context sContext;

    public static CommonBaseResources sGlobalResources;

    public static String sStringEncodeKey = null;

    public static String sStringEncodeFlag = null;

    public static boolean isSupportStatusBar = false;


    private static String sFlavorName;

    public static int DP;

    public static Activity sTopActivity;

    public static Activity sMainActivity;

    public static Class sMainActivityClass;

    public static boolean isOfflineMode = false;

    public static DESCrypt desCrypt = new DESCrypt();

    public static int sActivityCount;

    public static Context getContext() {
        return sContext;
    }

    public static Application getApp() {
        return (Application) sContext;
    }

    public static Resources getResources() {
        if (sGlobalResources == null) {
            sGlobalResources = new CommonBaseResources(getInnerResources());
        }
        return sGlobalResources;
    }

    public static String getPackageName() {
        return sContext.getPackageName();
    }

    public static void init(Context context) {
        sContext = context;
        DP = UIUtils.dp2px(context, 1);
    }

    public static void initDes(String desFlag, String desPwd) {
        GlobalApp.sStringEncodeFlag = desFlag;
        GlobalApp.sStringEncodeKey = desPwd;
        desCrypt.init(desPwd);
    }

    public static float dp2px(int dp) {
        return UIUtils.dp2px(sContext, dp);
    }

    // RefWatcher methods removed for compatibility


    public static String getFlavorName() {
        return sFlavorName;
    }

    public static String getString(int id) {
        String originString = getInnerResources().getString(id);
        return decrypt(originString);
    }

    public static String getString(int id, Object... args) {
        String raw = getString(id);
        if (args != null && args.length > 0) {
            if (raw.contains("%d%")) {
                raw = raw.replace("%d%", "%d%%");
            }
            if (raw.contains("%f%")) {
                raw = raw.replace("%f%", "%f%%");
            }
            if (raw.contains("%s%")) {
                raw = raw.replace("%s%", "%s%%");
            }
        }
        return String.format(GlobalApp.getInnerResources().getConfiguration().getLocales().get(0), raw,
                args);
    }

    public static String[] getStringArray(int id) {
        String[] stringArr = getInnerResources().getStringArray(id);
        String[] stringArrDecrypt = new String[stringArr.length];
        for (int i = 0; i < stringArrDecrypt.length; i++) {
            stringArrDecrypt[i] = decrypt(stringArr[i]);
        }
        return stringArrDecrypt;
    }

    public static CharSequence[] getTextArray(int id) {
        return getStringArray(id);
    }

    public static CharSequence getText(int id) {
        return getString(id);
    }

    public static String decrypt(String originString) {
        if (sStringEncodeKey == null || sStringEncodeFlag == null) {
            return originString;
        }
        if (originString != null && originString.startsWith(sStringEncodeFlag)) {
            try {
                return desCrypt.decrypt(originString.replace(sStringEncodeFlag, ""));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            return originString;
        }
    }

    private static Resources getInnerResources() {
        return sContext.getResources();
    }

    public static int[] getIntArray(int id) {
        return getInnerResources().getIntArray(id);
    }


    public static float getDimension(int resId) {
        return getInnerResources().getDimension(resId);
    }

    public static Drawable getDrawable(int resId) {
        return getInnerResources().getDrawable(resId);
    }

    public static int getColor(int colorId) {
        return getInnerResources().getColor(colorId);
    }

    public static int getInteger(int resId) {
        return getInnerResources().getInteger(resId);
    }

    public static void setFlavorName(String flavorName) {
        GlobalApp.sFlavorName = flavorName;
    }

    public static Activity getMainActivity() {
        return sMainActivity;
    }

    public static Class getMainActivityClass() {
        return sMainActivityClass;
    }

    public static Activity getTopActivity() {
        return sTopActivity;
    }

    public static Activity getAvailableActivity() {
        if (sTopActivity != null)
            return sTopActivity;
        if (sMainActivity != null)
            return sMainActivity;
        return null;
    }

    public static boolean isAppInForeground() {
        return sActivityCount > 0;
    }

    public static boolean isLandscape() {
        WindowManager windowManager = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics displayMetrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getRealMetrics(displayMetrics);
        return displayMetrics.widthPixels > displayMetrics.heightPixels;
    }
    public static int statusBarHeight() {
        return SystemProperty.getStatusBarHeight(GlobalApp.getContext());
    }
}
