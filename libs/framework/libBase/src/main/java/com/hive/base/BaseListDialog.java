// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.base;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.hive.adapter.core.AbsCardItemView;
import com.hive.adapter.core.CardItemData;
import com.hive.utils.GlobalApp;
import com.hive.utils.system.UIUtils;
import com.hive.views.StatefulLayout;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static androidx.recyclerview.widget.RecyclerView.VERTICAL;


public abstract class BaseListDialog extends Dialog implements IBaseListInterface {

    protected ViewHolder mViewHolder;
    protected BaseListHelper mListHelper;
    protected int DP = UIUtils.dp2px(GlobalApp.getApp(), 1);

    public static class ViewHolder {
        public RecyclerView mRecyclerView;
        public SwipeRefreshLayout mLayoutRefresh;
        public StatefulLayout mLayoutState;

        ViewHolder(BaseListDialog view) {
            mRecyclerView = view.findViewById(R.id.recycler_view);
            mLayoutRefresh = view.findViewById(R.id.layout_refresh);
            mLayoutState = view.findViewById(R.id.layout_state);
        }
    }

    public BaseListDialog(@NonNull Context context) {
        super(context);
    }

    public BaseListDialog(@NonNull Context context, int themeResId) {
        super(context, themeResId);
    }

    protected BaseListDialog(@NonNull Context context, boolean cancelable, @Nullable OnCancelListener cancelListener) {
        super(context, cancelable, cancelListener);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(LayoutInflater.from(getContext()).inflate(getLayoutId(), null));
        doOnCreate();
    }


    protected void doOnCreate() {
        mViewHolder = new ViewHolder(this);
        mListHelper = new BaseListHelper(this, mViewHolder.mRecyclerView, mViewHolder.mLayoutRefresh, mViewHolder.mLayoutState);
        mListHelper.initialize();
        doInitialize();
    }

    public List<CardItemData> getData() {
        if (mListHelper == null) return null;
        return mListHelper.getData();
    }

    public void notifyDataSetChanged() {
        mListHelper.mRecyclerAdapter.notifyDataSetChanged();
    }


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
        return 12;
    }

    @Override
    public RecyclerView.LayoutManager getLayoutManager() {
        return new LinearLayoutManager(getContext(), VERTICAL, false);
    }

    @Override
    public Map<String, String> getRequestParams() {
        return new HashMap<>();
    }

    @Override
    public String[] getPageParamsNames() {
        return new String[]{"page", "pagesize"};
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
    public void onLoadFinished() {
    }

    @Override
    public void onLoadFinishedBefore() {
    }

    @Override
    public void dismiss() {
        super.dismiss();
        if (mListHelper != null)
            mListHelper.onDestroy();
    }
}
