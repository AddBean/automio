// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.views.widgets.effect_text;


import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.view.View;

import java.util.Random;

/**
 * Created by AddBean on 2016/10/20.
 */

public class EffectTextDotView implements IEffectTextDot {
    Paint paint = new Paint();
    private EffectTextDot mData;
    private Point mStartPoint;
    private Point mTargetPoint;
    private Point mCurrPoint;
    private int mAlpha = 0;
    private float mCurSize = 0;
    private float mGapSize = 0;

    public EffectTextDotView(View v, EffectTextDot mData) {
        this.mData = mData;
        mStartPoint = getRandomPoint(v);
        mTargetPoint = mData.getmLoc();
        mGapSize = new Random().nextInt(mData.getmSize()*3);//初始大小；
        mCurrPoint = new Point(mStartPoint);
    }

    @Override
    public void onDraw(Canvas canvas) {
        if (mData == null) return;
        int count = canvas.save();
        paint.setStyle(Paint.Style.FILL);
        paint.setAntiAlias(true);
        paint.setColor(mData.getmColor());
        paint.setAlpha(mAlpha);
        canvas.drawCircle(mCurrPoint.x, mCurrPoint.y, mCurSize, paint);
        canvas.restoreToCount(count);
    }

    @Override
    public void evolveDot(float value) {
        if (mData.getmState() == 0) {
            onEnter(value);
        } else {
            onDismiss(value);
            if (value == 1.0f)
                mData.setmState(2);
        }
        if (mData.getmState() == 0 && value == 1.0f)
            mData.setmState(1);
    }

    /**
     * 入场动画;
     *
     * @param value
     */
    private void onEnter(float value) {
        float dx = mTargetPoint.x - mStartPoint.x;
        float dy = mTargetPoint.y - mStartPoint.y;
        mAlpha = (int) (value * 255);
        mCurSize = mGapSize * (1-value) + mData.getmSize() / 2.4f;
        mCurrPoint.set((int) (mStartPoint.x + dx * value), (int) (mStartPoint.y + dy * value));
    }

    /**
     * 出场动画；
     *
     * @param value
     */
    private void onDismiss(float value) {
        float dx = mTargetPoint.x - mStartPoint.x;
        float dy = mTargetPoint.y - mStartPoint.y;
        mAlpha = 255 - (int) (value * 255);
        mCurSize = mData.getmSize() / 2.6f*(1-value);
        mCurrPoint.set((int) (mTargetPoint.x - dx * value), (int) (mTargetPoint.y - dy * value));
    }

    public Point getRandomPoint(View v) {
        Random r = new Random();
        float baseR = Math.max(v.getMeasuredWidth(), v.getMeasuredHeight()) / 2;
        float R = r.nextInt((int) baseR);
        Double rd = r.nextFloat() * 2 * Math.PI;
        return new Point((int) (R * Math.cos(rd)) + v.getMeasuredWidth() / 2, (int) (R * Math.sin(rd)) + v.getMeasuredHeight() / 2);
    }

    public EffectTextDot getmData() {
        return mData;
    }
}