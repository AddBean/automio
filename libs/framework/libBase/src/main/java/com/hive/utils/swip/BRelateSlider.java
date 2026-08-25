// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.utils.swip;

import android.os.Build;

/**
 * Created by Mr.Jude on 2015/8/26.
 */
public class BRelateSlider implements BSwipeListener {
    private static final int DEFAULT_OFFSET = 40;
    public BSwipeBackPage curPage;
    private int offset = 500;

    public BRelateSlider(BSwipeBackPage curActivity) {
        this.curPage = curActivity;
        //curPage.addListener(this);
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public void setEnable(boolean enable) {
        if (enable) curPage.addListener(this);
        else curPage.removeListener(this);
    }

    @Override
    public void onScroll(float percent, int px) {
        if (Build.VERSION.SDK_INT > 11) {
            BSwipeBackPage page = BSwipeBackHelper.getPrePage(curPage);
            if (page != null) {
                page.getSwipeBackLayout().setX(Math.min(-offset * Math.max(1 - percent, 0) + DEFAULT_OFFSET, 0));
                if (percent == 0) {
                    page.getSwipeBackLayout().setX(0);
                }
            }
        }
    }

    @Override
    public void onEdgeTouch() {

    }

    @Override
    public void onScrollToClose() {
        BSwipeBackPage page = BSwipeBackHelper.getPrePage(curPage);
        if (Build.VERSION.SDK_INT > 11) {
            if (page != null) page.getSwipeBackLayout().setX(0);
        }
    }
}
