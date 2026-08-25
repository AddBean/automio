// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.carlos.ui.loading;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.animation.Interpolator;

import androidx.interpolator.view.animation.FastOutLinearInInterpolator;

import com.blankj.utilcode.util.ConvertUtils;


public class MaterialLoadingRenderer extends LoadingRenderer {
    private static final Interpolator MATERIAL_INTERPOLATOR = new FastOutLinearInInterpolator();

    private static final int DEGREE_360 = 360;
    private static final int NUM_POINTS = 20;

    private static final float MAX_SWIPE_DEGREES = 1f * DEGREE_360;
    private static final float FULL_GROUP_ROTATION = 3.0f * DEGREE_360;

    private static final float COLOR_START_DELAY_OFFSET = 1f;
    private static final float END_TRIM_DURATION_OFFSET = 1f;
    private static final float START_TRIM_DURATION_OFFSET = 0.5f;

    private static final float DEFAULT_CENTER_RADIUS = 30f;
    private static final float DEFAULT_STROKE_WIDTH = 6f;

    private static final int[] DEFAULT_COLORS = new int[]{
              Color.parseColor("#EF7945")
             ,Color.parseColor("#FDF0E5")
    };

    private final Paint mPaint = new Paint();
    private final RectF mTempBounds = new RectF();

    private final Animator.AnimatorListener mAnimatorListener = new AnimatorListenerAdapter() {
        @Override
        public void onAnimationRepeat(Animator animator) {
            super.onAnimationRepeat(animator);
            storeOriginals();

            mStartDegrees = mEndDegrees;
            mRotationCount = (mRotationCount + 1) % (NUM_POINTS);
        }

        @Override
        public void onAnimationStart(Animator animation) {
            super.onAnimationStart(animation);
            mRotationCount = 0;
        }
    };

    private int[] mColors;
    private int mColorIndex;
    private int mCurrentColor;

    private float mStrokeInset;

    private float mRotationCount;
    private float mGroupRotation;

    private float mEndDegrees;
    private float mEndDegrees2;
    private float mStartDegrees;
    private float mStartDegrees2;

    private float mSwipeDegrees;
    private float mSwipeDegrees2;
    private float mOriginEndDegrees;
    private float mOriginStartDegrees;

    private float mStrokeWidth;
    private float mCenterRadius;

    private MaterialLoadingRenderer(Context context) {
        super(context);
        init(context);
        setupPaint();
        addRenderListener(mAnimatorListener);
    }

    private void init(Context context) {
        mStrokeWidth = ConvertUtils.dp2px(DEFAULT_STROKE_WIDTH);
        mCenterRadius = ConvertUtils.dp2px(DEFAULT_CENTER_RADIUS);

        mColors = DEFAULT_COLORS;

        setColorIndex(0);
        initStrokeInset(mWidth, mHeight);
    }

    private void setupPaint() {
        mPaint.setAntiAlias(true);
        mPaint.setStrokeWidth(mStrokeWidth);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
    }

    @Override
    protected void draw(Canvas canvas) {
        int saveCount = canvas.save();

        mTempBounds.set(mBounds);
        mTempBounds.inset(mStrokeInset, mStrokeInset);

        mPaint.setColor(mColors[0]);
        if(mSwipeDegrees > 0){
            canvas.drawArc(mTempBounds, mStartDegrees, mSwipeDegrees, false, mPaint);
        }

        mPaint.setColor(mColors[1]);
        if(mSwipeDegrees2 > 0){
//            Log.e(">>>","draw 2 >" + mSwipeDegrees2);
            canvas.drawArc(mTempBounds, mStartDegrees2, mSwipeDegrees2, false, mPaint);
        }

        canvas.restoreToCount(saveCount);
    }

    @Override
    protected void computeRender(float renderProgress) {
//        updateRingColor(renderProgress);
//        Log.e(">>>","renderProgress = " + renderProgress);
        //0 - 0.5    start1 = -90   end1 = change with render progress
        if (renderProgress < START_TRIM_DURATION_OFFSET) {

            mEndDegrees2 = -90;
            mStartDegrees2 = -90;
            mStartDegrees = -90;
            float trimProgress = renderProgress / START_TRIM_DURATION_OFFSET;
            mEndDegrees = mOriginEndDegrees + MAX_SWIPE_DEGREES
                    * MATERIAL_INTERPOLATOR.getInterpolation(trimProgress);
        }

        //0.5 - 1
        if (renderProgress < END_TRIM_DURATION_OFFSET
            && renderProgress >= START_TRIM_DURATION_OFFSET ) {

            float trimProgress = (renderProgress - 0.5f) / START_TRIM_DURATION_OFFSET;

            mEndDegrees = mOriginEndDegrees + MAX_SWIPE_DEGREES;
            mEndDegrees2 = mOriginEndDegrees + MAX_SWIPE_DEGREES
                    * MATERIAL_INTERPOLATOR.getInterpolation(trimProgress);

            mStartDegrees = mOriginStartDegrees + MAX_SWIPE_DEGREES
                    * MATERIAL_INTERPOLATOR.getInterpolation(trimProgress);
            mStartDegrees2 = mStartDegrees;
            mEndDegrees2 = mStartDegrees2 + MAX_SWIPE_DEGREES * 0.25f;

            mEndDegrees2 = Math.min(mOriginEndDegrees + MAX_SWIPE_DEGREES,mEndDegrees2);
        }

        mSwipeDegrees = mEndDegrees - mStartDegrees;

        mSwipeDegrees2 = mEndDegrees2 - mStartDegrees2;
    }

    @Override
    protected void setAlpha(int alpha) {
        mPaint.setAlpha(alpha);
    }

    @Override
    protected void setColorFilter(ColorFilter cf) {
        mPaint.setColorFilter(cf);
    }

    @Override
    protected void reset() {
        resetOriginals();
    }

    private void setColorIndex(int index) {
        mColorIndex = index;
        mCurrentColor = mColors[mColorIndex];
    }

    private int getNextColor() {
        return mColors[getNextColorIndex()];
    }

    private int getNextColorIndex() {
        return (mColorIndex + 1) % (mColors.length);
    }

    private void initStrokeInset(float width, float height) {
        float minSize = Math.min(width, height);
        float strokeInset = minSize / 2.0f - mCenterRadius;
        float minStrokeInset = (float) Math.ceil(mStrokeWidth / 2.0f);
        mStrokeInset = strokeInset < minStrokeInset ? minStrokeInset : strokeInset;
    }

    private void storeOriginals() {
    }

    private void resetOriginals() {
        mOriginEndDegrees = -90;
        mOriginStartDegrees = -90;

        mEndDegrees = -90;
        mStartDegrees = -90;

        mStartDegrees2 = mEndDegrees;
        mEndDegrees2 = mEndDegrees;
    }

    private int getStartingColor() {
        return mColors[mColorIndex];
    }

    private void updateRingColor(float interpolatedTime) {
        if (interpolatedTime >= COLOR_START_DELAY_OFFSET) {
            mCurrentColor = evaluateColorChange((interpolatedTime - COLOR_START_DELAY_OFFSET)
                    / (1.0f - COLOR_START_DELAY_OFFSET), getStartingColor(), getNextColor());
        }
    }

    private int evaluateColorChange(float fraction, int startValue, int endValue) {
        int startA = (startValue >> 24) & 0xff;
        int startR = (startValue >> 16) & 0xff;
        int startG = (startValue >> 8) & 0xff;
        int startB = startValue & 0xff;

        int endA = (endValue >> 24) & 0xff;
        int endR = (endValue >> 16) & 0xff;
        int endG = (endValue >> 8) & 0xff;
        int endB = endValue & 0xff;

        return ((startA + (int) (fraction * (endA - startA))) << 24)
                | ((startR + (int) (fraction * (endR - startR))) << 16)
                | ((startG + (int) (fraction * (endG - startG))) << 8)
                | ((startB + (int) (fraction * (endB - startB))));
    }

    private void apply(Builder builder) {
        this.mWidth = builder.mWidth > 0 ? builder.mWidth : this.mWidth;
        this.mHeight = builder.mHeight > 0 ? builder.mHeight : this.mHeight;
        this.mStrokeWidth = builder.mStrokeWidth > 0 ? builder.mStrokeWidth : this.mStrokeWidth;
        this.mCenterRadius = builder.mCenterRadius > 0 ? builder.mCenterRadius : this.mCenterRadius;

        this.mDuration = builder.mDuration > 0 ? builder.mDuration : this.mDuration;

        this.mColors = builder.mColors != null && builder.mColors.length > 0 ? builder.mColors : this.mColors;

        setColorIndex(0);
        setupPaint();
        initStrokeInset(this.mWidth, this.mHeight);
    }

    public static class Builder {
        private final Context mContext;

        private int mWidth;
        private int mHeight;
        private int mStrokeWidth;
        private int mCenterRadius;

        private int mDuration;

        private int[] mColors;

        public Builder(Context mContext) {
            this.mContext = mContext;
        }

        public Builder setWidth(int width) {
            this.mWidth = width;
            return this;
        }

        public Builder setHeight(int height) {
            this.mHeight = height;
            return this;
        }

        public Builder setStrokeWidth(int strokeWidth) {
            this.mStrokeWidth = strokeWidth;
            return this;
        }

        public Builder setCenterRadius(int centerRadius) {
            this.mCenterRadius = centerRadius;
            return this;
        }

        public Builder setDuration(int duration) {
            this.mDuration = duration;
            return this;
        }

        public Builder setColors(int[] colors) {
            this.mColors = colors;
            return this;
        }

        public MaterialLoadingRenderer build() {
            MaterialLoadingRenderer loadingRenderer = new MaterialLoadingRenderer(mContext);
            loadingRenderer.apply(this);
            return loadingRenderer;
        }
    }
}
