// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.views.widgets.transformer;

import android.view.View;

public class CoverModeTransformer extends ABaseTransformer {

    private float reduceX = 0.0f;
    private float itemWidth = 0;
    private float offsetPosition = 0f;
    private int mCoverWidth;
    private float mScaleMax = 1.0f;
    private float mScaleMin = 0.7f;
    private float mAlphaMax = 0.7f;
    private float mAlphaMin = 0.1f;

    private View shadowMaskView;

    @Override
    protected void onPreTransform(View view, float position) {
        shadowMaskView = (View) view.getTag();
    }

    @Override
    protected void onTransform(View view, float position) {
        if (offsetPosition == 0f && null != view.getParent()) {
            //todo getParent = viewpager
            float paddingLeft = ((View) view.getParent()).getPaddingLeft();
            float paddingRight = ((View) view.getParent()).getPaddingRight();
            float width = ((View) view.getParent()).getMeasuredWidth();
            offsetPosition = paddingLeft / (width - paddingLeft - paddingRight);
        }
        float currentPos = position - offsetPosition;
        if (itemWidth == 0) {
            itemWidth = view.getWidth();
            //由于左右边的缩小而减小的x的大小的一半
            reduceX = (2.0f - mScaleMax - mScaleMin) * itemWidth / 2.0f;
        }
        if (currentPos <= -1.0f) {
            view.setTranslationX(reduceX + mCoverWidth);
            view.setScaleX(mScaleMin);
            view.setScaleY(mScaleMin);
            if (null != shadowMaskView) {
                shadowMaskView.setAlpha(mAlphaMax);
            }
        } else if (currentPos <= 1.0) {
            float scale = (mScaleMax - mScaleMin) * Math.abs(1.0f - Math.abs(currentPos));
            float alpha = (mAlphaMax - mAlphaMin) * Math.abs(currentPos);

            float translationX = currentPos * -reduceX;
            if (currentPos <= -0.5) {//两个view中间的临界，这时两个view在同一层，左侧View需要往X轴正方向移动覆盖的值()
                view.setTranslationX(translationX + mCoverWidth * Math.abs(Math.abs(currentPos) - 0.5f) / 0.5f);
            } else if (currentPos <= 0.0f) {
                view.setTranslationX(translationX);
            } else if (currentPos >= 0.5) {//两个view中间的临界，这时两个view在同一层
                view.setTranslationX(translationX - mCoverWidth * Math.abs(Math.abs(currentPos) - 0.5f) / 0.5f);
            } else {
                view.setTranslationX(translationX);
            }
            view.setScaleX(scale + mScaleMin);
            view.setScaleY(scale + mScaleMin);
            if (null != shadowMaskView) {
                shadowMaskView.setAlpha(alpha + mAlphaMin);
            }
        } else {
            view.setScaleX(mScaleMin);
            view.setScaleY(mScaleMin);
            view.setTranslationX(-reduceX - mCoverWidth);
            if (null != shadowMaskView) {
                shadowMaskView.setAlpha(mAlphaMax);
            }
        }
    }

    @Override
    protected boolean isPagingEnabled() {
        return true;
    }

    @Override
    protected boolean hideOffscreenPages() {
        return false;
    }
}

