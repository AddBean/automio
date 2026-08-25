// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.views.fragment;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.hive.base.BaseListFragment;

public abstract class PagerListFragment extends BaseListFragment implements IPagerFragment {

    protected PagerTag mPagerTag;

    @Override
    public void doInitialize() {
        initFragment();
    }

    public abstract void initFragment();

    public void setPagerTag(PagerTag mPagerTag) {
        this.mPagerTag = mPagerTag;
        Bundle bundle = new Bundle();
        bundle.putSerializable("pageTag", mPagerTag);
        setArguments(bundle);
    }

    public PagerTag getFragmentTag() {
        if (mPagerTag != null)
            return mPagerTag;
        if (getArguments() != null)
            mPagerTag = (PagerTag) getArguments().getSerializable("pageTag");
        return mPagerTag;
    }

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
