// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.base;

import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.view.View;

import com.hive.adapter.RecyclerListAdapter;
import com.hive.adapter.core.CardItemData;
import com.hive.adapter.core.ICardItemFactory;

import java.util.List;
import java.util.Map;

public interface IBaseListInterface extends RecyclerListAdapter.OnLoadListener, SwipeRefreshLayout.OnRefreshListener,IBaseEventInterface {

    void doInitialize();

    Map<String, String> getRequestParams();

    List<CardItemData> parseData(String data);

    void onRequestFailed(int pageIndex,Throwable e);

    ICardItemFactory getCardFactory();

    RecyclerView.LayoutManager getLayoutManager();

    BaseListHelper.RequestType getRequestType();

    Map<String, String> getHeaderParams();

    String[] getPageParamsNames();

    View getHeaderView();

    View getFooterView();

    boolean isStartRequest();

    boolean isLoadMoreEnable();

    boolean isRefreshEnable();

    int getPageSize();

    String getRequestUrl();

    int getLoadMoreLastCount();

    void onLoadFinished();

    void onLoadFinishedBefore();
}
