// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.views.widgets;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.util.AttributeSet;

import androidx.annotation.Nullable;

import com.hive.views.R;
import com.hive.views.utils.RoundLayoutHelper;

/**
 * @author jiadou
 * @email 172111432@qq.com
 * @date 4/21/21
 */
public class RectRoundImageView extends androidx.appcompat.widget.AppCompatImageView {
    private float mRate = -1f;
    private RoundLayoutHelper mRoundHelper;

    public RectRoundImageView(Context context) {
        super(context);
        initView(context, null);
    }

    public RectRoundImageView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initView(context, attrs);
    }

    public RectRoundImageView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context, attrs);
    }


    private void initView(Context context, AttributeSet attrs) {
        if (attrs != null) {
            mRoundHelper = new RoundLayoutHelper(false);
            mRoundHelper.initAttributeSet(context, attrs);
            TypedArray a = context.obtainStyledAttributes(attrs,
                    R.styleable.RectImageView);
            mRate = a.getFloat(R.styleable.RectImageView_sizeRate, 1f);// 默认为10dp
            a.recycle();
        }
    }

    public void setRate(float mRate) {
        this.mRate = mRate;
    }

    public void setColor(int color) {
        mRoundHelper.color = color;
        invalidate();
    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (mRate > 0) {
            widthMeasureSpec = MeasureSpec.makeMeasureSpec(widthMeasureSpec, MeasureSpec.EXACTLY);
            int width = MeasureSpec.getSize(widthMeasureSpec);
            int height = (int) (width * mRate);
            heightMeasureSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY);
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
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

}
