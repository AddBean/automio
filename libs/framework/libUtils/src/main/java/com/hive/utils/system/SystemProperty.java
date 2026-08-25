// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.utils.system;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.os.Build;
import android.view.ViewConfiguration;

import com.hive.utils.cache.LabSp;

import java.lang.reflect.Field;

/**
 * 获取一些系统属性
 * <p>
 * Created by gzg on 2015/12/22.
 */
public class SystemProperty {
    /*
     *为了获取状态栏，虚拟导航栏的高度，本来可以直接从 SystemBarTintManager 里面获取，但是SystemBarTintManager初始化挺耗时间的
     * 所以给 SystemBarTintManager 延迟初始化，让 SystemBarTintManager 只负责修改状态栏和虚拟导航栏的颜色，而不负责为外界提供 导航栏和虚拟导航栏高度。
     */
    private static final String STATUS_BAR_HEIGHT_RES_NAME = "status_bar_height";
    private static final String NAV_BAR_HEIGHT_RES_NAME = "navigation_bar_height";
    private static final String SHOW_NAV_BAR_RES_NAME = "config_showNavigationBar";


    private static int mNavigationBarHeight = -1;
    private static int mStatusBarHeight = -1;
    private static int mKeyboardHeight = -1;

    public static int getNavigationBarHeight(Context context) {

        if (mNavigationBarHeight < 0) {
            mNavigationBarHeight = calculateNavigationBarHeight(context);
        }

        return mNavigationBarHeight;
    }

    public static int getStatusBarHeight(Context context) {
        if (mStatusBarHeight <= 0) {
            mStatusBarHeight = getInternalDimensionSize(context.getResources(), STATUS_BAR_HEIGHT_RES_NAME);
        }
        //if mStatusBarHeight still 0 then try again
        if (mStatusBarHeight <= 0) {
            mStatusBarHeight = getStatusBarHeight2(context);
        }
        return mStatusBarHeight;
    }

    //获取手机状态栏高度
    public static int getStatusBarHeight2(Context context) {
        Class<?> c = null;
        Object obj = null;
        Field field = null;
        int x = 0, statusBarHeight = 0;
        try {
            c = Class.forName("com.android.internal.R$dimen");
            obj = c.newInstance();
            field = c.getField("status_bar_height");
            x = Integer.parseInt(field.get(obj).toString());
            statusBarHeight = context.getResources().getDimensionPixelSize(x);
        } catch (Exception e1) {
            e1.printStackTrace();
        }
        return statusBarHeight;
    }

    public static int getKeyboardHeight(Context context) {
        mKeyboardHeight = LabSp.getInstance(context).getInt(LabSp.KG_SOFT_KEYBOARD_WINDOW_HEIGHT, -1);
        if (mKeyboardHeight < 0) {
            try {
                int deviceDensityDpi = Integer.valueOf(CommonUtils.getDeviceDensityDpi(context));
                if (deviceDensityDpi > 450) {
                    //三星 560
                    mKeyboardHeight = UIUtils.dipToPx(context, 326);
                } else if (deviceDensityDpi < 250) {
                    mKeyboardHeight = UIUtils.dipToPx(context, 312);
                } else {
                    mKeyboardHeight = UIUtils.dipToPx(context, 312);
                }
            } catch (Exception e) {
                mKeyboardHeight = UIUtils.dipToPx(context, 312);
            }
        }
        return mKeyboardHeight;
    }

    public static void setKeyboardHeight(Context context, int keyboardHeight) {
        SystemProperty.mKeyboardHeight = keyboardHeight;
        LabSp.getInstance(context).putInt(LabSp.KG_SOFT_KEYBOARD_WINDOW_HEIGHT, keyboardHeight);
    }

    private static int isSupportVr = 0;

    public static boolean isSupportVr(Context context) {
//        if (isSupportVr == 0) {
//            TrackingSensorsHelper sensorsHelper = new TrackingSensorsHelper(context.getPackageManager());
//            boolean isVrEnable = Build.VERSION.SDK_INT > 15 && sensorsHelper.areTrackingSensorsAvailable();
//
//            isSupportVr = isVrEnable ? 1 : -1;
//        }

        return isSupportVr == 1;
    }


    private static int calculateNavigationBarHeight(Context context) {
        Resources res = context.getResources();
        int result = 0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            if (hasNavBar(context)) {
                String key = NAV_BAR_HEIGHT_RES_NAME;

                return getInternalDimensionSize(res, key);
            }
        }
        return result;
    }

    private static boolean hasNavBar(Context context) {
        Resources res = context.getResources();
        int resourceId = res.getIdentifier(SHOW_NAV_BAR_RES_NAME, "bool", "android");
        if (resourceId != 0) {
            boolean hasNav = res.getBoolean(resourceId);
            // check override flag (see static block)
            if ("1".equals(SystemBarTintManager.sNavBarOverride)) {
                hasNav = false;
            } else if ("0".equals(SystemBarTintManager.sNavBarOverride)) {
                hasNav = true;
            }
            return hasNav;
        } else { // fallback
            return !ViewConfiguration.get(context).hasPermanentMenuKey();
        }
    }

    private static int getInternalDimensionSize(Resources res, String key) {
        int result = 0;
        int resourceId = res.getIdentifier(key, "dimen", "android");
        if (resourceId > 0) {
            result = res.getDimensionPixelSize(resourceId);
        }
        return result;
    }

    private static int screenWidth;
    private static int screenHeight;

    public static int getScreenWidth(Context context) {
        if (screenWidth == 0) {
            if (null != context) {
                int w = context.getResources().getDisplayMetrics().widthPixels;
                int h = context.getResources().getDisplayMetrics().heightPixels;

                screenWidth = Math.min(w, h);
                screenHeight = Math.max(w, h);
            }
        }

        return screenWidth;
    }

    public static int getScreenHeight(Context context) {
        if (screenHeight == 0) {
            if (null != context) {
                int w = context.getResources().getDisplayMetrics().widthPixels;
                int h = context.getResources().getDisplayMetrics().heightPixels;

                screenWidth = Math.min(w, h);
                screenHeight = Math.max(w, h);
            }
        }

        return screenHeight;
    }

    public static int[] getWindowSize(Activity activity) {
        int[] size = new int[2];
        size[0] = activity.getWindowManager().getDefaultDisplay().getWidth();
        size[1] = activity.getWindowManager().getDefaultDisplay().getHeight();
        return size;
    }
}
