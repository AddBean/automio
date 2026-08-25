// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.views.widgets;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import com.hive.views.R;

public class ProgressView extends View {

    private float mTargetPercent = 0.5f;
    private float mCurrentPercent = 0;
    private float mDurPercent = 0.02f;
    private boolean isAnimRunning = true;
    private float mPercent = 0f;
    private int mBackgroundColor = Color.GRAY;
    private int mColor = Color.BLUE;
    private boolean mRoundEnable = true;
    private boolean isAnimEnable = true;
    private float mPadding = -1;
    private float mLineWidth = -1;

    public ProgressView(Context context) {
        super(context);
    }

    public ProgressView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ProgressView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(attrs);
    }

    private void initView(AttributeSet attrs) {
        if (attrs != null) {
            TypedArray ta = getContext().obtainStyledAttributes(attrs, R.styleable.ProgressView);
            mColor = ta.getColor(R.styleable.ProgressView_progressFrontColor, Color.BLUE);
            mBackgroundColor = ta.getColor(R.styleable.ProgressView_progressBackColor, Color.GRAY);
            mRoundEnable = ta.getBoolean(R.styleable.ProgressView_progressRound, true);
            isAnimEnable = ta.getBoolean(R.styleable.ProgressView_progressAnimEnable, true);
            float percent = ta.getFloat(R.styleable.ProgressView_progressPercent, 0.5f);
            mPadding = ta.getDimension(R.styleable.ProgressView_progressPadding, -1);
            mLineWidth = ta.getDimension(R.styleable.ProgressView_progressLineWidth, -1);
            setPercent(percent);
            ta.recycle();
            invalidate();
        }

    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawProgress(canvas);
        if (isAnimEnable)
            evolution();
    }

    private void drawProgress(Canvas canvas) {
        Paint paint = new Paint();
        if (mRoundEnable) {
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStrokeJoin(Paint.Join.ROUND);
        }
        paint.setStyle(Paint.Style.STROKE);
        paint.setAntiAlias(true);
        paint.setAlpha(100);
        float width = 0;
        if (getHeight() > getWidth()) {//垂直；
            width = getWidth() / 2;
            paint.setStrokeWidth(mLineWidth < 0 ? width : mLineWidth);
            RectF rectF = new RectF(0, 0, getWidth(), getHeight());
            rectF.inset(mPadding < 0 ? width / 2 : mPadding, mPadding < 0 ? width / 2 : mPadding);
            paint.setColor(mBackgroundColor);
            canvas.drawLine(rectF.centerX(), rectF.bottom - rectF.height() * mCurrentPercent, rectF.centerX(), rectF.top, paint);
            paint.setAlpha(200);
            paint.setColor(mColor);
            if (mCurrentPercent > 0)
                canvas.drawLine(rectF.centerX(), rectF.bottom, rectF.centerX(), rectF.bottom - rectF.height() * mCurrentPercent, paint);
        } else {
            width = getHeight();
            paint.setStrokeWidth(mLineWidth < 0 ? width : mLineWidth);
            RectF rectF = new RectF(0, 0, getWidth(), getHeight());
            rectF.inset(mPadding < 0 ? width / 2 : mPadding, mPadding < 0 ? width / 2 : mPadding);
            paint.setColor(mBackgroundColor);
            canvas.drawLine(rectF.left + rectF.width() * mCurrentPercent, rectF.centerY(), rectF.right, rectF.centerY(), paint);
            paint.setAlpha(200);
            paint.setColor(mColor);
            if (mCurrentPercent > 0)
                canvas.drawLine(rectF.left, rectF.centerY(), rectF.left + rectF.width() * mCurrentPercent, rectF.centerY(), paint);
        }

    }

    public void evolution() {
        if (isAnimRunning) {
            float dur = mTargetPercent - mCurrentPercent;
            if (Math.abs(dur) >= mDurPercent) {
                mCurrentPercent += dur > 0 ? mDurPercent : -mDurPercent;
                if (mCurrentPercent > 1f)
                    mCurrentPercent = 1f;
                if (mCurrentPercent < 0f)
                    mCurrentPercent = 0f;
                if (mTargetPercent - mCurrentPercent < mDurPercent) {
                    mCurrentPercent = mTargetPercent;
                }
                if (mCurrentPercent < mDurPercent) {
                    mCurrentPercent = 0;
                }
            } else {
                isAnimRunning = false;
            }
            invalidate();

        }
    }

    public void setPercent(float percent) {
        if (!isAnimEnable) {
            mCurrentPercent = percent;
        }
        mPercent = percent;
        mTargetPercent = mPercent;
        isAnimRunning = true;
        invalidate();
    }

    public void setAnimEnable(boolean enable) {
        isAnimEnable = enable;
        invalidate();
    }

    public float getPercent() {
        return mPercent;
    }

    public void setBackgroundColor(int mBackgroundColor) {
        this.mBackgroundColor = mBackgroundColor;
    }

    public void setColor(int mColor) {
        this.mColor = mColor;
    }
}