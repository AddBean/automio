// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.views.widgets;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;

import com.hive.views.utils.RoundLayoutHelper;

public class RoundCoverView extends View {
    private RoundLayoutHelper mRoundHelper;

    public RoundCoverView(Context context) {
        this(context, null);
    }

    public RoundCoverView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RoundCoverView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mRoundHelper = new RoundLayoutHelper(false);
        mRoundHelper.initAttributeSet(context, attrs);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mRoundHelper.onSizeChanged(w, h);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        mRoundHelper.dispatchDraw(canvas);
    }

//    @Override
//    public void setTag(int key, Object tag) {
//        super.setTag(key, tag);
//        if(null != mRoundHelper && TextUtils.equals(tag == null ? "" : tag.toString(), SkinAttrName.DRAW_VIEW_PAINT_COLOR)){
//            mRoundHelper.setRoundPaintColor(key);
//            postInvalidate();
//        }
//    }
}
