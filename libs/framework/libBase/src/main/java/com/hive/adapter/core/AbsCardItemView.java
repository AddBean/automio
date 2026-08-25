// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.adapter.core;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RelativeLayout;

import com.hive.base.IBaseEventInterface;
import com.hive.base.IBaseListInterface;
import com.hive.utils.utils.DensityUtil;

import java.util.List;

public abstract class AbsCardItemView extends RelativeLayout implements ICardItemView<CardItemData> {

    protected int DP = 1;
    private IBaseListInterface mListImpl;
    private IBaseEventInterface mEventImpl;

    public AbsCardItemView(Context context) {
        super(context);
        doInitBefore(null);
        init();
    }

    public AbsCardItemView(Context context, AttributeSet attrs) {
        super(context, attrs);
        doInitBefore(attrs);
        init();
    }

    public AbsCardItemView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        doInitBefore(attrs);
        init();
    }

    protected void doInitBefore(AttributeSet attrs) {

    }

    protected void init() {
        DP = DensityUtil.dip2px(1);
        View view = LayoutInflater.from(getContext()).inflate(getLayoutId(), this);
        initView(view);
    }

    public void setBaseListImpl(IBaseListInterface listImpl) {
        mListImpl = listImpl;
        mEventImpl = listImpl;
    }

    @Override
    public void onPayload(List payloads) {
    }

    public void setBaseEventImpl(IBaseEventInterface eventImpl) {
        mEventImpl = eventImpl;
    }

    protected abstract void initView(View view);

    public void postEvent(int event) {
        postEvent(event, null);
    }

    public void postEvent(int event, Object args) {
        if (mListImpl != null) {
            mListImpl.onCardEvent(event, args, this);
        } else {
            if (mEventImpl != null)
                mEventImpl.onCardEvent(event, args, this);
        }
    }

    @Override
    public View getView() {
        return this;
    }

    protected abstract int getLayoutId();

}
