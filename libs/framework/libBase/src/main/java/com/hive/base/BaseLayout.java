// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.base;

import android.content.Context;

import androidx.annotation.ColorInt;

import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RelativeLayout;

import com.hive.utils.GlobalApp;

/**
 * Created by Admin.
 */

public abstract class BaseLayout extends RelativeLayout {
    private int layoutId;
    protected View mView;
    protected int DP = 1;

    public BaseLayout(Context context) {
        super(context);
        initAttrs(null);
        initBaseLayout();
    }

    public BaseLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        initAttrs(attrs);
        initBaseLayout();
    }

    public BaseLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initAttrs(attrs);
        initBaseLayout();
    }

    protected void initBaseLayout() {
        DP = dp2Px(getContext(), 1);
        layoutId = getLayoutId();
        if (layoutId != -1)
            mView = LayoutInflater.from(getContext()).inflate(getLayoutId(), this);
        initView(mView);
    }

    protected void initAttrs(AttributeSet attrs) {
    }

    protected abstract void initView(View view);

    public abstract int getLayoutId();

    @ColorInt
    protected int getColor(int colorId) {
        return GlobalApp.getColor(colorId);
    }


    public static int dp2Px(Context context, int dp) {
        float scale = context.getResources().getDisplayMetrics().density;
        int px = (int) ((float) dp * scale + 0.5F);
        return px;
    }

    public String getString(int resId) {
        return GlobalApp.getString(resId);
    }

    public String getString(int resId, Object... formatArgs) {
        return GlobalApp.getString(resId, formatArgs);
    }

}
