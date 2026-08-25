// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.base;

import android.app.Activity;
import android.os.Bundle;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.View;

import com.hive.adapter.core.AbsCardItemView;
import com.hive.views.StatefulLayout;

import java.util.HashMap;
import java.util.Map;

import static androidx.recyclerview.widget.LinearLayoutManager.VERTICAL;

public abstract class BaseListFragmentActivity extends BaseFragmentActivity implements IBaseListInterface {
    protected ViewHolder mViewHolder;
    protected BaseListHelper mListHelper;


    public static class ViewHolder {
        public RecyclerView mRecyclerView;
        public SwipeRefreshLayout mLayoutRefresh;
        public StatefulLayout mLayoutState;

        ViewHolder(Activity view) {
            mRecyclerView = view.findViewById(R.id.recycler_view);
            mLayoutRefresh = view.findViewById(R.id.layout_refresh);
            mLayoutState = view.findViewById(R.id.layout_state);
        }
    }

    @Override
    public int getLayoutId() {
        return R.layout.base_list_fragment;
    }

    @Override
    protected void doOnCreate(Bundle savedState) {
        mViewHolder = new ViewHolder(this);
        mListHelper = new BaseListHelper(this, mViewHolder.mRecyclerView, mViewHolder.mLayoutRefresh, mViewHolder.mLayoutState);
        mListHelper.initialize();
        doInitialize();
    }

    @Override
    public RecyclerView.LayoutManager getLayoutManager() {
        return new LinearLayoutManager(this, VERTICAL, false);
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
    protected void onDestroy() {
        if (mListHelper != null)
            mListHelper.onDestroy();
        super.onDestroy();
    }
}
