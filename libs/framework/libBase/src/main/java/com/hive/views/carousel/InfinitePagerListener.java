// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.views.carousel;

import androidx.viewpager.widget.ViewPager;
import android.view.View;

public class InfinitePagerListener implements ViewPager.OnPageChangeListener {

    private InfiniteCarouseView mCarouseView;
    private int mCurPosition;

    private InfiniteViewPagerAdapter mPagerAdapter;
    private float mScale = 1f;
    private float mCurrPositionOffset;

    public InfinitePagerListener(InfiniteViewPagerAdapter mPagerAdapter, InfiniteCarouseView carouseView) {
        this.mPagerAdapter = mPagerAdapter;
        this.mCarouseView = carouseView;
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        if (mCurrPositionOffset == 0) {
            mCurrPositionOffset = positionOffset;
        }
        boolean isLeft = positionOffsetPixels - mCurrPositionOffset < 0;
        mCurrPositionOffset = positionOffset;
        scaleViews(position, positionOffset, isLeft);
        mCarouseView.onPageScrolled(position, positionOffset, positionOffsetPixels);
    }

    @Override
    public void onPageSelected(int position) {
        if (position != mCurPosition) {
            scaleViews(position, 0, false);
            scaleViews(position, 0, true);
        }
        mCurPosition = position;
        mCarouseView.setCurrPosition(position);
        mCarouseView.onPageSelected(position);
    }

    @Override
    public void onPageScrollStateChanged(int state) {
        mCarouseView.onPageScrollStateChanged(state);
    }

    private void scaleViews(int position, float progress, boolean isLeft) {
        if (mPagerAdapter.getCacheViews() == null) return;
        if (isLeft) {
            doScale(mPagerAdapter.getCacheViews().get(position - 1), progress);
        } else {
            doScale(mPagerAdapter.getCacheViews().get(position + 1), progress);
        }
        doScale(mPagerAdapter.getCacheViews().get(position), 1 - progress);
        for (int i = 0; i < mPagerAdapter.getCacheViews().size(); i++) {
            if (i != position - 1 && i != position + 1 && i != position) {
                doScale(mPagerAdapter.getCacheViews().get(i), 0);
            }
        }

    }

    public void setScale(float mScale) {
        this.mScale = mScale;
    }

    private void doScale(View view, float progress) {
        if (view == null) return;
        float scale = mScale + progress * (1f - mScale);
        view.setScaleX(scale);
        view.setScaleY(scale);
    }
}