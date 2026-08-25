// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.views.widgets;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import androidx.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

public class CustomIndexDotView extends View {

    protected int mMaxIndex;
    protected Paint mPaint;
    protected int mPosition = 0;

    public CustomIndexDotView(Context context) {
        super(context);
        initView();
    }

    public CustomIndexDotView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initView();
    }

    public CustomIndexDotView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView();
    }

    private void initView() {
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setStyle(Paint.Style.FILL);
//        mPaint.setStrokeCap(Paint.Cap.ROUND);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawDots(canvas);
    }

    private void drawDots(Canvas canvas) {
        if(mMaxIndex<=0)return;
        int width = getMeasuredWidth();
        int height = getMeasuredHeight();
        int offset = width / (mMaxIndex);
        mPaint.setColor(0xffdadada);
        for (int i = 0; i < mMaxIndex; i++) {
            canvas.drawCircle(offset * i, height / 2, height / 2, mPaint);
        }
        mPaint.setColor(0xffFCAA01);
        mPaint.setStrokeWidth(height);
        canvas.drawCircle(offset * (mPosition), height / 2, height / 2, mPaint);
    }

    public void setMaxIndex(int maxIndex) {
        this.mMaxIndex = maxIndex;
        invalidate();
    }

    public void selectIndex(int position) {
        mPosition = position;
        invalidate();
    }
}
