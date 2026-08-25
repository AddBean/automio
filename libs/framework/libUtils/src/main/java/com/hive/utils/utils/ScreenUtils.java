// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.utils.utils;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.os.Build;

import androidx.annotation.NonNull;

import android.util.DisplayMetrics;
import android.view.View;
import android.view.WindowManager;

import com.hive.utils.GlobalApp;
import com.hive.utils.system.UIUtils;

public class ScreenUtils {
    private ScreenUtils() {
        throw new UnsupportedOperationException("u can't instantiate me...");
    }

    /**
     * 横屏时会相反
     * @return
     */
    public static int getScreenWidth() {
        WindowManager wm = (WindowManager) GlobalApp.sContext.getSystemService(Context.WINDOW_SERVICE);
        if (wm == null) {
            return GlobalApp.sContext.getResources().getDisplayMetrics().widthPixels;
        } else {
            Point point = new Point();
            if (Build.VERSION.SDK_INT >= 17) {
                wm.getDefaultDisplay().getRealSize(point);
            } else {
                wm.getDefaultDisplay().getSize(point);
            }

            return point.x;
        }
    }

    /**
     * 横屏时会相反
     * @return
     */
    public static int getScreenHeight() {
        WindowManager wm = (WindowManager) GlobalApp.sContext.getSystemService(Context.WINDOW_SERVICE);
        if (wm == null) {
            return GlobalApp.sContext.getResources().getDisplayMetrics().heightPixels;
        } else {
            Point point = new Point();
            if (Build.VERSION.SDK_INT >= 17) {
                wm.getDefaultDisplay().getRealSize(point);
            } else {
                wm.getDefaultDisplay().getSize(point);
            }

            return point.y;
        }
    }

    public static float getScreenDensity() {
        return GlobalApp.sContext.getResources().getDisplayMetrics().density;
    }

    public static int getScreenDensityDpi() {
        return GlobalApp.sContext.getResources().getDisplayMetrics().densityDpi;
    }

    public static void setFullScreen(@NonNull Activity activity) {
        activity.getWindow().addFlags(1536);
    }

    public static void setLandscape(@NonNull Activity activity) {
        activity.setRequestedOrientation(0);
    }

    public static void setPortrait(@NonNull Activity activity) {
        activity.setRequestedOrientation(1);
    }

    public static boolean isLandscape() {
        return GlobalApp.sContext.getResources().getConfiguration().orientation == 2;
    }

    public static boolean isPortrait() {
        return GlobalApp.sContext.getResources().getConfiguration().orientation == 1;
    }

    public static int getScreenRotation(@NonNull Activity activity) {
        switch (activity.getWindowManager().getDefaultDisplay().getRotation()) {
            case 0:
                return 0;
            case 1:
                return 90;
            case 2:
                return 180;
            case 3:
                return 270;
            default:
                return 0;
        }
    }

    public static Bitmap screenShot(@NonNull Activity activity) {
        return screenShot(activity, false);
    }

    public static Bitmap screenShot(@NonNull Activity activity, boolean isDeleteStatusBar) {
        View decorView = activity.getWindow().getDecorView();
        decorView.setDrawingCacheEnabled(true);
        decorView.buildDrawingCache();
        Bitmap bmp = decorView.getDrawingCache();
        DisplayMetrics dm = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(dm);
        Bitmap ret;
        if (isDeleteStatusBar) {
            Resources resources = activity.getResources();
            int resourceId = resources.getIdentifier("status_bar_height", "dimen", "android");
            int statusBarHeight = resources.getDimensionPixelSize(resourceId);
            ret = Bitmap.createBitmap(bmp, 0, statusBarHeight, dm.widthPixels, dm.heightPixels - statusBarHeight);
        } else {
            ret = Bitmap.createBitmap(bmp, 0, 0, dm.widthPixels, dm.heightPixels);
        }

        decorView.destroyDrawingCache();
        return ret;
    }


    public static int getDpi() {
        return UIUtils.getDpi(GlobalApp.getContext());
    }
}
