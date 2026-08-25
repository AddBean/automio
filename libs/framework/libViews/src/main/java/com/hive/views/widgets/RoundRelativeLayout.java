// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.views.widgets;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import com.hive.views.utils.RoundLayoutHelper;

public class RoundRelativeLayout extends RelativeLayout {
    public RoundLayoutHelper mRoundHelper;

    public RoundRelativeLayout(Context context) {
        this(context, null);
    }

    public RoundRelativeLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RoundRelativeLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mRoundHelper = new RoundLayoutHelper(true);
        mRoundHelper.initAttributeSet(context, attrs);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mRoundHelper.onSizeChanged(w, h);
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        mRoundHelper.saveLayer(canvas);
        super.dispatchDraw(canvas);
        mRoundHelper.dispatchDraw(canvas);
    }
}