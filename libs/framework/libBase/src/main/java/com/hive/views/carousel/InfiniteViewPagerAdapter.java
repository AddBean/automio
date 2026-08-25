// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.views.carousel;

import androidx.annotation.NonNull;
import androidx.viewpager.widget.PagerAdapter;
import android.view.View;
import android.view.ViewGroup;

import com.hive.adapter.core.CardItemData;
import com.hive.adapter.core.ICardItemView;
import com.hive.base.IBaseEventInterface;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class InfiniteViewPagerAdapter extends PagerAdapter {
    private IBaseEventInterface mEventInterface;
    private InfiniteCarouseView mCarouseView;
    public List<CardItemData> mData = new ArrayList<>();
    private int mMaxCountTimes = 1;
    private LinkedHashMap<Integer, View> mCacheViews = new LinkedHashMap<>();
    private float mPageScale=1;

    public InfiniteViewPagerAdapter(InfiniteCarouseView onViewPagerListener, IBaseEventInterface eventInterface) {
        this.mCarouseView = onViewPagerListener;
        this.mEventInterface=eventInterface;

    }

    public int getMaxCountTimes() {
        return mMaxCountTimes;
    }

    public int getDataCount() {
        return mData.size();
    }

    @Override
    public int getCount() {
        return mData.size() * mMaxCountTimes;
    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
        return view == object;
    }

    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup container, int position) {
        int index = position % mData.size();
        ICardItemView itemView = mCarouseView.getView(mData.get(index));
        putCacheViews(position, itemView);
        container.addView(itemView.getView());
        itemView.setBaseEventImpl(mEventInterface);
        itemView.bindData(mData.get(index));
        doScale(itemView.getView(),mPageScale);
//        DLog.e("instantiateItem position=" + position + " " + itemView.getView());
        return itemView.getView();
    }

    private void doScale(View view, float scale) {
        if (view == null) return;
        view.setScaleX(scale);
        view.setScaleY(scale);
    }


    public void setData(List<CardItemData> mData) {
        this.mData = mData;
    }

    @Override
    public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        container.removeView((View) object);
    }

    public Map<Integer, View> getCacheViews() {
        return mCacheViews;
    }

    /**
     * 限制缓存shu
     *
     * @param position
     * @param itemView
     */
    private void putCacheViews(int position, ICardItemView itemView) {

        for (Iterator<Map.Entry<Integer, View>> it = mCacheViews.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<Integer, View> item = it.next();
            if (mCacheViews.entrySet().size() > mData.size()
                    && item.getKey() != mCarouseView.getCurrPosition()
                    && item.getKey() != mCarouseView.getCurrPosition() - 1
                    && item.getKey() != mCarouseView.getCurrPosition() + 1
                    && item.getKey() != mCarouseView.getCurrPosition() - 2
                    && item.getKey() != mCarouseView.getCurrPosition() + 2
                    && item.getKey() != mCarouseView.getCurrPosition() - 3
                    && item.getKey() != mCarouseView.getCurrPosition() + 3) {
                it.remove();
            }
        }
        mCacheViews.put(position, itemView.getView());
//        DLog.e("mCacheViews size=" + mCacheViews.keySet().size());

    }

    public View getCacheView(Integer position) {
        return mCacheViews.get(position);
    }

    public void setMaxTimes(int maxTimes) {
        this.mMaxCountTimes = maxTimes;
    }

    public void setScale(float pageScale) {
        mPageScale=pageScale;
    }

    public interface OnViewPagerListener {
        ICardItemView getView(CardItemData itemData);
    }
}