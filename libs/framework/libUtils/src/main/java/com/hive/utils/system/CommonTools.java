// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.utils.system;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.WindowManager;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Formatter;
import java.util.Locale;

/**
 * Created by zhigangguo on 15/9/2.
 */
public class CommonTools {

    //permissions
    public static final int MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 101;
    public static final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 102;

    private static StringBuilder formatProgressBuilder = new StringBuilder();
    private static Formatter formatter = new Formatter(formatProgressBuilder, Locale.getDefault());

    /**
     * 播放器中格式化播放时间的函数
     */
    public static String StringForTime(int timeMs) {
//        StringBuilder formatBuilder = new StringBuilder();
//        Formatter formatter = new Formatter(formatBuilder, Locale.getDefault());
        String result = null;

        int totalSeconds = timeMs / 1000;
        int seconds = totalSeconds % 60;
        int minutes = (totalSeconds / 60) % 60;
        int hours = totalSeconds / 3600;

        if (hours > 0) {
            result = formatter.format("%02d:%02d:%02d", hours, minutes, seconds).toString();
        } else {
            result = formatter.format("%02d:%02d", minutes, seconds).toString();
        }

//        formatter.close();
        formatProgressBuilder.delete(0, formatProgressBuilder.length());

        return result;
    }

    /**
     * 播放器中 广告格式化播放时间的函数
     */
    public static String StringForAdTime(int totalSeconds) {
        int seconds = totalSeconds % 60;
        int minutes = (totalSeconds / 60) % 60;

        String result = String.format(Locale.getDefault(), "%d:%02d", minutes, seconds);

        return result;
    }


    public static String encodeUrl(String url) {
        String[] arrays = url.split("/");
        int size = arrays.length;
        StringBuilder urlStr = new StringBuilder();
        String str = null;

        try {
            for (int i = 0; i < size; i++) {
                str = arrays[i];
                if (!TextUtils.isEmpty(str)) {
                    urlStr.append("/").append(URLEncoder.encode(str, "UTF-8"));
                }
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (!TextUtils.isEmpty(urlStr)) {
            url = urlStr.toString();
            url = url.replace("+", "%20");
        }

        return url;
    }


//    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
//    private static int getWidth(Activity _activity) {
//        DisplayMetrics dm = new DisplayMetrics();
//        if (Build.VERSION.SDK_INT >= 17) {
//            _activity.getWindowManager().getDefaultDisplay().getRealMetrics(dm);
//        } else {
//            _activity.getWindowManager().getDefaultDisplay().getMetrics(dm);
//        }
//
//        return dm.widthPixels;
//    }
//
//    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
//    private static int getHeight(Activity _activity) {
//        DisplayMetrics dm = new DisplayMetrics();
//        _activity.getWindowManager().getDefaultDisplay().getMetrics(dm);
//
//        if (Build.VERSION.SDK_INT >= 17) {
//            _activity.getWindowManager().getDefaultDisplay().getRealMetrics(dm);
//        } else {
//            _activity.getWindowManager().getDefaultDisplay().getMetrics(dm);
//        }
//
//        return dm.heightPixels;
//    }

    /**
     * 判断屏幕是否是竖屏
     *
     * @param mActivity
     * @return
     */
    public static boolean isLandscape(Activity mActivity) {
//        if (null != mActivity) {
//            int orienation = mActivity.getRequestedOrientation();//此方法耗时约3ms，多次调用阻塞ui明显；
//            if (ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED == orienation) {
//                return Configuration.ORIENTATION_LANDSCAPE == mActivity.getResources().getConfiguration().orientation;
//            }
//
//            if (ActivityInfo.SCREEN_ORIENTATION_SENSOR == orienation) {
//                return Configuration.ORIENTATION_LANDSCAPE == mActivity.getResources().getConfiguration().orientation;
//            }
//
//            if (Build.VERSION.SDK_INT >= 9) {
//                return ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE == orienation
//                        || ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE == orienation
//                        || ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE == orienation;
//            } else {
//                return ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE == orienation;
//            }
//        }

        if (null != mActivity) {
            return Configuration.ORIENTATION_LANDSCAPE == mActivity.getResources().getConfiguration().orientation;
        }
        return false;
    }

    public static boolean isLandscape(Context context) {
//        if (context instanceof Activity) {
//            return isLandscape((Activity) context);
//        }

        if (null != context) {
            return Configuration.ORIENTATION_LANDSCAPE == context.getResources().getConfiguration().orientation;
        }

        return false;
    }


    /**
     * 改变屏幕方向： 播放器专用
     *
     * @param activity
     * @param isToLandscape true：旋转到横屏；false:旋转到竖屏
     */
    public static void changeScreenOrientation(Activity activity, boolean isToLandscape) {
        if (null == activity) {
            return;
        }

        if (isToLandscape && isLandscape(activity)) {
            return;
        }

        if (!isToLandscape && !isLandscape(activity)) {
            return;
        }
        try {
            if (isToLandscape) {
//            activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                activity.setRequestedOrientation(Build.VERSION.SDK_INT >= 9 ? ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE : ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            } else {
//            activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

//
//    /**
//     * 横屏对于有虚拟导航栏的手机需要隐藏，并且不让视频尺寸发生变化
//     *
//     * @param mActivity
//     * @param isLand
//     */
//    public static void showOrHiddenSystemUI(Activity mActivity, boolean isLand) {
//
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
////            if (isLand) {
////                hideSystemUI(mActivity);
////            } else {
////                showSystemUI(mActivity);
////            }
//
//            int newVis;
//            if (isLand) {
//                newVis = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
//                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
//                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
//                        | View.SYSTEM_UI_FLAG_LOW_PROFILE
//                        | View.SYSTEM_UI_FLAG_FULLSCREEN
//                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
//                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
//            } else {
//                newVis = View.SYSTEM_UI_FLAG_VISIBLE;
//            }
//
//            View mDecorView = mActivity.getWindow().getDecorView();
//            mDecorView.setSystemUiVisibility(newVis);
//
//
//            if (isLand) {
//                StatusBarCompat.toggleStatusBarVisible(false);
//                mActivity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
//            } else {
//                mActivity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
//            }
//
//        } else {
//            if (isLand) {
//                WindowManager.LayoutParams lp = mActivity.getWindow().getAttributes();
//                lp.flags |= WindowManager.LayoutParams.FLAG_FULLSCREEN;
//                mActivity.getWindow().setAttributes(lp);
////            mActivity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
//            } else {
//                WindowManager.LayoutParams attr = mActivity.getWindow().getAttributes();
//                attr.flags &= (~WindowManager.LayoutParams.FLAG_FULLSCREEN);
//                mActivity.getWindow().setAttributes(attr);
////            mActivity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
//            }
//        }
//    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private static void hideSystemUI(Activity mActivity) {
        // This snippet hides the system bars.
        // Set the IMMERSIVE flag.
        // Set the content to appear under the system bars so that the content
        // doesn't resize when the system bars hide and show.
        View mDecorView = mActivity.getWindow().getDecorView();
        mDecorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                        | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

        mActivity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }


    @TargetApi(Build.VERSION_CODES.KITKAT)
    private static void showSystemUI(Activity mActivity) {
        // This snippet shows the system bars. It does this by removing all the flags
        // except for the ones that make the content appear under the system bars.
        View mDecorView = mActivity.getWindow().getDecorView();
        mDecorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_VISIBLE);

        mActivity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }

    // 获取ApiKey
    public static String getMetaValue(Context context, String metaKey) {
        Bundle metaData = null;
        String apiKey = null;
        if (context == null || metaKey == null) {
            return null;
        }
        try {
            ApplicationInfo ai = context.getPackageManager()
                    .getApplicationInfo(context.getPackageName(),
                            PackageManager.GET_META_DATA);
            if (null != ai) {
                metaData = ai.metaData;
            }
            if (null != metaData) {
                apiKey = metaData.getString(metaKey);
            }
        } catch (PackageManager.NameNotFoundException e) {

        }
        return apiKey;
    }

    public static boolean isValidContext(Context context) {
        if (context == null) {
            return false;
        }
        if (context instanceof Activity) {
            final Activity activity = (Activity) context;

            if (Build.VERSION.SDK_INT >= 17) {
                if (activity.isDestroyed() || activity.isFinishing()) {
                    return false;
                }
            } else {
                if (activity.isFinishing()) {
                    return false;
                }
            }
        }
        return true;
    }
}
