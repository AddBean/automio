// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.utils.adapter;

/**
 * Created by AddBean on 2016/5/12.
 */
public class ItemMeta {
    private int mLayoutId;
    private Object mData;
    private boolean mSelected=false;
    private boolean mEditable=false;

    public ItemMeta(int mLayoutId, Object mData) {
        this.mLayoutId = mLayoutId;
        this.mData = mData;
    }

    public boolean isEditable() {
        return mEditable;
    }

    public void setEditable(boolean mEditable) {
        this.mEditable = mEditable;
    }

    public boolean isSelected() {
        return mSelected;
    }

    public void setSelected(boolean mSelected) {
        this.mSelected = mSelected;
    }

    public int getmLayoutId() {
        return mLayoutId;
    }

    public void setmLayoutId(int mLayoutId) {
        this.mLayoutId = mLayoutId;
    }

    public Object getmData() {
        return mData;
    }

    public void setmData(Object mData) {
        this.mData = mData;
    }
}
