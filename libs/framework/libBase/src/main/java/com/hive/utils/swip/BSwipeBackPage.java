// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.utils.swip;

import android.annotation.TargetApi;
import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.MotionEvent;
import android.view.ViewGroup;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;

public class BSwipeBackPage {

    Activity mActivity;
    BSwipeBackLayout mSwipeBackLayout;
    BRelateSlider mSlider;

    private boolean mEnable = true;
    private boolean mRelativeEnable = false;

    BSwipeBackPage(@NonNull Activity activity) {
        mActivity = activity;
    }

    //页面的回调用于配置滑动效果
    void onCreate() {

        mActivity.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        mActivity.getWindow().getDecorView().setBackgroundColor(Color.TRANSPARENT);

        mSwipeBackLayout = new BSwipeBackLayout(mActivity.getApplicationContext());
        mSwipeBackLayout.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        mSwipeBackLayout.setEdgeTrackingEnabled(BViewDragHelper.EDGE_LEFT);

        mSlider = new BRelateSlider(this);
    }

    void onPostCreate() {
        handleLayout();
    }


    @TargetApi(11)
    public BSwipeBackPage setSwipeRelateEnable(boolean enable) {
        mRelativeEnable = enable;
        if (mSlider != null) {
            mSlider.setEnable(enable);
        }
        return this;
    }

    public BSwipeBackPage setSwipeRelateOffset(int offset) {
        mSlider.setOffset(offset);
        return this;
    }


    /**
     * 动态改变滑动的背景透明度
     *
     * @param alpha
     */
    public void onChangeSwipeLayoutBg(@IntRange(from = 0, to = 255) int alpha) {
        if (mSwipeBackLayout != null) {
//            int color = mSwipeBackLayout.getScrimColor();
            mSwipeBackLayout.setAlpha(alpha / 255.f);
        }
    }

    /**
     * 设置是否可滑动关闭
     *
     * @param enable
     * @return
     */
    public BSwipeBackPage setSwipeBackEnable(boolean enable) {
        mEnable = enable;
        if (mSwipeBackLayout != null) {
            mSwipeBackLayout.setEnableGesture(enable);
        }
        handleLayout();
        return this;
    }

    /**
     * 设置可滑动的范围
     *
     * @param swipeEdge 百分比，eg:200表示为左边200px的屏幕
     * @return
     */
    public BSwipeBackPage setSwipeEdge(int swipeEdge) {
        mSwipeBackLayout.setEdgeSize(swipeEdge);
        return this;
    }

    /**
     * 可滑动的范围。百分比。0.2表示为左边20%的屏幕
     *
     * @param swipeEdgePercent
     * @return
     */
    public BSwipeBackPage setSwipeEdgePercent(float swipeEdgePercent) {
        mSwipeBackLayout.setEdgeSizePercent(swipeEdgePercent);
        return this;
    }

    /**
     * 对横向滑动手势的敏感程度。0为迟钝 1为敏感
     *
     * @param sensitivity
     * @return
     */
    public BSwipeBackPage setSwipeSensitivity(float sensitivity) {
        mSwipeBackLayout.setSensitivity(mActivity, sensitivity);
        return this;
    }

    /**
     * 底层阴影颜色
     *
     * @param color
     * @return
     */
    public BSwipeBackPage setScrimColor(int color) {
        mSwipeBackLayout.setScrimColor(color);
        return this;
    }

    /**
     * 触发关闭Activity百分比
     *
     * @param percent
     * @return
     */
    public BSwipeBackPage setClosePercent(float percent) {
        mSwipeBackLayout.setScrollThreshold(percent);
        return this;
    }

    public BSwipeBackPage setDisallowInterceptTouchEvent(boolean disallowIntercept) {
        mSwipeBackLayout.setDisallowInterceptTouchEvent(disallowIntercept);
        return this;
    }

    public BSwipeBackPage setSwipeViewPager(ISwipe viewPager) {
        if (mSwipeBackLayout != null) {
            mSwipeBackLayout.setSwipeViewPager(viewPager);
        }
        return this;
    }

    public BSwipeBackPage addListener(BSwipeListener listener) {
        mSwipeBackLayout.addSwipeListener(listener);
        return this;
    }

    public BSwipeBackPage removeListener(BSwipeListener listener) {
        mSwipeBackLayout.removeSwipeListener(listener);
        return this;
    }

    public BSwipeBackLayout getSwipeBackLayout() {
        return mSwipeBackLayout;
    }

    public void scrollToFinishActivity() {
        if (mSwipeBackLayout != null) {
            mSwipeBackLayout.scrollToFinishActivity();
        }
    }

    private void handleLayout() {
        if (mSwipeBackLayout == null) {
            return;
        }

        if (mEnable || mRelativeEnable) {
            mSwipeBackLayout.attachToActivity(mActivity);
        } else {
            mSwipeBackLayout.removeFromActivity(mActivity);
        }
    }

    public interface ISwipe {
        /**
         * @param event
         * @return false:不需要拦截事件
         */
        boolean shouldInterceptTouchEvent(MotionEvent event);

        /**
         * @param event false: dispatchTouchEvent to this
         * @return
         */
        boolean shouldDispatchTouchEvent(MotionEvent event);
    }
}
