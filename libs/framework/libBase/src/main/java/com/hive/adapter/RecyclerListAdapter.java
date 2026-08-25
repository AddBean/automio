// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.adapter;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.hive.adapter.core.CardItemData;
import com.hive.adapter.core.ICardItemFactory;
import com.hive.adapter.core.ICardItemView;
import com.hive.adapter.holder.FooterViewHolder;
import com.hive.adapter.holder.HeaderViewHolder;
import com.hive.base.IBaseListInterface;
import com.hive.base.R;
import com.hive.utils.utils.CollectionUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RecyclerListAdapter extends RecyclerAdapter {
    private Context mContext;
    private View mHeaderView;
    private View mFooterView;
    private HeaderViewHolder mHeaderViewHolder;
    private FooterViewHolder mFooterViewHolder;
    private int mLastVisibleItem = 0;
    private final int TYPE_HEADER = -1;
    private final int TYPE_FOOTER = -2;
    private IBaseListInterface mBaseListImpl;
    protected List<CardItemData> mData = new ArrayList<>();
    private ICardItemFactory mFactory;

    public enum ELoadState {
        READY, LOADING, EMPTY, GONE, FAILED
    }

    private ELoadState mLoadState = ELoadState.READY;

    public RecyclerListAdapter(Context context) {
        this.mContext = context;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_HEADER) {
            mHeaderViewHolder = new HeaderViewHolder(mHeaderView);
            return mHeaderViewHolder;
        }
        if (viewType == TYPE_FOOTER) {
            mFooterViewHolder = new FooterViewHolder(mFooterView);
            return mFooterViewHolder;
        }
        ICardItemView iCardItemView = mFactory.createItemView(parent.getContext(), viewType);
        iCardItemView.setBaseListImpl(mBaseListImpl);
        return new RecyclerViewHolder(iCardItemView);
    }


    @Override
    public void setFactory(ICardItemFactory factory) {
        mFactory = factory;
    }

    @Override
    public void setData(List<CardItemData> data) {
        mData = data;
        if (mData != null) {
            for (int i = 0; i < mData.size(); i++) {
                mData.get(i).setPosition(i);
            }
        }
        notifyDataSetChanged();
    }


    public void addData(List<CardItemData> data) {
        int startInsert = mData.size();
        if (data != null) {
            for (int i = 0; i < data.size(); i++) {
                data.get(i).setPosition(startInsert + i);
            }
        }
        mData.addAll(data);
        if (mHeaderViewHolder != null) {
            startInsert++;
        }
        notifyItemRangeInserted(startInsert, data.size());
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position, @NonNull List payloads) {
        if (holder instanceof RecyclerViewHolder) {
            RecyclerViewHolder viewHolder = (RecyclerViewHolder) holder;
            if (mHeaderView != null)
                position--;
            if(CollectionUtil.empty(payloads)){
                viewHolder.getCardItemView().bindData(mData.get(position));
            }else{
                viewHolder.getCardItemView().onPayload(payloads);
            }
        }
    }


    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
//        super.onBindViewHolder(holder, position);
    }

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
        return mData.get(mPositionTemp).cardType;
    }

    public boolean isHeaderOrFooter(int position) {
        if (position == 0 && mHeaderView != null) return true;
        if (position == this.getItemCount() - 1 && mFooterView != null) return true;
        return false;
    }

    public void setHeader(RecyclerView.LayoutManager layoutManager, View headerView) {
        this.mHeaderView = headerView;
        if (layoutManager instanceof GridLayoutManager) {
            final GridLayoutManager mManager = ((GridLayoutManager) layoutManager);
            mManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
                @Override
                public int getSpanSize(int position) {
                    int size = isHeaderOrFooter(position) ? mManager.getSpanCount() : 1;
                    return size;
                }
            });
        }
    }


    public void notifyRecyclerDataSetChanged() {
        super.notifyDataSetChanged();
    }

    public void notifyRecyclerItemChanged(int pos) {
        if (mHeaderView != null)
            pos++;
        super.notifyItemChanged(pos);
    }

    public void notifyRecyclerItemChanged(int pos, Object payload) {
        if (mHeaderView != null)
            pos++;
        super.notifyItemChanged(pos, payload);
    }

    public void notifyRecyclerItemInserted(int pos) {
        if (mHeaderView != null)
            pos++;
        super.notifyItemInserted(pos);
    }

    public void notifyRecyclerItemMoved(int fromPosition, int toPosition) {
        if (mHeaderView != null) {
            fromPosition++;
            toPosition++;
        }
        super.notifyItemMoved(fromPosition, toPosition);

    }

    public void notifyRecyclerItemRangeChanged(int positionStart, int itemCount) {
        if (mHeaderView != null) {
            positionStart++;
        }
        super.notifyItemRangeChanged(positionStart, itemCount);
    }


    public void notifyRecyclerItemRangeChanged(int positionStart, int itemCount,
                                               @Nullable Object payload) {
        if (mHeaderView != null) {
            positionStart++;
        }
        super.notifyItemRangeChanged(positionStart, itemCount, payload);
    }

    public void notifyRecyclerItemRemoved(int position) {
        if (mHeaderView != null) {
            position++;
        }
        super.notifyItemRangeRemoved(position, 1);
    }

    public void setLoadMore(final RecyclerView.LayoutManager layoutManager, RecyclerView view, View footerView) {
        this.mFooterView = footerView == null ? LayoutInflater.from(mContext).inflate(R.layout.footer_view, null) : footerView;
        view.setOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState == RecyclerView.SCROLL_STATE_IDLE && mLastVisibleItem + 1 == RecyclerListAdapter.this.getItemCount()) {

                    if (mLoadState == ELoadState.READY || mLoadState == ELoadState.FAILED) {
                        setLoadState(ELoadState.LOADING);
                        if (mOnLoadListener != null)
                            mOnLoadListener.onLoadMore();
                    }
                }
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (layoutManager instanceof LinearLayoutManager)
                    mLastVisibleItem = ((LinearLayoutManager) layoutManager).findLastVisibleItemPosition();
            }
        });
        if (layoutManager instanceof GridLayoutManager) {
            final GridLayoutManager mManager = ((GridLayoutManager) layoutManager);
            mManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
                @Override
                public int getSpanSize(int position) {
                    int size = isHeaderOrFooter(position) ? mManager.getSpanCount() : 1;
                    return size;
                }
            });
        }
    }

    public List<CardItemData> getDataSets() {
        return mData;
    }

    public void setBaseListInterface(IBaseListInterface baseListImpl) {
        mBaseListImpl = baseListImpl;
    }

    private OnLoadListener mOnLoadListener;

    public void setOnLoadListener(OnLoadListener mOnLoadListener) {
        this.mOnLoadListener = mOnLoadListener;
    }

    public interface OnLoadListener {
        void onLoadMore();
    }

    public void setLoadState(ELoadState loadState) {
        this.mLoadState = loadState;
        if (mFooterViewHolder != null)
            mFooterViewHolder.setState(mLoadState);
    }
}