// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.adapter;

import androidx.annotation.NonNull;
import androidx.viewpager.widget.PagerAdapter;
import android.view.View;
import android.view.ViewGroup;


import com.hive.adapter.core.CardItemData;
import com.hive.adapter.core.ICardAdapter;
import com.hive.adapter.core.ICardItemFactory;
import com.hive.adapter.core.ICardItemView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ViewPagerAdapter extends PagerAdapter implements ICardAdapter {
    protected List<CardItemData> mData = new ArrayList<>();
    private Map<Integer, View> mCacheViews = new HashMap<>();
    private ICardItemFactory mFactory;


    @Override
    public int getCount() {
        return mData.size();
    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
        return view == object;
    }

    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup container, int position) {
        ICardItemView itemView = mFactory.createItemView(container.getContext(), mData.get(position).cardType);
        container.addView(itemView.getView());
        itemView.bindData(mData.get(position));
        mCacheViews.put(position, itemView.getView());
        return itemView.getView();
    }

    @Override
    public void setFactory(ICardItemFactory factory) {
        this.mFactory = factory;
        notifyDataSetChanged();
    }

    @Override
    public void setData(List<CardItemData> mData) {
        this.mData = mData;
        notifyDataSetChanged();
    }

    @Override
    public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        container.removeView((View) object);

    }

    public Map<Integer, View> getCacheViews() {
        return mCacheViews;

    }

}
