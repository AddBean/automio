// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.carlos.ui.header;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;

import com.blankj.utilcode.util.BarUtils;
import com.hive.views.R;


public class StatusBarAutoPaddingView extends LinearLayout {
    public static final int STATUS_BAR_TYPE_NO_PADDING = 0;
    public static final int STATUS_BAR_TYPE_PADDING = 1;
    public static final int STATUS_BAR_TYPE_GONE = 2;
    private int statusBarHeight;
    private int type;

    public StatusBarAutoPaddingView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public StatusBarAutoPaddingView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    public StatusBarAutoPaddingView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    private void init(@Nullable AttributeSet attrs) {
        statusBarHeight = BarUtils.getStatusBarHeight();
        if (attrs != null) {
            TypedArray typedArray = getContext().obtainStyledAttributes(attrs, R.styleable.StatusBarHeightView);
            type = typedArray.getInt(R.styleable.StatusBarHeightView_use_type, 0);
            typedArray.recycle();
        }
        if (type == STATUS_BAR_TYPE_PADDING) {
            setPadding(getPaddingLeft(), statusBarHeight, getPaddingRight(), getPaddingBottom());
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
//        if (type == STATUS_BAR_TYPE_NO_PADDING) {
//            setMeasuredDimension(getDefaultSize(getSuggestedMinimumWidth(), widthMeasureSpec),
//                    statusBarHeight);
//        } else {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
//        }
    }

    public void setStatusBarType(int type) {
        this.type = type;
        if (type == STATUS_BAR_TYPE_PADDING) {
            setPadding(getPaddingLeft(), statusBarHeight, getPaddingRight(), getPaddingBottom());
        } else {
            setPadding(getPaddingLeft(), 0, getPaddingRight(), getPaddingBottom());
        }
        invalidate();
    }
}