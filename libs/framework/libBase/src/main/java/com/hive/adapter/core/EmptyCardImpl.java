// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.adapter.core;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import com.hive.base.R;
import com.hive.utils.utils.GsonHelper;

public class EmptyCardImpl extends AbsCardItemView {

    public CardItemData mData;

    public EmptyCardImpl(Context context) {
        super(context);
    }

    public EmptyCardImpl(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public EmptyCardImpl(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void initView(View view) {
    }


    @Override
    protected int getLayoutId() {
        return R.layout.empty_card_item_impl;
    }

    @Override
    public void bindData(CardItemData data) {
        mData = data;
        if (data == null || data.data == null) return;
        try{
            ((TextView) findViewById(R.id.tv_msg)).setText(GsonHelper.getInstance().toJson(data.data));
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
