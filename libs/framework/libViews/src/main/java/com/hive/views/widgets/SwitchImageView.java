// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.views.widgets;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;

import androidx.appcompat.widget.AppCompatImageView;

import com.hive.views.R;

public class SwitchImageView extends AppCompatImageView implements View.OnClickListener {
    private Context mContext;
    private Boolean mSwitchStatus = false;
    private Drawable mDrawableChecked;
    private Drawable mDrawableUnchecked;
    private int mDrawableCheckedTint;
    private int mDrawableUncheckedTint;
    private boolean mDisableClick = true;

    public SwitchImageView(Context context) {
        super(context);
        initView(null);
    }

    public SwitchImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView(attrs);
    }

    public SwitchImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(attrs);
    }

    private void initView(AttributeSet attrs) {
        this.setOnClickListener(this);
        if (attrs == null) return;
        TypedArray typedArray = getContext().obtainStyledAttributes(attrs, R.styleable.SwitchImageView);
        mSwitchStatus = typedArray.getBoolean(R.styleable.SwitchImageView_checked, false);
        mDisableClick = typedArray.getBoolean(R.styleable.SwitchImageView_disable_click, false);
        if (mDisableClick) {
            this.setOnClickListener(null);
        }
        int drawableCheckedId = typedArray.getResourceId(R.styleable.SwitchImageView_res_checked, R.drawable.setting_on);
        int drawableUncheckedId = typedArray.getResourceId(R.styleable.SwitchImageView_res_unchecked, R.drawable.setting_off);
        mDrawableChecked = getResources().getDrawable(drawableCheckedId);
        mDrawableUnchecked = getResources().getDrawable(drawableUncheckedId);
        if (typedArray.hasValue(R.styleable.SwitchImageView_res_checked_tint)) {
            mDrawableCheckedTint = typedArray.getColor(R.styleable.SwitchImageView_res_checked_tint, -1);
            mDrawableChecked.setColorFilter(mDrawableCheckedTint, PorterDuff.Mode.SRC_ATOP);
        }
        if (typedArray.hasValue(R.styleable.SwitchImageView_res_checked_tint)) {
            mDrawableUncheckedTint = typedArray.getColor(R.styleable.SwitchImageView_res_unchecked_tint, -1);
            mDrawableUnchecked.setColorFilter(mDrawableUncheckedTint, PorterDuff.Mode.SRC_ATOP);
        }

        setSwitchStatus(mSwitchStatus);
        typedArray.recycle();
    }

    /**
     * 设置开关状态；
     */
    public void setSwitchStatus(Boolean isOn) {
        this.mSwitchStatus = isOn;
        this.setImageDrawable(mSwitchStatus ? mDrawableChecked : mDrawableUnchecked);
        invalidate();
    }

    public void setDrawableChecked(int drawableChecked) {
        this.mDrawableChecked = getResources().getDrawable(drawableChecked);
        setSwitchStatus(mSwitchStatus);
    }

    public void setDrawableUnchecked(int drawableUnchecked) {
        this.mDrawableUnchecked = getResources().getDrawable(drawableUnchecked);
        setSwitchStatus(mSwitchStatus);
    }

    /**
     * 获取开关状态
     */
    public Boolean getSwitchStatus() {
        return this.mSwitchStatus;
    }

    @Override
    public void onClick(View v) {
        setSwitchStatus(!mSwitchStatus);
        if (mOnSwitcherListener != null) mOnSwitcherListener.onStateChanged(mSwitchStatus);
    }

    public OnSwitcherListener mOnSwitcherListener;

    public void setOnSwitcherListener(OnSwitcherListener mOnSwitcherListener) {
        this.mOnSwitcherListener = mOnSwitcherListener;
    }

    public interface OnSwitcherListener {
        void onStateChanged(boolean status);
    }
}
