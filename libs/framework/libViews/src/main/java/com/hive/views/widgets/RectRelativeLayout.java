// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.views.widgets;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.RelativeLayout;

import com.hive.views.R;

public class RectRelativeLayout extends RelativeLayout {
    private float mRate = -1f;
    public RectRelativeLayout(Context context) {
        super(context);

    }

    public RectRelativeLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView(context, attrs);
    }

    public RectRelativeLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context, attrs);
    }

    private void initView(Context context, AttributeSet attrs) {
        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs,
                    R.styleable.RectRelativeLayout);
            mRate = a.getFloat(R.styleable.RectRelativeLayout_rectRate, 1f);// 默认为10dp
            a.recycle();
        }
    }

    public void setmRate(float mRate) {
        this.mRate = mRate;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if(mRate>0){
            widthMeasureSpec=MeasureSpec.makeMeasureSpec(widthMeasureSpec,MeasureSpec.EXACTLY);
            int width = MeasureSpec.getSize(widthMeasureSpec);
            int height = (int) (width * mRate);
            heightMeasureSpec=MeasureSpec.makeMeasureSpec(height,MeasureSpec.EXACTLY);
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }
}
