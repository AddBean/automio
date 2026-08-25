// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.utils.adapter;

import androidx.recyclerview.widget.RecyclerView;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Created by AddBean on 2016/5/12.
 */
public class MultiViewHolder extends RecyclerView.ViewHolder {

    private SparseArray<View> viewList;

    public MultiViewHolder(View view) {
        super(view);
        this.viewList = new SparseArray<>();
    }

    public View getView() {
        return itemView;
    }

    public TextView getTextView(int viewId) {
        return retrieveView(viewId);
    }

    public Button getButton(int viewId) {
        return retrieveView(viewId);
    }

    public ImageView getImageView(int viewId) {
        return retrieveView(viewId);
    }

    public View getChildView(int viewId) {
        return retrieveView(viewId);
    }

    public View getmView() {
        return itemView;
    }

    @SuppressWarnings("unchecked")
    protected <T extends View> T retrieveView(int viewId) {
        View view = viewList.get(viewId);
        if (view == null) {
            view = itemView.findViewById(viewId);
            viewList.put(viewId, view);
        }
        return (T) view;
    }

    public void setVisibility(boolean isVisible) {
        RecyclerView.LayoutParams param = (RecyclerView.LayoutParams) itemView.getLayoutParams();
        if (param == null)
            param = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        if (isVisible) {
            param.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            param.width = ViewGroup.LayoutParams.MATCH_PARENT;
            itemView.setVisibility(View.VISIBLE);
        } else {
            itemView.setVisibility(View.GONE);
            param.height = 0;
            param.width = 0;
        }
        itemView.setLayoutParams(param);
    }
}
