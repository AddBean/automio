// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.views.fragment;

import android.graphics.Canvas;

import androidx.viewpager.widget.ViewPager;

import android.view.View;
import android.widget.HorizontalScrollView;

import com.hive.base.BaseFragment;
import com.hive.base.R;
import com.hive.utils.debug.DLog;
import com.hive.utils.utils.DensityUtil;
import com.hive.utils.utils.TypeUtils;

import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.List;

public abstract class PagerHostFragment<T extends PagerTitleView> extends BaseFragment implements PagerTitleScroller.OnTabClickListener<T>, ViewPager.OnPageChangeListener, PagerTitleScroller.OnIndexDrawListener {
    private int mState;

    public class ViewHolder {
        public HorizontalScrollView mTitleView;
        public ViewPager mViewPager;

        ViewHolder(View view) {
            mTitleView = view.findViewById(R.id.title_view);
            mViewPager = view.findViewById(R.id.view_pager);
        }
    }

    protected PagerFragmentAdapter mAdapter;

    protected List<T> mTitleViews = new ArrayList<>();
    protected PagerTitleScroller<T> mPagerTitleScroller;
    protected int mPosition;
    public int DP = 1;
    protected ViewHolder mViewHolder;
    protected List<IPagerFragment> mTabFragments = new ArrayList<>();

    @Override
    public void initView() {
        DP = DensityUtil.dip2px(1);
        mViewHolder = new ViewHolder(getCurrentView());
        mPagerTitleScroller = new PagerTitleScroller(getActivity());
        mAdapter = new PagerFragmentAdapter(getChildFragmentManager());
        mViewHolder.mViewPager.setOnPageChangeListener(this);
        mPagerTitleScroller.setOnTabClickListener(this);
        mPagerTitleScroller.setOnIndexDrawListener(this);
        mViewHolder.mViewPager.setAdapter(mAdapter);
        mViewHolder.mTitleView.addView(mPagerTitleScroller);
        mViewHolder.mTitleView.setClipChildren(false);
        mViewHolder.mTitleView.setClipToPadding(false);
        initFragment();
    }

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
        ensurePageScaleRight(positionOffset);
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
        if (mPosition < mTabFragments.size()) {
            return mTabFragments.get(mPosition);
        } else {
            mPosition = 0;
            return mTabFragments.get(0);
        }
    }

    public void selectFragment(PagerTag tag) {
        if (tag == null) return;
        for (int i = 0; i < mTabFragments.size(); i++) {
            if (tag.name.equals(mTabFragments.get(i).getFragmentTag().name)) {
                mViewHolder.mViewPager.setCurrentItem(i, false);
            }
        }
    }


    public void selectFragment(int pos) {
        if (mViewHolder != null)
            mViewHolder.mViewPager.setCurrentItem(pos, true);
    }

    public List<IPagerFragment> getTabFragments() {
        return mTabFragments;
    }

    public void notifyDataSetChanged(List<IPagerFragment> tabFragments) {
        mTabFragments = tabFragments;
        if (mTabFragments == null || mTabFragments.isEmpty()) return;
        mTitleViews.clear();
        for (IPagerFragment fragment : mTabFragments) {
            mTitleViews.add(getTitleView(fragment.getFragmentTag(), fragment));
        }
        mPagerTitleScroller.setTitleViews(mTitleViews);
        mAdapter.setFragments(mTabFragments);
        mAdapter.notifyDataSetChanged();
        onPageSelected(mPosition);
    }


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
        return R.layout.pager_host_fragment;
    }


}