// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.views.view_pager;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import com.hive.base.BaseLayout;
import com.hive.base.BaseListLayout;
import com.hive.base.R;
import com.hive.views.fragment.PagerTag;

public abstract class PagerListLayout extends BaseListLayout implements IPagerLayout {
    protected PagerTag mPagerTag;

    public PagerListLayout(Context context) {
        super(context);
    }

    public PagerListLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public PagerListLayout(Context context, AttributeSet attrs, int defStyleAttr) {
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
    public View getLayout() {
        return this;
    }
}
