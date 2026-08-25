// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.views.widgets;

import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import androidx.annotation.IntDef;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.hive.utils.system.CommonUtils;
import com.hive.views.R;

public class CustomNavigationBar extends RelativeLayout implements View.OnClickListener {
    private ViewHolder mViewHolder;
    private View mView;
    private String mTitleText;
    private int mTitleColor;
    private Drawable mRightDrawable;
    private float mRightPadding;
    private float mRightSize;
    private float mLeftPadding;
    private float mLeftSize;
    private float mTitleSize;
    public static final int LEFT = 0, MID = 1, RIGHT = 2;
    private Drawable mLeftDrawable;
    private float mSpiltLineWidth;
    private int mSpiltLineColor;
    private int mLeftColorFilter;
    private int mRightColorFilter;
    private int DP;


    static class ViewHolder {
        ImageView mIvLeft;
        TextView mTvTitle;
        ImageView mIvRight;
        View mSplitLine;

        ViewHolder(View view) {
            mIvLeft = view.findViewById(R.id.iv_left);
            mTvTitle = view.findViewById(R.id.tv_title);
            mIvRight = view.findViewById(R.id.iv_right);
            mSplitLine = view.findViewById(R.id.split_line);
        }
    }

    public CustomNavigationBar(Context context) {
        super(context);
        initView(null);
    }

    public CustomNavigationBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView(attrs);
    }

    public CustomNavigationBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(attrs);
    }

    private void initView(AttributeSet attrs) {
        mView = LayoutInflater.from(getContext()).inflate(R.layout.custom_navigation_bar, this);
        mViewHolder = new ViewHolder(mView);
        mViewHolder.mIvLeft.setOnClickListener(this);
        mViewHolder.mIvRight.setOnClickListener(this);
        mViewHolder.mTvTitle.setOnClickListener(this);
        DP = CommonUtils.dp2Px(1);
        if (attrs != null) {
            TypedArray ta = getContext().obtainStyledAttributes(attrs, R.styleable.CustomNavigationBar);
            mTitleText = ta.getString(R.styleable.CustomNavigationBar_titleText);
            mTitleColor = ta.getColor(R.styleable.CustomNavigationBar_titleColor, Color.BLACK);
            mTitleSize = ta.getDimensionPixelSize(R.styleable.CustomNavigationBar_titleSize, 14*DP);

            mRightDrawable = ta.getDrawable(R.styleable.CustomNavigationBar_rightDrawable);
            mRightPadding = ta.getDimension(R.styleable.CustomNavigationBar_rightPadding, 12 * DP);
            mRightSize = ta.getDimension(R.styleable.CustomNavigationBar_rightSize, 40 * DP);
            mRightColorFilter = ta.getColor(R.styleable.CustomNavigationBar_rightColorFilter, Color.TRANSPARENT);

            mLeftDrawable = ta.getDrawable(R.styleable.CustomNavigationBar_leftDrawable);
            mLeftPadding = ta.getDimension(R.styleable.CustomNavigationBar_leftPadding, 12 * DP);
            mLeftSize = ta.getDimension(R.styleable.CustomNavigationBar_leftSize, 40 * DP);
            mLeftColorFilter = ta.getColor(R.styleable.CustomNavigationBar_leftColorFilter, Color.TRANSPARENT);


            mSpiltLineWidth = ta.getDimension(R.styleable.CustomNavigationBar_spiltLineWidth, 0.5f * DP);
            mSpiltLineColor = ta.getColor(R.styleable.CustomNavigationBar_spiltLineColor, 0x00000000);


            ta.recycle();
        }
        updateUi();
    }

    private void updateUi() {
        mViewHolder.mTvTitle.setText(mTitleText);
        mViewHolder.mTvTitle.setTextSize(TypedValue.COMPLEX_UNIT_PX, mTitleSize);
        mViewHolder.mTvTitle.setTextColor(mTitleColor);
        RelativeLayout.LayoutParams lp;
        if (mRightDrawable != null)
            mViewHolder.mIvRight.setImageDrawable(mRightDrawable);
        mViewHolder.mIvRight.setPadding((int) mRightPadding, (int) mRightPadding, (int) mRightPadding, (int) mRightPadding);
        lp = (RelativeLayout.LayoutParams) mViewHolder.mIvRight.getLayoutParams();
        lp.height = (int) mRightSize;
        lp.width = (int) mRightSize;
        mViewHolder.mIvRight.setLayoutParams(lp);
        mViewHolder.mIvRight.setColorFilter(mRightColorFilter);

        if (mLeftDrawable != null)
            mViewHolder.mIvLeft.setImageDrawable(mLeftDrawable);
        mViewHolder.mIvLeft.setPadding((int) mLeftPadding, (int) mLeftPadding, (int) mLeftPadding, (int) mLeftPadding);
        lp = (RelativeLayout.LayoutParams) mViewHolder.mIvLeft.getLayoutParams();
        lp.height = (int) mLeftSize;
        lp.width = (int) mLeftSize;
        mViewHolder.mIvLeft.setLayoutParams(lp);
        mViewHolder.mIvLeft.setColorFilter(mLeftColorFilter);


        mViewHolder.mSplitLine.setBackgroundColor(mSpiltLineColor);
        lp = (RelativeLayout.LayoutParams) mViewHolder.mSplitLine.getLayoutParams();
        lp.height = (int) mSpiltLineWidth;
        mViewHolder.mSplitLine.setLayoutParams(lp);

    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.iv_left) {
            if (mOnTitleClickListener != null && mOnTitleClickListener.onTitleClick(LEFT))
                return;

            if (getContext() instanceof Activity) {
                ((Activity) getContext()).finish();
            }
        }
        if (v.getId() == R.id.tv_title) {
            if (mOnTitleClickListener != null && mOnTitleClickListener.onTitleClick(MID))
                return;
        }
        if (v.getId() == R.id.iv_right) {
            if (mOnTitleClickListener != null && mOnTitleClickListener.onTitleClick(RIGHT))
                return;
        }
    }

    public void setmTitleText(String mTitleText) {
        this.mTitleText = mTitleText;
        updateUi();
    }

    public void setmTitleColor(int mTitleColor) {
        this.mTitleColor = mTitleColor;
        updateUi();
    }

    public void setmRightDrawable(Drawable mRightDrawable) {
        this.mRightDrawable = mRightDrawable;
        updateUi();
    }

    public void setmRightDrawableResId(int resId) {
        this.mRightDrawable = getContext().getResources().getDrawable(resId);
        updateUi();
    }
    public void setmLeftDrawableResId(int resId) {
        this.mLeftDrawable = getContext().getResources().getDrawable(resId);
        updateUi();
    }

    public void setmRightPadding(float mRightPadding) {
        this.mRightPadding = mRightPadding;
        updateUi();
    }

    public void setmRightSize(float mRightSize) {
        this.mRightSize = mRightSize;
        updateUi();
    }

    public void setmLeftPadding(float mLeftPadding) {
        this.mLeftPadding = mLeftPadding;
        updateUi();
    }

    public void setmLeftSize(float mLeftSize) {
        this.mLeftSize = mLeftSize;
        updateUi();
    }

    public void setmTitleSize(float mTitleSize) {
        this.mTitleSize = mTitleSize;
        updateUi();
    }

    public void setmLeftDrawable(Drawable mLeftDrawable) {
        this.mLeftDrawable = mLeftDrawable;
        updateUi();
    }

    public void setmSpiltLineWidth(float mSpiltLineWidth) {
        this.mSpiltLineWidth = mSpiltLineWidth;
        updateUi();
    }

    public void setmSpiltLineColor(int mSpiltLineColor) {
        this.mSpiltLineColor = mSpiltLineColor;
        updateUi();
    }

    public void setmLeftColorFilter(int mLeftColorFilter) {
        this.mLeftColorFilter = mLeftColorFilter;
        updateUi();
    }

    public void setmRightColorFilter(int mRightColorFilter) {
        this.mRightColorFilter = mRightColorFilter;
        updateUi();
    }

    @IntDef({LEFT, MID, RIGHT})
    public @interface ClickType {
    }

    private OnTitleClickListener mOnTitleClickListener;


    public void setOnTitleClickListener(OnTitleClickListener mOnTitleClickListener) {
        this.mOnTitleClickListener = mOnTitleClickListener;
    }

    public interface OnTitleClickListener {
        boolean onTitleClick(@ClickType int type);
    }

}
