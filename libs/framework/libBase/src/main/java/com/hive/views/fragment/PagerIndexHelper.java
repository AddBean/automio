// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.views.fragment;

import android.graphics.Canvas;
import android.view.View;

public class PagerIndexHelper {
    private int mPosition;
    private float mPositionOffset;
    private PagerTitleScroller mScroller;
    private com.hive.views.view_pager.PagerTitleScroller mScroller2;
    private Canvas mConvas;

    public PagerIndexHelper() {
    }

    public void setPosition(PagerTitleScroller scroller, Canvas convas, int position, float positionOffset) {
        this.mScroller = scroller;
        this.mPosition = position;
        this.mPositionOffset = positionOffset;
        this.mConvas = convas;
    }

    public void setPosition(com.hive.views.view_pager.PagerTitleScroller scroller, Canvas convas, int position, float positionOffset) {
        this.mScroller2 = scroller;
        this.mPosition = position;
        this.mPositionOffset = positionOffset;
        this.mConvas = convas;
    }

    public void setCallback(OnCovertCallback callback) {
        if (callback == null) return;
        int centerPrevX = 0;
        int centerNextX = 0;
        int centerX = 0;
        int tempX1 = 0;
        int tempX2 = 0;
        int tempW = 0;
        if (mScroller != null) {
            if (mPosition < mScroller.getChildCount() - 1) {
                View view0 = mScroller.getChildAt(mPosition);
                View view1 = mScroller.getChildAt(mPosition + 1);
                centerPrevX = (int) (view0.getX() + view0.getMeasuredWidth() / 2);
                centerNextX = (int) (view1.getX() + view1.getMeasuredWidth() / 2);
                centerX = (int) (centerPrevX + (centerNextX - centerPrevX) * mPositionOffset);
                tempW = (int) (view0.getMeasuredWidth() + (view1.getMeasuredWidth() - view0.getMeasuredWidth()) * mPositionOffset);
                tempX1 = (centerX - tempW / 2);
                tempX2 = (centerX + tempW / 2);
                callback.onCovertFinished(mConvas, tempX1, mScroller.getMeasuredHeight(), tempX2, mScroller.getMeasuredHeight());
            } else if (mPosition == mScroller.getChildCount() - 1) {
                tempX1 = (int) mScroller.getChildAt(mPosition).getX();
                tempX2 = (int) (mScroller.getChildAt(mPosition).getX() + mScroller.getChildAt(mPosition).getMeasuredWidth());
                callback.onCovertFinished(mConvas, tempX1, mScroller.getMeasuredHeight(), tempX2, mScroller.getMeasuredHeight());
            }
        }

        if (mScroller2 != null) {
            if (mPosition < mScroller2.getChildCount() - 1) {
                View view0 = mScroller2.getChildAt(mPosition);
                View view1 = mScroller2.getChildAt(mPosition + 1);
                centerPrevX = (int) (view0.getX() + view0.getMeasuredWidth() / 2);
                centerNextX = (int) (view1.getX() + view1.getMeasuredWidth() / 2);
                centerX = (int) (centerPrevX + (centerNextX - centerPrevX) * mPositionOffset);
                tempW = (int) (view0.getMeasuredWidth() + (view1.getMeasuredWidth() - view0.getMeasuredWidth()) * mPositionOffset);
                tempX1 = (centerX - tempW / 2);
                tempX2 = (centerX + tempW / 2);
                callback.onCovertFinished(mConvas, tempX1, mScroller2.getMeasuredHeight(), tempX2, mScroller2.getMeasuredHeight());
            } else if (mPosition == mScroller2.getChildCount() - 1) {
                tempX1 = (int) mScroller2.getChildAt(mPosition).getX();
                tempX2 = (int) (mScroller2.getChildAt(mPosition).getX() + mScroller2.getChildAt(mPosition).getMeasuredWidth());
                callback.onCovertFinished(mConvas, tempX1, mScroller2.getMeasuredHeight(), tempX2, mScroller2.getMeasuredHeight());
            }
        }
    }

    public interface OnCovertCallback {
        void onCovertFinished(Canvas canvas, int x1, int y1, int x2, int y2);
    }
}
