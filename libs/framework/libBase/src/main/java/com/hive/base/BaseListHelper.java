// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.base;

import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.text.TextUtils;
import android.view.View;

import com.hive.adapter.RecyclerListAdapter;
import com.hive.adapter.core.AbsCardItemView;
import com.hive.adapter.core.CardItemData;
import com.hive.adapter.core.ICardItemFactory;
import com.hive.net.BaseApiService;
import com.hive.net.RxTransformer;
import com.hive.utils.OnHttpStateListener;
import com.hive.utils.debug.DLog;
import com.hive.views.StatefulLayout;
import com.hive.views.widgets.AbsStatefulLayout;

import org.reactivestreams.Subscription;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BaseListHelper implements IBaseListInterface {
    public RecyclerListAdapter mRecyclerAdapter;
    public RecyclerView.LayoutManager mLayoutManager;
    //    protected List<CardItemData> mData = new ArrayList<>();
    public RecyclerView mRecyclerView;
    public IBaseListInterface mBaseListImpl;
    public SwipeRefreshLayout mLayoutRefresh;
    public StatefulLayout mLayoutState;
    private int mPageIndex = 1;
    private Subscription mSubscribe;
    public boolean isRefresh = true;

    public enum RequestType {REQUEST_NET, REQUEST_LOCAL}


    public BaseListHelper(IBaseListInterface mBaseListImpl, RecyclerView mRecyclerView, SwipeRefreshLayout mLayoutRefresh, StatefulLayout mLayoutState) {
        this.mRecyclerView = mRecyclerView;
        this.mBaseListImpl = mBaseListImpl;
        this.mLayoutRefresh = mLayoutRefresh;
        this.mLayoutState = mLayoutState;
    }

    public void initialize() {
        mRecyclerAdapter = new RecyclerListAdapter(mRecyclerView.getContext());
        mLayoutManager = getLayoutManager();
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setAdapter(mRecyclerAdapter);
        mRecyclerAdapter.setFactory(getCardFactory());
        mRecyclerAdapter.setHeader(mLayoutManager, getHeaderView());
        mRecyclerAdapter.setBaseListInterface(mBaseListImpl);
        mRecyclerView.addOnScrollListener(mOnScrollListener);
        if (mLayoutRefresh != null)
            mLayoutRefresh.setEnabled(isRefreshEnable());
        if (isLoadMoreEnable()) {
            mRecyclerAdapter.setLoadMore(mLayoutManager, mRecyclerView, getFooterView());
            mRecyclerAdapter.setOnLoadListener(this);
        }
        if (mLayoutRefresh != null)
            mLayoutRefresh.setOnRefreshListener(this);
        mLayoutState.showProgress();
        if (isStartRequest()) {
            RequestType type = getRequestType();
            if (type == RequestType.REQUEST_NET)
                requestData(mPageIndex, true);
        }
    }

    public void requestData(final int pageIndex, final boolean isRefresh) {
        this.isRefresh = isRefresh;
        String url = getRequestUrl();
        if (TextUtils.isEmpty(url)) {
            DLog.e("request url=null!!");
            return;
        }
        DLog.e("pageIndex = " + pageIndex);
        mPageIndex = pageIndex;
        cancelRequest();
        BaseApiService.data().getList(url, getHeaderParams(), getRequestParams(String.valueOf(pageIndex), String.valueOf(getPageSize())))
                .compose(RxTransformer.<String>stringTrans())
                .subscribe(new OnHttpStateListener<String>(mBaseListImpl) {
                    @Override
                    public void onSubscribe(Subscription s) {
                        super.onSubscribe(s);
                        mSubscribe = s;
                    }

                    @Override
                    public void onSuccess(String data) throws Throwable {
                        onRequestSuccess(data, isRefresh);
                        if (isRefresh)
                            mRecyclerView.scrollToPosition(0);
                    }

                    @Override
                    public boolean onFailure(Throwable e) {
                        super.onFailure(e);
                        onRequestFailed(mPageIndex, e);
                        return true;
                    }
                });
    }

    @Override
    public void doInitialize() {
    }

    @Override
    public void onCardEvent(int cardEvent, Object args, AbsCardItemView itemView) {

    }

    public void notifyData(boolean isRefresh) {
        onRequestSuccess(null, isRefresh);
    }

    public void onRequestSuccess(String data, boolean isRefresh) {
        this.isRefresh = isRefresh;
        List<CardItemData> dataTmp = parseData(data);
        if (mLayoutRefresh != null)
            mLayoutRefresh.setRefreshing(false);
        if (isRefresh) {
            if (isLoadMoreEnable()) {
                if (dataTmp == null || dataTmp.size() == 0) {
                    mRecyclerAdapter.setLoadState(RecyclerListAdapter.ELoadState.GONE);
                } else if (dataTmp.size() < getPageSize()) {
                    mRecyclerAdapter.setLoadState(RecyclerListAdapter.ELoadState.EMPTY);
                } else {
                    mRecyclerAdapter.setLoadState(RecyclerListAdapter.ELoadState.READY);
                }
            }
            mPageIndex++;
        } else {
            if (dataTmp == null || dataTmp.size() < getPageSize()) {
                if (isLoadMoreEnable())
                    mRecyclerAdapter.setLoadState(RecyclerListAdapter.ELoadState.EMPTY);
            } else {
                if (isLoadMoreEnable())
                    mRecyclerAdapter.setLoadState(RecyclerListAdapter.ELoadState.READY);
                mPageIndex++;
            }

        }

        onLoadFinishedBefore();
        if (dataTmp != null) {
            if (isRefresh) {
                mRecyclerAdapter.setData(dataTmp);
            } else {
                mRecyclerAdapter.addData(dataTmp);
            }
        }
        if (mRecyclerAdapter.getDataSets() == null || mRecyclerAdapter.getDataSets().size() == 0) {
            mLayoutState.showEmpty();
        } else {
            mLayoutState.showContent();
        }
        onLoadFinished();
    }


    @Override
    public boolean isStartRequest() {
        return mBaseListImpl.isStartRequest();
    }

    @Override
    public ICardItemFactory getCardFactory() {
        return mBaseListImpl.getCardFactory();
    }

    @Override
    public RequestType getRequestType() {
        return mBaseListImpl.getRequestType();
    }

    @Override
    public String getRequestUrl() {
        return mBaseListImpl.getRequestUrl();
    }

    @Override
    public List<CardItemData> parseData(String data) {
        return mBaseListImpl.parseData(data);
    }

    @Override
    public void onRequestFailed(int pageIndex, Throwable e) {
        if (mPageIndex != 1 && mPageIndex != 0) {
            mRecyclerAdapter.setLoadState(RecyclerListAdapter.ELoadState.FAILED);
            return;
        }
        if (mLayoutRefresh != null)
            mLayoutRefresh.setRefreshing(false);
        mBaseListImpl.onRequestFailed(pageIndex, e);
    }

    @Override
    public boolean isLoadMoreEnable() {
        return mBaseListImpl.isLoadMoreEnable();
    }

    @Override
    public boolean isRefreshEnable() {
        return mBaseListImpl.isRefreshEnable();
    }

    @Override
    public int getPageSize() {
        return mBaseListImpl.getPageSize();
    }


    public Map<String, String> getRequestParams(String pageIndex, String pageSize) {
        Map<String, String> map = getRequestParams();
        if (map == null)
            map = new HashMap<>();
        String[] pageParams = getPageParamsNames();
        map.put(pageParams[0], pageIndex);
        map.put(pageParams[1], pageSize);
        return map;
    }

    @Override
    public Map<String, String> getHeaderParams() {
        Map<String, String> map = mBaseListImpl.getHeaderParams();
        return map;
    }

    @Override
    public Map<String, String> getRequestParams() {
        Map<String, String> map = mBaseListImpl.getRequestParams();
        if (map == null) map = new HashMap<>();
        return map;
    }

    @Override
    public String[] getPageParamsNames() {
        String[] pageParams = mBaseListImpl.getPageParamsNames();
        if (pageParams == null || pageParams.length < 2) {
            pageParams = new String[2];
            pageParams[0] = "page";
            pageParams[1] = "pagesize";
        }
        return pageParams;
    }

    @Override
    public RecyclerView.LayoutManager getLayoutManager() {
        return mBaseListImpl.getLayoutManager();
    }

    @Override
    public void onLoadMore() {
        mBaseListImpl.onLoadMore();
        requestData(mPageIndex, false);
    }

    /**
     * 到倒数第几个开始预加载；
     *
     * @return
     */
    public int getLoadMoreLastCount() {
        return mBaseListImpl.getLoadMoreLastCount();
    }

    @Override
    public void onLoadFinishedBefore() {
        mBaseListImpl.onLoadFinishedBefore();
    }

    @Override
    public void onLoadFinished() {
        mBaseListImpl.onLoadFinished();
    }


    @Override
    public void onRefresh() {
        mBaseListImpl.onRefresh();
        requestData(1, true);
    }

    @Override
    public View getHeaderView() {
        return mBaseListImpl.getHeaderView();
    }

    @Override
    public View getFooterView() {
        return mBaseListImpl.getFooterView();
    }

    public RecyclerListAdapter getRecyclerAdapter() {
        return mRecyclerAdapter;
    }

    public List<CardItemData> getData() {
        return mRecyclerAdapter.getDataSets();
    }

    public void showState(AbsStatefulLayout.State type) {
        switch (type) {
            case PROGRESS:
                mLayoutState.showProgress();
                break;
            case OFFLINE:
                mLayoutState.showOffline();
                break;
            case EMPTY:
                mLayoutState.showEmpty();
                break;
            case CONTENT:
                mLayoutState.showContent();
                break;
        }
    }

    private RecyclerView.OnScrollListener mOnScrollListener = new RecyclerView.OnScrollListener() {
        @Override
        public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
            super.onScrollStateChanged(recyclerView, newState);
            if (getLoadMoreLastCount() == -1) return;
            RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
            if (layoutManager instanceof LinearLayoutManager) {
                LinearLayoutManager linearManager = (LinearLayoutManager) layoutManager;
                //获取最后一个可见view的位置
                int lastItemPosition = linearManager.findLastVisibleItemPosition();
                //倒数第mLoadConditionPaddingSize个开始加载；
                if (lastItemPosition == recyclerView.getAdapter().getItemCount() - getLoadMoreLastCount()) {
                    DLog.e("onScrollStateChanged", "满足加载条件" + lastItemPosition);
                    onLoadMore();
                }
            }
        }


    };


    public void onDestroy() {
        cancelRequest();
    }


    public void cancelRequest() {
        if (mSubscribe != null) mSubscribe.cancel();
    }

    public int getPageIndex() {
        return mPageIndex;
    }
}
