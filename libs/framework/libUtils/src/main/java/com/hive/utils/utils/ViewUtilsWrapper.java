// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.utils.utils;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Build;

import android.text.TextUtils;
import android.view.View;
import android.view.WindowManager;
import android.widget.HorizontalScrollView;
import android.widget.ScrollView;

import androidx.viewpager.widget.ViewPager;

import com.hive.utils.debug.DLog;
import com.hive.utils.statusbar.LightStatusBarCompat;
import com.hive.utils.statusbar.StatusBarCompat;

/**
 * view 工具包装调用类
 * Created by kuaigeng01 on 2018/4/23.
 */
public final class ViewUtilsWrapper {
//    public static String sStatusBarColor = "#ffffff";

    private static final String FRAGMENT_CON = "NoSaveStateFrameLayout";

    /**
     * @param statusBarWhite
     * @param activity
     * @param skinStyle      0：默认皮肤  1：夜间皮肤
     */
    public static void setStatusBarBgColor(boolean statusBarWhite, Activity activity, int skinStyle) {
        if (DLog.isDebug()) {
            DLog.e("ViewUtilsWrapper", "setStatusBarBgColor.statusBarWhite=" + statusBarWhite);
        }
        if (activity == null || activity.isFinishing()) return;
        String deColor = StatusBarCompat.ThemeColor;

        //如果大>=5.0且<6.0、非魅族miui，则不支持春节通知栏主题
        if ((Build.VERSION.SDK_INT < Build.VERSION_CODES.M) && !LightStatusBarCompat.isMiUiOrMeizu()) {
            deColor = null;
        }

        if (TextUtils.isEmpty(deColor)) {
            statusBarWhite = true;
            deColor = skinStyle == 0 ? "#FFFFFF" : "#252428";
        }
        if (statusBarWhite) {
            StatusBarCompat.setStatusBarColor(activity, Color.parseColor(skinStyle == 0 ? "#FFFFFF" : "#252428"));
        } else {
            StatusBarCompat.setStatusBarColor(activity, Color.parseColor(deColor));
        }
    }


    /**
     * 横屏对于有虚拟导航栏的手机需要隐藏，并且不让视频尺寸发生变化
     *
     * @param mActivity
     * @param isLand
     */
    public static void showOrHiddenSystemUI(Activity mActivity, boolean isLand) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
//            if (isLand) {
//                hideSystemUI(mActivity);
//            } else {
//                showSystemUI(mActivity);
//            }

            int newVis;
            if (isLand) {

                newVis = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
//                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
//                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LOW_PROFILE
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
            } else {
                newVis = View.SYSTEM_UI_FLAG_VISIBLE;
            }

            View mDecorView = mActivity.getWindow().getDecorView();
            mDecorView.setSystemUiVisibility(newVis);


            if (isLand) {
                StatusBarCompat.toggleStatusBarVisible(false);
                mActivity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            } else {
                StatusBarCompat.toggleStatusBarVisible(true);
                mActivity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            }

        } else {
            if (isLand) {
                WindowManager.LayoutParams lp = mActivity.getWindow().getAttributes();
                lp.flags |= WindowManager.LayoutParams.FLAG_FULLSCREEN;
                mActivity.getWindow().setAttributes(lp);
//            mActivity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
            } else {
                WindowManager.LayoutParams attr = mActivity.getWindow().getAttributes();
                attr.flags &= (~WindowManager.LayoutParams.FLAG_FULLSCREEN);
                mActivity.getWindow().setAttributes(attr);
//            mActivity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
            }
        }
    }

    public static Rect getLocationInView(View parent, View child) {
        if (child == null || parent == null) {
            throw new IllegalArgumentException("parent and child can not be null .");
        }

        View decorView = null;
        Context context = child.getContext();
        if (context instanceof Activity) {
            decorView = ((Activity) context).getWindow().getDecorView();
        }

        Rect result = new Rect();
        Rect tmpRect = new Rect();

        View tmp = child;

        if (child == parent) {
            child.getHitRect(result);
            return result;
        }
        while (tmp != decorView && tmp != parent) {
            tmp.getHitRect(tmpRect);
            if (!tmp.getClass().equals(FRAGMENT_CON)) {
                result.left += tmpRect.left;
                result.top += tmpRect.top;
            }
            tmp = (View) tmp.getParent();
            if (tmp == null) {
                throw new IllegalArgumentException("the view is not showing in the window!");
            }
            //fix ScrollView中无法获取正确的位置
            if (tmp.getParent() instanceof ScrollView) {
                ScrollView scrollView = (ScrollView) tmp.getParent();
                int scrollY = scrollView.getScrollY();
                result.top -= scrollY;
            }
            if (tmp.getParent() instanceof HorizontalScrollView) {
                HorizontalScrollView horizontalScrollView = (HorizontalScrollView) tmp.getParent();
                int scrollX = horizontalScrollView.getScrollX();
                result.left -= scrollX;
            }

            //added by isanwenyu@163.com fix bug #21 the wrong rect user will received in ViewPager
            if (tmp.getParent() != null && (tmp.getParent() instanceof ViewPager)) {
                tmp = (View) tmp.getParent();
            }
        }
        result.right = result.left + child.getMeasuredWidth();
        result.bottom = result.top + child.getMeasuredHeight();
        return result;
    }
}
