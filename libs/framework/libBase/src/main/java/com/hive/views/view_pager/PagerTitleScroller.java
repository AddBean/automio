// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.views.view_pager;

import android.content.Context;
import android.graphics.Canvas;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.hive.views.fragment.PagerTitleView;

import java.util.List;
import com.hive.annotation.NotProguard;
@NotProguard
public class PagerTitleScroller<T extends PagerTitleView> extends LinearLayout implements View.OnClickListener {
    private Context mContext;
    private OnTabClickListener mOnTabClickListener;
    private int mPosition;
    private float mPositionOffset;
    private int mPositionOffsetPixels;

    public PagerTitleScroller(Context context) {
        super(context);
        this.mContext = context;
        initView();
    }

    private void initView() {
        ViewGroup.LayoutParams fl = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        this.setLayoutParams(fl);
        this.setOrientation(HORIZONTAL);
    }


    @Override
    public void onClick(View v) {
        T tabView = (T) v;
        if (mOnTabClickListener != null)
            mOnTabClickListener.onTabClick(tabView);

    }

    public void setOnTabClickListener(OnTabClickListener onTabClickListener) {
        this.mOnTabClickListener = onTabClickListener;
    }

    public void setTitleViews(List<T> mTitleViews) {
        removeAllViews();
        for (int i = 0; i < mTitleViews.size(); i++) {
            T tabView = mTitleViews.get(i);
            this.addView(tabView);
            tabView.setOnClickListener(this);
        }
        this.requestLayout();
    }

    public interface OnTabClickListener<T> {
        void onTabClick(T tab);
    }


    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        if (mOnIndexDrawListener != null) {
            mOnIndexDrawListener.onIndexDraw(this,canvas, mPosition, mPositionOffset, mPositionOffsetPixels);
        }
    }


    public void setIndexPosition(int position, float positionOffset, int positionOffsetPixels) {
        this.mPosition = position;
        this.mPositionOffset = positionOffset;
        this.mPositionOffsetPixels = positionOffsetPixels;
        invalidate();
    }

    private OnIndexDrawListener mOnIndexDrawListener;

    public void setOnIndexDrawListener(OnIndexDrawListener onIndexDrawListener) {
        this.mOnIndexDrawListener = onIndexDrawListener;
    }

    public interface OnIndexDrawListener {
        void onIndexDraw(PagerTitleScroller scroller, Canvas canvas, int position, float positionOffset, int positionOffsetPixels);
    }
}
