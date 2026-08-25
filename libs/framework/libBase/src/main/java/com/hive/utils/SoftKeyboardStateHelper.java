// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.utils;

import android.app.Activity;
import android.content.Context;
import android.graphics.Rect;
import android.os.Build;
import androidx.annotation.NonNull;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;

import com.hive.utils.system.CommonTools;
import com.hive.utils.system.SystemProperty;

import java.util.LinkedList;
import java.util.List;


/**
 * Created by joyisn on 2017/12/13.
 */

public class SoftKeyboardStateHelper implements ViewTreeObserver.OnGlobalLayoutListener {
    public interface SoftKeyboardStateListener {
        void onSoftKeyboardOpened(int keyboardHeightInPx);

        void onSoftKeyboardClosed();
    }

    private final List<SoftKeyboardStateListener> listeners = new LinkedList<SoftKeyboardStateListener>();
    private final View activityRootView;
    private int usableHeightPrevious;
    private int navigationBarHeight;
    private int screenHeight;
    private int screenWidth;
    private boolean isSoftKeyboardOpen;

    private boolean isAdjustRootViewLayout;

    public SoftKeyboardStateHelper(@NonNull Context context, View rootView) {
        int w = context.getResources().getDisplayMetrics().widthPixels;
        int h = context.getResources().getDisplayMetrics().heightPixels;
        this.navigationBarHeight = SystemProperty.getNavigationBarHeight(context);
        this.screenHeight = Math.max(w, h);
        this.screenWidth = Math.min(w, h);
        this.activityRootView = rootView;
    }

    public SoftKeyboardStateHelper(Activity activity) {
        int w = activity.getResources().getDisplayMetrics().widthPixels;
        int h = activity.getResources().getDisplayMetrics().heightPixels;
        this.navigationBarHeight = SystemProperty.getNavigationBarHeight(activity);
        this.screenHeight = Math.max(w, h);
        this.screenWidth = Math.min(w, h);
        //1､找到Activity的最外层布局控件，它其实是一个DecorView,它所用的控件就是FrameLayout
        FrameLayout content = (FrameLayout) activity.findViewById(android.R.id.content);
        //2､获取到setContentView放进去的View
        this.activityRootView = content.getChildAt(0);
    }

    public void setAdjustRootViewLayout(boolean adjustRootViewLayout) {
        isAdjustRootViewLayout = adjustRootViewLayout;
    }

    @Override
    public void onGlobalLayout() {
        //1､获取当前界面可用高度，键盘弹起后，当前界面可用布局会减少键盘的高度
        int usableHeightNow = computeUsableHeight();

        //2､如果当前可用高度和原始值不一样
        if (activityRootView != null && usableHeightNow != usableHeightPrevious) {
            //3､获取Activity中xml中布局在当前界面显示的高度
            int usableHeightSansKeyboard = CommonTools.isLandscape(activityRootView.getContext()) ? screenWidth : screenHeight;
            //4､Activity中xml布局的高度-当前可用高度
            int heightDifference = usableHeightSansKeyboard - usableHeightNow;

            //5､高度差大于屏幕1/4时，说明键盘弹出
            if (heightDifference > (usableHeightSansKeyboard / 4)) {
                // 6､键盘弹出了，Activity的xml布局高度应当减去键盘高度
                if (!isSoftKeyboardOpen) {
                    notifyOnSoftKeyboardOpened(heightDifference);
                    isSoftKeyboardOpen = true;
                }
            } else {
                if (isSoftKeyboardOpen) {
                    notifyOnSoftKeyboardClosed();
                    isSoftKeyboardOpen = false;
                }
            }

            if (isAdjustRootViewLayout) {
                activityRootView.setPadding(0, 0, 0, getSoftKeyboardHeight());
            }

            usableHeightPrevious = usableHeightNow;
        }
    }

    public int getSoftKeyboardHeight() {
        //1､获取Activity中xml中布局在当前界面显示的高度
        int usableHeightSansKeyboard = CommonTools.isLandscape(activityRootView.getContext()) ? screenWidth : screenHeight;
//        if (isSoftKeyboardOpen) {
        //2､获取当前界面可用高度，键盘弹起后，当前界面可用布局会减少键盘的高度
        int usableHeightNow = computeUsableHeight();

        //3､Activity中xml布局的高度-当前可用高度
        return usableHeightSansKeyboard - usableHeightNow + navigationBarHeight;
//        } else {
//            return 0;
//        }
    }

    private int computeUsableHeight() {
        if (activityRootView != null) {
            Rect r = new Rect();
            activityRootView.getWindowVisibleDisplayFrame(r);
            // 全屏模式下：直接返回r.bottom，r.top其实是状态栏的高度
            return (r.bottom - r.top);
        }
        return 0;
    }

    public void addSoftKeyboardStateListener(SoftKeyboardStateListener listener) {
        listeners.add(listener);

        isSoftKeyboardOpen = false;
        //3､给Activity的xml布局设置View树监听，当布局有变化，如键盘弹出或收起时，都会回调此监听
        activityRootView.getViewTreeObserver().addOnGlobalLayoutListener(this);
    }

    public void removeSoftKeyboardStateListener(SoftKeyboardStateListener listener) {
        if (!listeners.contains(listener)) {
            return;
        }

        listeners.remove(listener);

        if (listeners.size() == 0 && activityRootView != null && activityRootView.getViewTreeObserver() != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                activityRootView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            } else {
                activityRootView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
            }
        }
    }

    private void notifyOnSoftKeyboardOpened(int keyboardHeightInPx) {
        for (SoftKeyboardStateListener listener : listeners) {
            if (listener != null) {
                listener.onSoftKeyboardOpened(keyboardHeightInPx);
            }
        }
    }

    private void notifyOnSoftKeyboardClosed() {
        for (SoftKeyboardStateListener listener : listeners) {
            if (listener != null) {
                listener.onSoftKeyboardClosed();
            }
        }
    }
}
