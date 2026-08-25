// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.views.carousel;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Handler;
import android.os.Message;
import androidx.viewpager.widget.ViewPager;
import android.util.AttributeSet;
import android.view.View;

import com.hive.adapter.core.AbsCardItemView;
import com.hive.adapter.core.CardItemData;
import com.hive.adapter.core.ICardItemView;
import com.hive.base.BaseLayout;
import com.hive.base.IBaseEventInterface;
import com.hive.base.R;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class InfiniteCarouseView extends BaseLayout implements ViewPager.OnPageChangeListener, InfiniteViewPagerAdapter.OnViewPagerListener, IBaseEventInterface {
    private InfiniteViewPagerAdapter mPagerAdapter;
    public int MAX_TIME = 1;
    private int mCurrPosition;
    private WorkHandler mHandler;
    private int mInterval = -1;
    private List<Object> mDataSets;
    private int mPageMargin = 0;
    private int mPagePadding = 0;
    private float mPageScale = 1;
    public ViewHolder mViewHolder;
    private InfinitePagerListener mInfinitePagerListener;
    private final int MSG_BEAT=0x1;


    static class ViewHolder {
        public SpeedViewPager mViewPager;

        ViewHolder(View view) {
            mViewPager = view.findViewById(R.id.view_pager);
        }
    }

    public InfiniteCarouseView(Context context) {
        super(context);
    }

    public InfiniteCarouseView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public InfiniteCarouseView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void initAttrs(AttributeSet attrs) {
        super.initAttrs(attrs);
        TypedArray typedArray = getContext().obtainStyledAttributes(attrs, com.hive.views.R.styleable.InfiniteCarouseView);
        mPageMargin = (int) typedArray.getDimension(com.hive.views.R.styleable.InfiniteCarouseView_pageMargin, 0);
        mPagePadding = (int) typedArray.getDimension(com.hive.views.R.styleable.InfiniteCarouseView_pagePadding, 0);
        mPageScale = typedArray.getFloat(com.hive.views.R.styleable.InfiniteCarouseView_pageScale, 1f);
        typedArray.recycle();
    }

    @Override
    protected void initView(View view) {
        MAX_TIME = 1;
        mViewHolder = new ViewHolder(view);
        mPagerAdapter = new InfiniteViewPagerAdapter(this, this);
        mInfinitePagerListener = new InfinitePagerListener(mPagerAdapter, this);
        mInfinitePagerListener.setScale(mPageScale);
        mPagerAdapter.setScale(mPageScale);
        mPagerAdapter.setMaxTimes(MAX_TIME);
        mViewHolder.mViewPager.setOffscreenPageLimit(3);
        mViewHolder.mViewPager.setAdapter(mPagerAdapter);
        mViewHolder.mViewPager.setPadding(mPagePadding, 0, mPagePadding, 0);
        mViewHolder.mViewPager.setPageMargin(mPageMargin);
        enableScale(false);
        mHandler = new WorkHandler(this);
    }

    public void setPageMargin(int mPageMargin) {
        this.mPageMargin = mPageMargin;
        mViewHolder.mViewPager.setPageMargin(mPageMargin);
    }

    public void setPagePadding(int mPagePadding) {
        this.mPagePadding = mPagePadding;
        mViewHolder.mViewPager.setPadding(mPagePadding, 0, mPagePadding, 0);
    }

    public void setPageScale(float mPageScale) {
        this.mPageScale = mPageScale;
        mInfinitePagerListener.setScale(mPageScale);
        mPagerAdapter.setScale(mPageScale);
    }

    public void setCarouseAdapter(InfiniteCarouseAdapter mInfiniteCarouseAdapter) {
        this.mInfiniteCarouseAdapter = mInfiniteCarouseAdapter;
    }

    @Override
    public void onCardEvent(int cardEvent, Object args, AbsCardItemView itemView) {
        if (mInfiniteCarouseAdapter != null)
            mInfiniteCarouseAdapter.onItemEvent(cardEvent, args);
    }

    public void setDataSets(List<Object> objects, int interval) {
        mDataSets = objects;
        mInterval = interval;
        ArrayList<CardItemData> data = new ArrayList<>();
        for (int i = 0; i < objects.size(); i++) {
            CardItemData itemData = new CardItemData(0, objects.get(i));
            itemData.position = i;
            data.add(itemData);
        }
        mPagerAdapter.setData(data);
        mCurrPosition = data.size() * (MAX_TIME / 2);
        mPagerAdapter.notifyDataSetChanged();
        mViewHolder.mViewPager.setCurrentItem(mCurrPosition, false);
        startTimer(interval);
    }

    public void nextView(boolean smoothScroll) {
        mCurrPosition++;
        if (mCurrPosition >= mPagerAdapter.getCount()) {
            mCurrPosition = 0;
        }
        mViewHolder.mViewPager.setCurrentItem(mCurrPosition, smoothScroll);
    }

    public void prevView(boolean smoothScroll) {
        mCurrPosition--;
        if (mCurrPosition < 0) {
            mCurrPosition = MAX_TIME - 1;
            smoothScroll = false;
        }
        mViewHolder.mViewPager.setCurrentItem(mCurrPosition, smoothScroll);
    }

    public void enableScale(boolean enable) {
        mViewHolder.mViewPager.setOnPageChangeListener(enable ? mInfinitePagerListener : this);
    }

    public void enableInfinite(boolean enable) {
        MAX_TIME = enable ? 10000 : 1;
        mPagerAdapter.setMaxTimes(MAX_TIME);
    }

    private void handleMessage(Message msg) {
        if (mCurrPosition >= mPagerAdapter.getCount()) {
            mViewHolder.mViewPager.setCurrentItem(mCurrPosition, false);
        } else {
            mViewHolder.mViewPager.setCurrentItem(mCurrPosition, true);
        }
        startTimer(mInterval);
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        if (mInfiniteCarouseAdapter != null)
            mInfiniteCarouseAdapter.onItemPageScrolled(position, positionOffset, positionOffsetPixels);
    }

    @Override
    public void onPageSelected(int position) {
        mCurrPosition = position;
        int index = position % mPagerAdapter.mData.size();
        if (mInfiniteCarouseAdapter != null)
            mInfiniteCarouseAdapter.onItemSelected(mPagerAdapter.getCacheView(index), mPagerAdapter.mData.get(index));
    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }

    @Override
    public int getLayoutId() {
        return R.layout.infinite_auto_view_pager;
    }


    protected void startTimer(int interval) {
        if (interval <= 0) return;
        mInterval=interval;
        mCurrPosition++;
        mHandler.removeMessages(MSG_BEAT);
        mHandler.sendEmptyMessageDelayed(MSG_BEAT,interval);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        startTimer(mInterval);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
    }

    public static class WorkHandler extends Handler {
        private WeakReference<InfiniteCarouseView> ref;

        public WorkHandler(InfiniteCarouseView pager) {
            ref = new WeakReference<>(pager);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (ref == null || ref.get() == null) return;
            ref.get().handleMessage(msg);
        }
    }

    public SpeedViewPager getViewPager() {
        return mViewHolder.mViewPager;
    }

    @Override
    public ICardItemView getView(CardItemData itemData) {
        return mInfiniteCarouseAdapter.getCardView(itemData);
    }

    private InfiniteCarouseAdapter mInfiniteCarouseAdapter;

    public void setCurrPosition(int mCurrPosition) {
        this.mCurrPosition = mCurrPosition;
    }

    public int getCurrPosition() {
        return mCurrPosition;
    }


    public void setTouchScrollable(boolean scrollable) {
        mViewHolder.mViewPager.setTouchScrollable(scrollable);
    }

}
