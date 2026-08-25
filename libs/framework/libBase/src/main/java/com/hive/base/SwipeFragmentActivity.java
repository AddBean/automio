// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.base;

import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;

import com.hive.utils.SoftKeyboardStateHelper;
import com.hive.utils.swip.BSwipeBackHelper;
import com.hive.utils.swip.BSwipeBackPage;
import com.hive.utils.swip.BSwipeListener;
import com.hive.utils.system.SystemProperty;
import com.hive.utils.system.ThemeUtils;

/**
 * @since 3.9.1
 */
public abstract class SwipeFragmentActivity extends BaseFragmentActivity implements SoftKeyboardStateHelper.SoftKeyboardStateListener, BSwipeListener {

    private SoftKeyboardStateHelper mSoftKeyboardStateHelper;

    /**
     * 背景是否透明
     *
     * @return
     */
    protected boolean enableBackTransparent() {
        return true;
    }

    /**
     * 是否禁用手势右滑退出
     *
     * @param isEnabled
     */
    public void setSwipeEnabled(boolean isEnabled) {
        BSwipeBackPage page = BSwipeBackHelper.getCurrentPage(this);
        if (page != null) {
            page.setSwipeBackEnable(isEnabled);
        }
    }

    protected boolean supportTranslucentStyle() {
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (supportTranslucentStyle()) {
            ThemeUtils.convertActivityToTranslucent(this);
        }

        super.onCreate(savedInstanceState);

        BSwipeBackHelper.onCreate(this);

        BSwipeBackPage page = BSwipeBackHelper.getCurrentPage(this).
                setSwipeBackEnable(true);
        if (isInPlayView()) {
            page.setSwipeViewPager(new BSwipeBackPage.ISwipe() {
                @Override
                public boolean shouldInterceptTouchEvent(MotionEvent event) {
                    return !forbiddenSwipeInSpecialStatus(event);
                }

                @Override
                public boolean shouldDispatchTouchEvent(MotionEvent event) {
                    return false;
                }
            });
        }
        page.setScrimColor(enableBackTransparent() ? 0x00000000 : 0x99000000).
                setSwipeSensitivity(0.5f).
                addListener(this).
                setSwipeRelateEnable(false).
                setSwipeRelateOffset(300);
    }


    @Override
    public void setContentView(View view) {
        super.setContentView(view);
        if (mSoftKeyboardStateHelper == null) {
            mSoftKeyboardStateHelper = new SoftKeyboardStateHelper(this);
            mSoftKeyboardStateHelper.addSoftKeyboardStateListener(this);
        }
    }


    /**
     * Called when activity start-up is complete (after {@link #onStart}
     * and {@link #onRestoreInstanceState} have been called)
     *
     * @param savedInstanceState
     */
    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        BSwipeBackHelper.onPostCreate(this);
    }


    @Override
    protected void onDestroy() {
        BSwipeBackHelper.onDestroy(this);
        if (null != mSoftKeyboardStateHelper) {
            mSoftKeyboardStateHelper.removeSoftKeyboardStateListener(this);
        }
        super.onDestroy();
    }

    @Override
    public void finish() {
        BSwipeBackHelper.onDestroy(this);
        super.finish();
    }


    ///////////////////////////////////////////////////////////////////////////
    //SoftKeyboardStateHelper.SoftKeyboardStateListener：目前只有播放器在依赖这个值(键盘的高度)
    ///////////////////////////////////////////////////////////////////////////
    @Override
    public void onSoftKeyboardOpened(int keyboardHeightInPx) {
        SystemProperty.setKeyboardHeight(this, keyboardHeightInPx);
    }

    @Override
    public void onSoftKeyboardClosed() {

    }

    ///////////////////////////////////////////////////////////////////////////
    // BSwipeListener:滑动回调
    ///////////////////////////////////////////////////////////////////////////
    @Override
    public void onScroll(float percent, int px) {

    }

    @Override
    public void onEdgeTouch() {

    }

    @Override
    public void onScrollToClose() {
        if (!isFinishing()) {
            finish();
            overridePendingTransition(0, 0);
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // 用于特殊需要拦截滑动区域的处理
    ///////////////////////////////////////////////////////////////////////////

    /**
     * 是否需要禁用响应右滑退出
     *
     * @return true 拦截滑动返回事件
     */
    public boolean forbiddenSwipeInSpecialStatus(MotionEvent event) {
        return false;
    }

    /**
     * 是否是播放器Activity
     *
     * @return
     */
    public boolean isInPlayView() {
        return false;
    }

}
