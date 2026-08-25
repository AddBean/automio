// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.views.view_pager;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import com.hive.base.BaseFragment;
import com.hive.base.BaseLayout;
import com.hive.base.R;
import com.hive.views.fragment.PagerTag;

public abstract class PagerLayout extends BaseLayout implements IPagerLayout {
    protected PagerTag mPagerTag;

    public PagerLayout(Context context) {
        super(context);
    }

    public PagerLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public PagerLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }


    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {

    }

    public void setPagerTag(PagerTag mPagerTag) {
        this.mPagerTag = mPagerTag;
    }

    public PagerTag getLayoutTag() {
        return mPagerTag;
    }

    @Override
    public int getLayoutId() {
        return R.layout.pager_fragment;
    }

    @Override
    public View getLayout() {
        return this;
    }
}
