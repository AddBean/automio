// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.richeditor.core;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.ColorInt;

import com.hive.utils.GlobalApp;

/**
 * Created by Administrator.
 */

public abstract class BaseLayout extends FrameLayout {
    private int layoutId;
    private View mView;
    protected int DP = 1;

    public BaseLayout(Context context) {
        super(context);
        initBaseLayout();
    }

    public BaseLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        initBaseLayout();
    }

    public BaseLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initBaseLayout();
    }

    private void initBaseLayout() {
        DP = dpConvertToPx(getContext(), 1);
        mView = LayoutInflater.from(getContext()).inflate(getLayoutId(), this);
//        this.addView(mView);
        initView(mView);
    }

    protected abstract void initView(View view);

    public abstract int getLayoutId();
@ColorInt
    protected int getColor(int colorId) {
        return GlobalApp.getColor(colorId);
    }


    public static int dpConvertToPx(Context context, int dp) {
        float scale = context.getResources().getDisplayMetrics().density;
        int px = (int) ((float) dp * scale + 0.5F);
        return px;
    }

    public String getString(int resId) {
        return GlobalApp.getString(resId);
    }



}
