// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.views.carousel;

import android.content.Context;
import android.view.animation.Interpolator;
import android.widget.Scroller;

public class InfiniteSpeedScroller extends Scroller {

    public int mDuration = 1500;

    public InfiniteSpeedScroller(Context context) {
        super(context);
    }

    public InfiniteSpeedScroller(Context context, Interpolator interpolator) {
        super(context, interpolator);
    }

    public InfiniteSpeedScroller(Context context, Interpolator interpolator, boolean flywheel) {
        super(context, interpolator, flywheel);
    }

    @Override
    public void startScroll(int startX, int startY, int dx, int dy) {
        startScroll(startX, startY, dx, dy, mDuration);
    }

    @Override
    public void startScroll(int startX, int startY, int dx, int dy, int duration) {
        super.startScroll(startX, startY, dx, dy, mDuration);
    }

    public int getFixedDuration() {
        return mDuration;
    }

    public void setFixedDuration(int duration) {
        mDuration = duration;
    }
}