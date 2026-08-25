// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.views.widgets;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import com.hive.views.R;


/**
 * Created by Administrator on 2015/12/23.
 */
public class CustomGridView extends ViewGroup {
    private int mWidth;
    private int mHeight;
    private int mCurrentX = 0;
    private int mCurrentY = 0;
    private int mRaw = 2;
    private Context mContext;
    private int mPadding = 2;
    private float mRate = 1.3f;

    public CustomGridView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mContext = context;
        initView(attrs);
    }

    public CustomGridView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mContext = context;
        initView(attrs);
    }

    public CustomGridView(Context context) {
        super(context);
        this.mContext = context;
        initView(null);
    }

    private void initView(AttributeSet attrs) {
        if (attrs == null) return;
        TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.CustomGridView);
        if (a.hasValue(R.styleable.CustomGridView_gridRaw)) {
            this.mRaw = a.getInteger(R.styleable.CustomGridView_gridRaw, 2);
        }
        if (a.hasValue(R.styleable.CustomGridView_gridPadding)) {
            this.mPadding = (int) a.getDimension(R.styleable.CustomGridView_gridPadding, 10f);
        }
        if (a.hasValue(R.styleable.CustomGridView_gridRate)) {
            this.mRate = a.getFloat(R.styleable.CustomGridView_gridRate, 1.3f);
        }
        a.recycle();
    }

    public void setGridRate(float mRate) {
        this.mRate = mRate;
    }

    public void setRawCount(int count) {
        this.mRaw = count;
    }

    public void removeAllChild() {
        this.removeAllViews();
    }

    public void addView(View view, int padding, int width, int height) {
        this.addView(view);
    }


    private int getChildWidth() {
        return (mWidth - (mRaw + 1) * mPadding) / this.mRaw;
    }

    private int getChildHeight() {
        return (int) (getChildWidth() * mRate);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        mCurrentX = mPadding;
        mCurrentY = mPadding;

        for (int i = 0; i < getChildCount(); i++) {
            View view = getChildAt(i);
            if (mCurrentX + getChildWidth() >= mWidth) {
                mCurrentX = mPadding;
                mCurrentY = mCurrentY + getChildHeight() + mPadding;
            }
            view.layout(mCurrentX, mCurrentY, mCurrentX + getChildWidth(), mCurrentY + getChildHeight());
            mCurrentX = mCurrentX + getChildWidth() + mPadding;
            final int position = i;
            if (mOnItemClickListener != null) {
                view.setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        if (mOnItemClickListener != null) {
                            mOnItemClickListener.onItemClick(v, position);
                        }
                    }
                });
            }
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        this.mWidth = MeasureSpec.getSize(widthMeasureSpec);
        this.mHeight = 0;
        int widthMeasureChild = MeasureSpec.makeMeasureSpec(getChildWidth(), MeasureSpec.EXACTLY);
        int heightMeasureChild = MeasureSpec.makeMeasureSpec(getChildHeight(), MeasureSpec.EXACTLY);
        for (int i = 0; i < getChildCount(); i++) {
            View childView = getChildAt(i);
            measureChild(childView, widthMeasureChild, heightMeasureChild);//此句较为关键，若无
            childView.measure(widthMeasureChild,heightMeasureChild);
            if (i % mRaw == 0) this.mHeight = this.mHeight + getChildHeight() + mPadding;
        }
        this.mHeight = this.mHeight + this.mPadding;
        setMeasuredDimension(widthMeasureSpec, this.mHeight);
    }


    private IOnItemClickListener mOnItemClickListener;

    public void setOnItemClick(IOnItemClickListener onItemClickListener) {
        this.mOnItemClickListener = onItemClickListener;
    }

    public interface IOnItemClickListener {
        public void onItemClick(View v, int position);
    }

}