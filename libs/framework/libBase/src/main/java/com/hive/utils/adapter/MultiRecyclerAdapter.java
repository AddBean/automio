// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.utils.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.hive.base.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by AddBean on 2016/5/12.
 */
public abstract class MultiRecyclerAdapter extends RecyclerView.Adapter {
    private List<ItemMeta> mData = new ArrayList<>();
    private Context mContext;
    private OnItemClickListener mOnItemClickListener;
    private OnLongItemClickListener mOnLongItemClickListener;
    private View mHeaderView;
    private View mFooterView;
    private int mLastVisibleItem = 0;
    private final int TYPE_HEADER = -1;
    private final int TYPE_FOOTER = -2;
    private final int TYPE_CONTENT = 0;
    private int mCurrentPosition = 0;

    public MultiRecyclerAdapter(Context context, List<ItemMeta> mData) {
        this.mContext = context;
        this.mData = mData;
    }


    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(final ViewGroup parent, final int viewType) {
//        ULog.e("viewType:" + viewType);
        if (viewType == TYPE_HEADER) {
            return new MultiViewHolder(mHeaderView);
        }
        if (viewType == TYPE_FOOTER) {
            return new MultiViewHolder(mFooterView);
        }
        View view = LayoutInflater.from(parent.getContext()).inflate(viewType, null);
//        ULog.e("正在创建：viewholder");
        return new MultiViewHolder(view);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position) {
        if (isHeaderOrFooter(position)) return;
        MultiViewHolder muiltViewHolder = ((MultiViewHolder) holder);
        int mPositionTemp = position;
        if (mHeaderView != null && mPositionTemp > 0)
            mPositionTemp--;
        if (muiltViewHolder.getView() != mFooterView && muiltViewHolder.getView() != mHeaderView) {
            final int finalMPositionTemp = mPositionTemp;
            muiltViewHolder.getView().setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mOnItemClickListener != null)
                        mOnItemClickListener.onClick(v, finalMPositionTemp, mData.get(finalMPositionTemp));
                }
            });
            muiltViewHolder.getView().setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    if (mOnLongItemClickListener != null)
                        mOnLongItemClickListener.onLongClick(v, finalMPositionTemp, mData.get(finalMPositionTemp));
                    return false;
                }
            });
            convert(muiltViewHolder, mPositionTemp, mData.get(mPositionTemp));
        }
    }

    public abstract void convert(MultiViewHolder holder, int position, ItemMeta data);


    @Override
    public int getItemCount() {
        int size = mData.size();
        if (mHeaderView != null) size++;
        if (mFooterView != null) size++;
        return size;
    }

    @Override
    public int getItemViewType(int position) {
        if (0 == position && mHeaderView != null) {
            return TYPE_HEADER;
        }
        if ((getItemCount() - 1) == position && mFooterView != null) {
            return TYPE_FOOTER;
        }
        int mPositionTemp = position;
        if (mHeaderView != null && mPositionTemp > 0)
            mPositionTemp--;
        return mData.get(mPositionTemp).getmLayoutId();
    }

    public void setOnItemClickListener(OnItemClickListener mOnItemClickListener) {
        this.mOnItemClickListener = mOnItemClickListener;
    }

    public void setHeaderAndFooter(RecyclerView.LayoutManager mLayoutManager, View mHeaderView, View mFooterView) {
        this.mHeaderView = mHeaderView;
        this.mFooterView = mFooterView;
        if (mFooterView == null && mHeaderView == null) return;
        if (mLayoutManager instanceof GridLayoutManager) {
            final GridLayoutManager mManager = ((GridLayoutManager) mLayoutManager);
            mManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
                @Override
                public int getSpanSize(int position) {
                    int size = isHeaderOrFooter(position) ? mManager.getSpanCount() : 1;
                    return size;
                }
            });
        }
    }

    private boolean isHeaderOrFooter(int position) {
        if (position == 0 && mHeaderView != null) return true;
        if (position == this.getItemCount() - 1 && mFooterView != null) return true;
        return false;
    }


    public enum ELoadState {
        READY, LOADING, EMPTY, GONE;
    }

    public void setOnLongItemClickListener(OnLongItemClickListener mOnLongItemClickListener) {
        this.mOnLongItemClickListener = mOnLongItemClickListener;
    }

    private ELoadState mLoadState = ELoadState.READY;

    public void setLoadMoreEnable(final RecyclerView.LayoutManager mLayoutManager, RecyclerView view) {
        this.mFooterView = LayoutInflater.from(mContext).inflate(R.layout.footer_view, null);
        setHeaderAndFooter(mLayoutManager, mHeaderView, mFooterView);
        view.setOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState == RecyclerView.SCROLL_STATE_IDLE && mLastVisibleItem + 1 == MultiRecyclerAdapter.this.getItemCount()) {
                    if (mOnLoadListener == null) return;
                    if (mLoadState == ELoadState.READY) {
                        mOnLoadListener.onLoadMore();
                        setLoadState(ELoadState.LOADING);
                    }
                }
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (mLayoutManager instanceof LinearLayoutManager)
                    mLastVisibleItem = ((LinearLayoutManager) mLayoutManager).findLastVisibleItemPosition();
            }
        });
    }


    private OnLoadListener mOnLoadListener;

    public void setOnLoadListener(OnLoadListener mOnLoadListener) {
        this.mOnLoadListener = mOnLoadListener;
    }

    public interface OnLoadListener {
        public void onLoadMore();
    }

    public void setLoadState(final ELoadState mLoadState) {
        this.mLoadState = mLoadState;
        final TextView msg = (TextView) mFooterView.findViewById(R.id.text_msg);
        final View animView = mFooterView.findViewById(R.id.view_anim);
        switch (mLoadState) {
            case GONE:
                msg.setText(mContext.getString(com.hive.i8n.R.string.base_list_uppull));
                mFooterView.setVisibility(View.GONE);
                break;
            case LOADING:
                msg.setText(mContext.getString(com.hive.i8n.R.string.base_list_loading));
                animView.setVisibility(View.VISIBLE);
                mFooterView.setVisibility(View.VISIBLE);
                break;
            case READY:
                msg.setText(mContext.getString(com.hive.i8n.R.string.base_list_uppull));
                animView.setVisibility(View.GONE);
                mFooterView.setVisibility(View.VISIBLE);
                break;
            case EMPTY:
                msg.setText(mContext.getString(com.hive.i8n.R.string.base_list_empty));
                animView.setVisibility(View.GONE);
                mFooterView.setVisibility(View.VISIBLE);
                break;
        }

    }

}
