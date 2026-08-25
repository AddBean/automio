// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.utils.statusbar;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;

import com.hive.utils.R;

/**
 * 兼容KITKAT版本
 *
 * @author 黄浩杭 (huanghaohang@parkingwang.com)
 * @version 2016-06-20
 * @since 2016-06-20
 */

@TargetApi(Build.VERSION_CODES.KITKAT)
class StatusBarKitkatImpl implements IStatusBar {
    private View statusBarView;

    @Override
    public void toggleStatusBarVisible(boolean show) {
        if (null != statusBarView) {
            statusBarView.setVisibility(show ? View.VISIBLE : View.INVISIBLE);
        }
    }

    @Override
    public void clearStatusBarColor() {
        statusBarView = null;
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    public void setStatusBarColor(Window window, int color, boolean lightStatusBar) {
        if (!StatusBarCompat.toGrey(color) || LightStatusBarCompat.isMiUiOrMeizu()) {
            window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);

            if (null == statusBarView) {
                //            statusBarView.setVisibility(View.VISIBLE);
                //6.0以下  白色主题沉浸式，状态栏底色采用黑色（规避字体无法设置黑色）
                statusBarView = createStatusBarView(window.getContext(), color);
            }

            StatusBarCompat.setFitsSystemWindows(window, true);
            LightStatusBarCompat.setLightStatusBar(window, lightStatusBar);
        }
    }

    @Override
    public void setStatusBarImmersion(Window window) {
        window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        ViewGroup decorView = (ViewGroup) window.getDecorView();
        int finalColor = Color.TRANSPARENT;
        decorView.addView(createStatusBarView(window.getContext(), com.hive.i8n.R.color.transparent));
//        if (StatusBarCompat.navigationBarExist(window)) {
//            window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
//            decorView.addView(createNavigationBarView(window.getContext(), finalColor));
//        }
    }

    private View createStatusBarView(Context context, int color) {
        View statusBarView = new View(context);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, StatusBarCompat.getStatusBarHeight(context));
        params.gravity = Gravity.TOP;
        statusBarView.setLayoutParams(params);
        statusBarView.setBackgroundColor(color);
        return statusBarView;
    }

    private View createNavigationBarView(Context context, int color) {
        View navBarView = new View(context);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, StatusBarCompat.getNavigationBarHeight(context));
        params.gravity = Gravity.BOTTOM;
        navBarView.setLayoutParams(params);
        navBarView.setBackgroundColor(color);
        return navBarView;
    }

}
