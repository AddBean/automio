// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.richeditor.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;

/**
 * Created by Administrator on 2017/7/7.
 */

public class ItemColor extends View {
    @ColorInt
    public int mColor = 0xff000000;
    private Paint mPaint;
    private int DP;
    private int mSelectedColor=0xffffffff;

    public ItemColor(Context context) {
        super(context);
        initView();
    }

    public ItemColor(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initView();
    }

    public ItemColor(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView();
    }

    private void initView() {
        DP = dp2px(1);
        mPaint = new Paint();
        mPaint.setStrokeWidth(1 * DP);
        mPaint.setAntiAlias(true);
    }

    public int dp2px(int dp) {
        float scale = getContext().getResources().getDisplayMetrics().density;
        int px = (int) ((float) dp * scale + 0.5F);
        return px;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawBackgrounp(canvas);
        if (isSelected())
            drawSelector(canvas);
    }

    private void drawBackgrounp(Canvas canvas) {
        Rect rect = new Rect();
        canvas.getClipBounds(rect);
        rect.inset(1*DP,1*DP);
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setColor(getmColor());
        canvas.drawOval(new RectF(rect), mPaint);
    }

    public void setmSelectedColor(int mSelectedColor) {
        this.mSelectedColor = mSelectedColor;
        invalidate();
    }

    private void drawSelector(Canvas canvas) {
        Rect rect = new Rect();
        canvas.getClipBounds(rect);
        rect.inset(1*DP,1*DP);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setColor(mSelectedColor);
        canvas.drawOval(new RectF(rect), mPaint);
    }

    public int getmColor() {
        return mColor;
    }

    public void setmColor(int mColor) {
        this.mColor = mColor;
        invalidate();
    }

    @Override
    public void setSelected(boolean selected) {
        super.setSelected(selected);
        invalidate();
    }
}
