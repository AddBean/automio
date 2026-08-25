// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.views.view_pager;

import android.content.Context;
import android.graphics.Canvas;

import androidx.viewpager.widget.ViewPager;

import android.util.AttributeSet;
import android.view.View;
import android.widget.HorizontalScrollView;

import com.hive.base.BaseLayout;
import com.hive.base.R;
import com.hive.utils.utils.DensityUtil;
import com.hive.utils.utils.TypeUtils;
import com.hive.views.fragment.PagerTag;
import com.hive.views.fragment.PagerTitleView;

import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.List;

import com.hive.annotation.NotProguard;

@NotProguard
public abstract class PagerHostLayout<T extends PagerTitleView> extends BaseLayout implements PagerTitleScroller.OnTabClickListener<T>, ViewPager.OnPageChangeListener, PagerTitleScroller.OnIndexDrawListener {
    public PagerHostLayout(Context context) {
        super(context);
    }

    public PagerHostLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public PagerHostLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public static class ViewHolder {
        public HorizontalScrollView mTitleView;
        public ViewPager mViewPager;

        ViewHolder(View view) {
            mTitleView = view.findViewById(R.id.title_view);
            mViewPager = view.findViewById(R.id.view_pager);
        }
    }

    protected PagerLayoutAdapter mAdapter;
    public List<T> mTitleViews;
    protected PagerTitleScroller<T> mPagerTitleScroller;
    protected int mPosition = 0;
    public int DP = 1;
    protected ViewHolder mViewHolder;
    public List<IPagerLayout> mTabViews;

    @Override
    protected void initView(View view) {
        mTitleViews = new ArrayList<>();
        mTabViews = new ArrayList<>();
        DP = DensityUtil.dip2px(1);
        mViewHolder = new ViewHolder(this);
        mPagerTitleScroller = new PagerTitleScroller(getContext());
        mAdapter = new PagerLayoutAdapter();
        mViewHolder.mViewPager.setOnPageChangeListener(this);
        mPagerTitleScroller.setOnTabClickListener(this);
        mPagerTitleScroller.setOnIndexDrawListener(this);
        mViewHolder.mViewPager.setAdapter(mAdapter);
        mViewHolder.mTitleView.addView(mPagerTitleScroller);
        initLayout();
    }

    @Override
    public void onTabClick(T tab) {
        for (int i = 0; i < mTabViews.size(); i++) {
            if (tab.getPagerTag().name.equals(mTabViews.get(i).getLayoutTag().name)) {
                mViewHolder.mViewPager.setCurrentItem(i);
            }
        }
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        mPagerTitleScroller.setIndexPosition(position, positionOffset, positionOffsetPixels);
        int curIndex = position;
        int nextIndex = position + 1;
        mTitleViews.get(curIndex).onScrolling(1 - positionOffset);
        if (nextIndex < mTitleViews.size())
            mTitleViews.get(nextIndex).onScrolling(positionOffset);
        ensurePageScaleRight(positionOffset);
        mViewHolder.mTitleView.scrollTo((int) ((curIndex + positionOffset) * mTitleViews.get(0).getView().getWidth()), 0);
    }

    private void ensurePageScaleRight(float positionOffset) {
        if (positionOffset == 0) {
            mTitleViews.get(mPosition).onScrolling(1);
            for (int i = 0; i < mTitleViews.size(); i++) {
                if (mPosition != i) {
                    mTitleViews.get(i).onScrolling(0);
                }
            }
        }
    }


    @Override
    public void onPageSelected(int position) {
        mPosition = position;
        final T tab = mTitleViews.get(position);
        for (int i = 0; i < mTabViews.size(); i++) {
            mTitleViews.get(i).setSelected(false);
            mTitleViews.get(i).onPageSelected(false, mTabViews.get(position).getLayoutTag());
        }
        tab.setSelected(true);
        tab.onPageSelected(true, mTabViews.get(position).getLayoutTag());

    }

    public int getScrollerOffset() {
        return -50 * DP;
    }

    public IPagerLayout getCurrentFragment() {
        if (mPosition < mTabViews.size()) {
            return mTabViews.get(mPosition);
        } else {
            mPosition = 0;
            return mTabViews.get(0);
        }
    }

    public void selectFragment(PagerTag tag, boolean smoothScroll) {
        if (tag == null) return;
        for (int i = 0; i < mTabViews.size(); i++) {
            if (tag.name.equals(mTabViews.get(i).getLayoutTag().name)) {
                mViewHolder.mViewPager.setCurrentItem(i, smoothScroll);
            }
        }
    }

    public void selectFragment(PagerTag tag) {
        selectFragment(tag, true);
    }

    public void notifyDataSetChanged(List<IPagerLayout> tabViews) {
        mTabViews = tabViews;
        if (mTabViews == null || mTabViews.isEmpty()) return;
        mTitleViews.clear();
        for (IPagerLayout fragment : mTabViews) {
            mTitleViews.add(getTitleView(fragment.getLayoutTag(), fragment));
        }
        mPagerTitleScroller.setTitleViews(mTitleViews);
        mAdapter.setData(mTabViews);
        mAdapter.notifyDataSetChanged();
        onPageSelected(mPosition);
    }


    protected abstract void initLayout();

    protected T getTitleView(PagerTag tag, Object pager) {
        Class<T> clazz = (Class<T>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
        T t = TypeUtils.createContextInstance(clazz, getContext());
        t.setPagerTag(tag);
        t.setPager(pager);
        return t;
    }

    @Override
    public void onIndexDraw(PagerTitleScroller scroller, Canvas canvas, int position, float positionOffset, int positionOffsetPixels) {
    }

    @Override
    public int getLayoutId() {
        return R.layout.pager_host_layout;
    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }

    public ViewPager getViewPager() {
        return mViewHolder.mViewPager;
    }
}