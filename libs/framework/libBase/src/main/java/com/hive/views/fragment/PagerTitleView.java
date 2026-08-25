// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.views.fragment;

import android.content.Context;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RelativeLayout;

import com.hive.annotation.NotProguard;
import com.hive.utils.utils.DensityUtil;
@NotProguard
public abstract class PagerTitleView extends RelativeLayout {
    public int DP = 1;
    private PagerTag mPagerTag;
    private boolean mSelected = false;
    private View mView;
    protected Object mPager;

    public PagerTitleView(Context context) {
        super(context);
        DP = DensityUtil.dip2px(1);
        init();
        initView();
    }

    private void init() {
        mView = LayoutInflater.from(getContext()).inflate(getLayoutId(), this);
    }

    public void setSelected(boolean isSelected) {
        this.mSelected = isSelected;
    }

    public boolean isSelected() {
        return this.mSelected;
    }

    public PagerTag getPagerTag() {
        return mPagerTag;
    }

    public void setPagerTag(PagerTag mPagerTag) {
        this.mPagerTag = mPagerTag;
        onSetPagerTag(mPagerTag);
    }

    protected abstract void onSetPagerTag(PagerTag pagerTag);

    public View getView() {
        return mView;
    }

    protected abstract void initView();

    protected abstract int getLayoutId();

    /**
     * pager选中事件；
     *
     * @param isSelected 是否被选中
     */
    public abstract void onPageSelected(Boolean isSelected, PagerTag tag);

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (getMeasureWidthPercent() <= 0) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            return;
        }

        int width = (int) (((View) getParent().getParent()).getMeasuredWidth() * getMeasureWidthPercent());
        widthMeasureSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY);
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

    }

    public float getMeasureWidthPercent() {
        return -1;
    }


    public void onScrolling(float progress) {

    }

    public static int mixColors(int colorFrom, int colorTo, float alpha) {
        int r1 = (colorFrom & 0xff0000) >> 16;
        int g1 = (colorFrom & 0xff00) >> 8;
        int b1 = colorFrom & 0xff;
        int r2 = (colorTo & 0xff0000) >> 16;
        int g2 = (colorTo & 0xff00) >> 8;
        int b2 = colorTo & 0xff;
        int r = (int) (r1 * alpha + r2 * (1 - alpha));
        int g = (int) (g1 * alpha + g2 * (1 - alpha));
        int b = (int) (b1 * alpha + b2 * (1 - alpha));
        return ((r << 16) | (g << 8) | b) | 0xff000000;
    }

    public void setPager(Object pager) {
        mPager = pager;
    }

    public void onTabClicked() {

    }
}