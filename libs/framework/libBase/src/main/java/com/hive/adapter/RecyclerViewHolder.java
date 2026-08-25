// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.adapter;

import androidx.recyclerview.widget.RecyclerView;

import com.hive.adapter.core.ICardItemView;
public class RecyclerViewHolder extends RecyclerView.ViewHolder {
    private ICardItemView mItemView;

    public RecyclerViewHolder(ICardItemView itemView) {
        super(itemView.getView());
        mItemView = itemView;
    }

    public ICardItemView getCardItemView() {
        return mItemView;
    }
}
