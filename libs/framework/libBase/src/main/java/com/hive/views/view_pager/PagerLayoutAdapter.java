// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.views.view_pager;

import androidx.annotation.NonNull;
import androidx.viewpager.widget.PagerAdapter;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

public class PagerLayoutAdapter extends PagerAdapter {
    private List<IPagerLayout> mData;
    private View mCurrentPrimaryItem;

    public PagerLayoutAdapter() {
        mData = new ArrayList<>();
    }

    public void setData(List<IPagerLayout> mData) {
        this.mData = mData;
    }

    @Override
    public int getCount() {
        return mData.size();
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        View pager = (View) mData.get(position);
        container.addView(pager);//添加页卡
        if (pager != mCurrentPrimaryItem && pager instanceof IPagerLayout) {
            ((IPagerLayout) pager).setUserVisibleHint(false);
        }
        return mData.get(position);
    }

    @Override
    public void setPrimaryItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        super.setPrimaryItem(container, position, object);
        if (mCurrentPrimaryItem != object && object instanceof IPagerLayout) {
            ((IPagerLayout) object).setUserVisibleHint(true);
        }
        mCurrentPrimaryItem = (View) object;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        container.removeView((View) object);
    }

    @Override
    public int getItemPosition(Object object) {
        return POSITION_NONE;
    }
}