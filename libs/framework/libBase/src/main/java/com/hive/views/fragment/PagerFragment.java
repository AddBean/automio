// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.views.fragment;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.hive.base.BaseFragment;

public abstract class PagerFragment extends BaseFragment implements IPagerFragment {
    protected PagerTag mPagerTag;

    @Override
    public void initView() {
        if (mPagerTag == null)
            throw new RuntimeException("PagerFragment must call setPagerTag method first!");
    }

    public void setPagerTag(PagerTag mPagerTag) {
        this.mPagerTag = mPagerTag;
    }

    public PagerTag getFragmentTag() {
        return mPagerTag;
    }

    public abstract int getLayoutId();

    @Override
    public Fragment getFragment() {
        return this;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable("pageTag", mPagerTag);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            mPagerTag = (PagerTag) savedInstanceState.getSerializable("pageTag");
        }
    }
}
