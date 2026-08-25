// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.utils.statusbar;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Build;
import android.os.Environment;

import android.util.DisplayMetrics;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.ColorInt;
import androidx.appcompat.app.AppCompatActivity;

import com.hive.utils.GlobalApp;
import com.hive.utils.debug.DLog;
import com.hive.utils.device.DeviceUtil;
import com.hive.utils.utils.ScreenUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Properties;


/**
 * 设置系统状态栏颜色
 * <p>
 * compile 'com.githang:status-bar-compat:0.3'
 */
public class StatusBarCompat {

    public static String ThemeColor = null;
    static final IStatusBar IMPL;

    static {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !DeviceUtil.isYunOS()) {
            IMPL = new StatusBarMImpl();
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {// && !isEMUI() 测试反应一台华为荣耀手机夜间切换异常，状态栏所占高度偏大
            IMPL = new StatusBarLollipopImpl();
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            IMPL = new StatusBarKitkatImpl();
        } else {
            IMPL = new IStatusBar() {
                @Override
                public void setStatusBarColor(Window window, int color, boolean lightStatusBar) {

                }

                @Override
                public void toggleStatusBarVisible(boolean show) {

                }

                @Override
                public void clearStatusBarColor() {

                }

                @Override
                public void setStatusBarImmersion(Window window) {

                }
            };
        }
    }

    private static boolean isEMUI() {
        File file = new File(Environment.getRootDirectory(), "build.prop");
        if (file.exists()) {
            Properties properties = new Properties();
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(file);
                properties.load(fis);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (fis != null) {
                    try {
                        fis.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            return properties.containsKey("ro.build.hw_emui_api_level");
        }
        return false;
    }

    /**
     * Set system status bar color.
     *
     * @param activity
     * @param color    status bar color
     */
    public static void setStatusBarColor(final Activity activity, final int color) {
        if (activity == null) {
            DLog.e("TAG", "activity == null");
            return;
        }
        final Window window = activity.getWindow();
//        if ((window.getAttributes().flags & WindowManager.LayoutParams.FLAG_FULLSCREEN) > 0) {
//            return;
//        }

        View view = null;
        try {
            view = window.getDecorView();
        } catch (Exception ignore) {
            DLog.e("TAG", "view == null");
        }

        if (null != view) {
            view.post(new Runnable() {
                @Override
                public void run() {
                    IMPL.setStatusBarColor(window, color, toGrey(color));
                }
            });
        }
    }

    public static void setStatusBarImmersion(final Activity activity) {
        if (activity == null) {
            DLog.e("TAG", "activity == null");
            return;
        }
        final Window window = activity.getWindow();
        View view = null;

        try {
            view = window.getDecorView();
        } catch (Exception ignore) {
            DLog.e("TAG", "view == null");
        }

        if (null != view) {
            view.post(new Runnable() {
                @Override
                public void run() {
                    IMPL.setStatusBarImmersion(window);
                }
            });
        }
    }

    /**
     * 把颜色转换成灰度值。
     * 代码来自 Flyme 示例代码
     */
    public static boolean toGrey(@ColorInt int color) {
        int blue = Color.blue(color);
        int green = Color.green(color);
        int red = Color.red(color);
        int dark = (red * 38 + green * 75 + blue * 15) >> 7;
        return dark > 225;
    }

    /**
     * app退出时调用一次即可
     */
    public static void clearStatusBarColor() {
        IMPL.clearStatusBarColor();
//        StatusBarCompat.ThemeColor = null;
    }

    public static void toggleStatusBarVisible(boolean show) {
        IMPL.toggleStatusBarVisible(show);
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    public static void setFitsSystemWindows(Window window, boolean fitSystemWindows) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            ViewGroup mContentView = (ViewGroup) window.findViewById(Window.ID_ANDROID_CONTENT);
            View mChildView = mContentView.getChildAt(0);
            if (mChildView != null) {
                //注意不是设置 ContentView 的 FitsSystemWindows, 而是设置 ContentView 的第一个子 View . 预留出系统 View 的空间.
                mChildView.setFitsSystemWindows(fitSystemWindows);
            }
        }
    }

    public static boolean navigationBarExist(Window window) {
        WindowManager windowManager = window.getWindowManager();
        Display defaultDisplay = windowManager.getDefaultDisplay();

        DisplayMetrics realDisplayMetrics = new DisplayMetrics();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            defaultDisplay.getRealMetrics(realDisplayMetrics);
        }

        int realHeight = realDisplayMetrics.heightPixels;
        int realWidth = realDisplayMetrics.widthPixels;

        DisplayMetrics displayMetrics = new DisplayMetrics();
        defaultDisplay.getMetrics(displayMetrics);

        int displayHeight = displayMetrics.heightPixels;
        int displayWidth = displayMetrics.widthPixels;

        return realWidth - displayWidth > 0 || realHeight - displayHeight > 0;
    }


    // 状态栏高度
    public static int getStatusBarHeight(Context context) {
        return getBarHeight(context, "status_bar_height");
    }

    // 导航栏高度
    public static int getNavigationBarHeight(Context context) {
        return getNavigationBarHeight();
    }

    public static int dpToPx(int dp) {
        DisplayMetrics metrics = getDisplayMetrics();
        return (int) (dp * metrics.density + 0.5f * (dp >= 0 ? 1 : -1));
    }

    public static int pxToDp(int px) {
        DisplayMetrics metrics = getDisplayMetrics();
        return (int) (px / metrics.density);
    }

    public static int spToPx(int sp) {
        float fontScale = getDisplayMetrics().scaledDensity;
        return (int) (sp * fontScale + 0.5f);
    }

    public static int pxToSp(int px) {
        DisplayMetrics metrics = getDisplayMetrics();
        return (int) (px / metrics.scaledDensity);
    }

    /**
     * 获取手机显示App区域的大小（头部导航栏+ActionBar+根布局），不包括虚拟按钮
     *
     * @return
     */
    public static int[] getAppSize() {
        int[] size = new int[2];
        DisplayMetrics metrics = getDisplayMetrics();
        size[0] = metrics.widthPixels;
        size[1] = metrics.heightPixels;
        return size;
    }

    /**
     * 获取整个手机屏幕的大小(包括虚拟按钮)
     * 必须在onWindowFocus方法之后使用
     *
     * @param activity
     * @return
     */
    public static int[] getScreenSize(AppCompatActivity activity) {
        int[] size = new int[2];
        View decorView = activity.getWindow().getDecorView();
        size[0] = decorView.getWidth();
        size[1] = decorView.getHeight();
        return size;
    }

    /**
     * 获取状态栏的高度
     */
    public static int getStatusBarHeight() {
        Resources resources = GlobalApp.getResources();
        int resourceId = resources.getIdentifier("status_bar_height", "dimen", "android");
        return resources.getDimensionPixelSize(resourceId);
    }

    /**
     * 获取虚拟按键的高度
     */
    public static int getNavigationBarHeight() {
        int navigationBarHeight = 0;
        Resources rs = GlobalApp.getResources();
        int id = rs.getIdentifier("navigation_bar_height", "dimen", "android");
        if (id > 0 && hasNavigationBar()) {
            navigationBarHeight = rs.getDimensionPixelSize(id);
        }
        return navigationBarHeight;
    }

    public static Boolean hasNavigationBar() {
        return hasNavigationBar(GlobalApp.getContext());
    }

    public static Boolean hasNavigationBar(Context context) {
        Display defaultDisplay = ((WindowManager)
                (context.getSystemService(Context.WINDOW_SERVICE))).getDefaultDisplay();
        //屏幕实际高度
        Point realPoint = new Point();
        defaultDisplay.getRealSize(realPoint);
        //屏幕显示高度
        DisplayMetrics outMetrics = new DisplayMetrics();
        defaultDisplay.getMetrics(outMetrics);
        //虚拟底部导航高度
        int navigationBarHeight = StatusBarCompat.getNavigationBarHeight(context);
        return outMetrics.heightPixels + navigationBarHeight <= realPoint.y;
    }

    public static DisplayMetrics getDisplayMetrics() {
        return GlobalApp
                .getResources()
                .getDisplayMetrics();
    }

    private static int getBarHeight(Context context, String name) {
        Resources res = context.getResources();
        int resourceId = res.getIdentifier(name, "dimen", "android");
        return res.getDimensionPixelSize(resourceId);
    }

}
