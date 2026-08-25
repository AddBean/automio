// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.base;

import android.content.Context;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.recyclerview.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.View;

import com.hive.adapter.core.AbsCardItemView;
import com.hive.views.StatefulLayout;

import java.util.HashMap;
import java.util.Map;

public abstract class BaseListLayout extends BaseLayout implements IBaseListInterface {
    protected ViewHolder mViewHolder;
    public BaseListHelper mListHelper;
    private OnCardEventListener mOnCardEventListener;

    public static class ViewHolder {
        public RecyclerView mRecyclerView;
        public SwipeRefreshLayout mLayoutRefresh;
        public StatefulLayout mLayoutState;

        ViewHolder(View view) {
            mRecyclerView = view.findViewById(R.id.recycler_view);
            mLayoutRefresh = view.findViewById(R.id.layout_refresh);
            mLayoutState = view.findViewById(R.id.layout_state);
        }
    }

    public BaseListLayout(Context context) {
        super(context);
    }

    public BaseListLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public BaseListLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }


    @Override
    protected void initView(View view) {
        mViewHolder = new ViewHolder(this);
        mListHelper = new BaseListHelper(this, mViewHolder.mRecyclerView, mViewHolder.mLayoutRefresh, mViewHolder.mLayoutState);
        mListHelper.initialize();
        doInitialize();
    }

    @Override
    public int getLayoutId() {
        return R.layout.base_list_fragment;
    }


    @Override
    public boolean isLoadMoreEnable() {
        return true;
    }

    @Override
    public boolean isRefreshEnable() {
        return true;
    }

    @Override
    public int getPageSize() {
        return 24;
    }

    @Override
    public String[] getPageParamsNames() {
        return new String[]{"page", "pagesize"};
    }

    @Override
    public Map<String, String> getRequestParams() {
        return new HashMap<>();
    }

    @Override
    public void onLoadMore() {

    }

    @Override
    public void onRefresh() {

    }

    @Override
    public void onCardEvent(int cardEvent, Object args, AbsCardItemView itemView) {
        if (mOnCardEventListener != null)
            mOnCardEventListener.onCardEvent(cardEvent, args);
    }

    @Override
    public BaseListHelper.RequestType getRequestType() {
        return BaseListHelper.RequestType.REQUEST_NET;
    }

    @Override
    public View getHeaderView() {
        return null;
    }

    @Override
    public View getFooterView() {
        return null;
    }


    @Override
    public boolean isStartRequest() {
        return true;
    }

    @Override
    public Map<String, String> getHeaderParams() {
        return new HashMap<>();
    }

    public void setOnCardEventListener(OnCardEventListener mOnCardEventListener) {
        this.mOnCardEventListener = mOnCardEventListener;
    }

    public interface OnCardEventListener {
        void onCardEvent(int cardEvent, Object args);
    }

    @Override
    public int getLoadMoreLastCount() {
        return -1;
    }

    @Override
    public void onRequestFailed(int pageIndex, Throwable e) {

    }

    @Override
    public void onLoadFinishedBefore() {
    }

    @Override
    public void onLoadFinished() {
    }

    @Override
    protected void onDetachedFromWindow() {
        if (mListHelper != null)
            mListHelper.onDestroy();
        super.onDetachedFromWindow();

    }
}
