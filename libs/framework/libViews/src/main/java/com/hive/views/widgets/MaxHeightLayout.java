// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.views.widgets;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import com.hive.views.R;

/**
 * Created by carbs on 2016/5/12.
 */

public class MaxHeightLayout extends LinearLayout {


    private static final float DEFAULT_MAX_HEIGHT = 0f;

    private float mMaxHeight = DEFAULT_MAX_HEIGHT;// 优先级低

    public MaxHeightLayout(Context context) {
        super(context);
    }

    public MaxHeightLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        initAttrs(context, attrs);
    }

    public MaxHeightLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initAttrs(context, attrs);
    }

    private void initAttrs(Context context, AttributeSet attrs) {
        TypedArray a = context.obtainStyledAttributes(attrs,
                R.styleable.MaxHeightLayout);

        final int count = a.getIndexCount();
        for (int i = 0; i < count; ++i) {
            int attr = a.getIndex(i);
            if (attr == R.styleable.MaxHeightLayout_heightDimen) {
                mMaxHeight = a.getDimension(attr, DEFAULT_MAX_HEIGHT);
            }
        }
        a.recycle();
    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (mMaxHeight > 0) {
            int heightMode = MeasureSpec.getMode(heightMeasureSpec);
            int heightSize = MeasureSpec.getSize(heightMeasureSpec);

            if (heightMode == MeasureSpec.EXACTLY) {
                heightSize = heightSize <= mMaxHeight ? heightSize
                        : (int) mMaxHeight;
            }

            if (heightMode == MeasureSpec.UNSPECIFIED) {
                heightSize = heightSize <= mMaxHeight ? heightSize
                        : (int) mMaxHeight;
            }
            if (heightMode == MeasureSpec.AT_MOST) {
                heightSize = heightSize <= mMaxHeight ? heightSize
                        : (int) mMaxHeight;
            }
            int maxHeightMeasureSpec = MeasureSpec.makeMeasureSpec(heightSize,
                    heightMode);
            super.onMeasure(widthMeasureSpec, maxHeightMeasureSpec);
        }else{
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }

    }

}
