// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.base;

import android.graphics.Canvas;
import android.os.Bundle;
import androidx.viewpager.widget.ViewPager;
import android.widget.HorizontalScrollView;

import com.hive.utils.debug.DLog;
import com.hive.utils.utils.DensityUtil;
import com.hive.utils.utils.TypeUtils;
import com.hive.views.fragment.IPagerFragment;
import com.hive.views.fragment.PagerFragmentAdapter;
import com.hive.views.fragment.PagerTag;
import com.hive.views.fragment.PagerTitleScroller;
import com.hive.views.fragment.PagerTitleView;

import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.List;

public abstract class BasePagerHostActivity<T extends PagerTitleView> extends BaseFragmentActivity implements PagerTitleScroller.OnTabClickListener<T>, ViewPager.OnPageChangeListener, PagerTitleScroller.OnIndexDrawListener {
    private int mState;

    static class ViewHolder {
        HorizontalScrollView mTitleView;
        ViewPager mViewPager;

        ViewHolder(BaseFragmentActivity view) {
            mTitleView = view.findViewById(R.id.title_view);
            mViewPager = view.findViewById(R.id.view_pager);
        }
    }

    private PagerFragmentAdapter mAdapter;

    protected List<T> mTitleViews = new ArrayList<>();
    protected PagerTitleScroller<T> mPagerTitleScroller;
    private int mPosition;
    public int DP = 1;
    private ViewHolder mViewHolder;
    protected List<IPagerFragment> mTabFragments = new ArrayList<>();


    protected abstract void initFragment();

    @Override
    public void onTabClick(T tab) {
        for (int i = 0; i < mTabFragments.size(); i++) {
            if (tab.getPagerTag().name.equals(mTabFragments.get(i).getFragmentTag().name)) {
                boolean overPager = Math.abs(i - mViewHolder.mViewPager.getCurrentItem()) > 1;
                if (!isSmoothScroll()) {
                    mViewHolder.mViewPager.setCurrentItem(i, !overPager);
                } else {
                    mViewHolder.mViewPager.setCurrentItem(i, true);
                }
            }
        }
    }

    @Override
    protected void doOnCreate(Bundle savedState) {
        DP = DensityUtil.dip2px(1);
        mViewHolder = new ViewHolder(this);
        mPagerTitleScroller = new PagerTitleScroller(this);
        mAdapter = new PagerFragmentAdapter(getSupportFragmentManager());
        mViewHolder.mViewPager.setOnPageChangeListener(this);
        mPagerTitleScroller.setOnTabClickListener(this);
        mPagerTitleScroller.setOnIndexDrawListener(this);
        mViewHolder.mViewPager.setAdapter(mAdapter);
        mViewHolder.mTitleView.addView(mPagerTitleScroller);
        mViewHolder.mTitleView.setClipChildren(false);
        mViewHolder.mTitleView.setClipToPadding(false);
        initFragment();
    }

    protected boolean isSmoothScroll() {
        return true;
    }

    @Override
    public void onPageScrollStateChanged(int state) {
        DLog.e("onPageScrolled state=" + state);
        mState = state;
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        mPagerTitleScroller.setIndexPosition(position, positionOffset, positionOffsetPixels);
        int curIndex = position;
        int nextIndex = position + 1;
        mTitleViews.get(curIndex).onScrolling(1 - positionOffset);
        if (nextIndex < mTitleViews.size())
            mTitleViews.get(nextIndex).onScrolling(positionOffset);
    }

    @Override
    public void onPageSelected(int position) {
        mPosition = position;
        T tab = mTitleViews.get(position);
        for (int i = 0; i < mTabFragments.size(); i++) {
            mTitleViews.get(i).setSelected(false);
            mTitleViews.get(i).onPageSelected(false, mTabFragments.get(position).getFragmentTag());
        }
        tab.setSelected(true);
        tab.onPageSelected(true, mTabFragments.get(position).getFragmentTag());
        for (int i = 0; i < mTabFragments.size(); i++) {
            if (tab.getPagerTag().name.equals(mTabFragments.get(i).getFragmentTag().name)) {
                mViewHolder.mTitleView.smoothScrollTo((int) tab.getX() + getScrollerOffset(), 0);
            }
        }
    }

    public int getScrollerOffset() {
        return -50 * DP;
    }

    public IPagerFragment getCurrentFragment() {
        if (mPosition >= mTabFragments.size() - 1) {
            mPosition = 0;
            return mTabFragments.get(0);
        }
        return mTabFragments.get(mPosition);
    }

    public void selectFragment(PagerTag tag) {
        if (tag == null) return;
        for (int i = 0; i < mTabFragments.size(); i++) {
            if (tag.name.equals(mTabFragments.get(i).getFragmentTag().name)) {
                mViewHolder.mViewPager.setCurrentItem(i);
            }
        }
    }

    public List<IPagerFragment> getTabFragments() {
        return mTabFragments;
    }

    public void notifyDataSetChanged(List<IPagerFragment> tabFragments) {
        mTabFragments = tabFragments;
        if (mTabFragments == null || mTabFragments.isEmpty()) return;
        mTitleViews.clear();
        for (IPagerFragment fragment : mTabFragments) {
            mTitleViews.add(getTitleView(fragment.getFragmentTag()));
        }
        mPagerTitleScroller.setTitleViews(mTitleViews);
        mAdapter.setFragments(mTabFragments);
        mAdapter.notifyDataSetChanged();
        onPageSelected(mPosition);
    }


    protected T getTitleView(PagerTag tag) {
        Class<T> clazz = (Class<T>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
        T t = TypeUtils.createContextInstance(clazz, this);
        t.setPagerTag(tag);
        return t;
    }

    @Override
    public void onIndexDraw(PagerTitleScroller scroller, Canvas canvas, int position, float positionOffset, int positionOffsetPixels) {
    }


    @Override
    protected int getLayoutId() {
        return R.layout.base_pager_host_activity;
    }
}
