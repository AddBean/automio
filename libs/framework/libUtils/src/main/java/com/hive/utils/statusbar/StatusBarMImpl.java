// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.utils.statusbar;

import android.annotation.TargetApi;
import android.graphics.Color;
import android.os.Build;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

/**
 * 兼容M版本
 *
 */
@TargetApi(Build.VERSION_CODES.M)
class StatusBarMImpl implements IStatusBar {
    @Override
    public void toggleStatusBarVisible(boolean show) {

    }

    @Override
    public void clearStatusBarColor() {

    }

    @TargetApi(Build.VERSION_CODES.M)
    public void setStatusBarColor(Window window, int color, boolean lightStatusBar) {
        //取消设置透明状态栏,使 ContentView 内容不再覆盖状态栏
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        //需要设置这个 flag 才能调用 setStatusBarColor 来设置状态栏颜色
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        //设置状态栏颜色
        window.setStatusBarColor(color);

        window.setNavigationBarColor(Color.parseColor("#000000"));

        // 设置浅色状态栏时的界面显示
        View decor = window.getDecorView();
        int ui = decor.getSystemUiVisibility();
        if (lightStatusBar) {
            ui |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        } else {
            ui &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        }
        decor.setSystemUiVisibility(ui);

        // 去掉系统状态栏下的windowContentOverlay
        View v = window.findViewById(android.R.id.content);
        if (v != null) {
            v.setForeground(null);
        }

        LightStatusBarCompat.setLightStatusBar(window, lightStatusBar);
    }

    @Override
    public void setStatusBarImmersion(Window window) {
        View decorView = window.getDecorView();

        int option = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE;

//        if (StatusBarCompat.navigationBarExist(window)) {
//            option |= View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
//        }

        decorView.setSystemUiVisibility(option);

        int finalColor = Color.TRANSPARENT;
//        window.setNavigationBarColor(finalColor);
        window.setStatusBarColor(finalColor);
    }

}
