// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.adapter;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.view.ViewGroup;


import com.hive.adapter.core.CardItemData;
import com.hive.adapter.core.ICardAdapter;
import com.hive.adapter.core.ICardItemFactory;
import com.hive.adapter.core.ICardItemView;

import java.util.ArrayList;
import java.util.List;

public class RecyclerAdapter extends RecyclerView.Adapter implements ICardAdapter {

    protected List<CardItemData> mData = new ArrayList<>();
    private ICardItemFactory mFactory;

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ICardItemView iCardItemView = mFactory.createItemView(parent.getContext(), viewType);
        return new RecyclerViewHolder(iCardItemView);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        RecyclerViewHolder viewHolder = (RecyclerViewHolder) holder;
        viewHolder.getCardItemView().bindData(mData.get(position));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position,List payloads) {
        RecyclerViewHolder viewHolder = (RecyclerViewHolder) holder;
        viewHolder.getCardItemView().bindData(mData.get(position));
    }

    @Override
    public int getItemViewType(int position) {
        return mData.get(position).cardType;
    }

    @Override
    public int getItemCount() {
        return mData.size();
    }

    @Override
    public void setFactory(ICardItemFactory factory) {
        mFactory = factory;
    }

    @Override
    public void setData(List<CardItemData> data) {
        int startInsert = mData.size() - 1;
        mData = data;
        notifyItemRangeInserted(startInsert, data.size());
    }

}
