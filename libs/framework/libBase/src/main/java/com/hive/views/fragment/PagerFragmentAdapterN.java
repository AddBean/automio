// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.views.fragment;

import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.viewpager.widget.PagerAdapter;

import java.util.ArrayList;
import java.util.List;

public class PagerFragmentAdapterN extends PagerAdapter {
    private final FragmentManager mFragmentManager;
    private FragmentTransaction mCurTransaction = null;
    private Fragment mCurrentPrimaryItem = null;
    private List<PageData> mFragmentPageData = new ArrayList<>();
    public boolean mEnableDetachFragment = true;
    private int mContainerId = -1;

    public PagerFragmentAdapterN(FragmentManager fm) {
        mFragmentManager = fm;
    }

    public void setFragmentData(List<PageData> mFragments) {
        this.mFragmentPageData = mFragments;
    }

    @Override
    public int getCount() {
        return mFragmentPageData.size();
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        mContainerId = container.getId();
        if (mCurTransaction == null) {
            mCurTransaction = mFragmentManager.beginTransaction();
        }
        final long itemId = getItemId(position);
        String name = getFragmentName(itemId);
        Fragment fragment = mFragmentManager.findFragmentByTag(name);
        if (fragment != null) {
            if (mEnableDetachFragment) {
                mCurTransaction.attach(fragment);
            }
        } else {
            fragment = (Fragment) mFragmentPageData.get(position).newFragment();
            mCurTransaction.add(container.getId(), fragment,
                    getFragmentName(itemId));
        }
        if (fragment != mCurrentPrimaryItem) {
            fragment.setMenuVisibility(false);
            fragment.setUserVisibleHint(false);
        }

        return fragment;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        if (mEnableDetachFragment) {
            if (mCurTransaction == null) {
                mCurTransaction = mFragmentManager.beginTransaction();
            }
            mCurTransaction.detach((Fragment) object);
        }
    }

    @Override
    public void setPrimaryItem(ViewGroup container, int position, Object object) {
        Fragment fragment = (Fragment) object;
        if (fragment != mCurrentPrimaryItem) {
            if (mCurrentPrimaryItem != null) {
                mCurrentPrimaryItem.setMenuVisibility(false);
                mCurrentPrimaryItem.setUserVisibleHint(false);
            }
            if (fragment != null) {
                fragment.setMenuVisibility(true);
                fragment.setUserVisibleHint(true);
            }
            mCurrentPrimaryItem = fragment;
        }
    }

    @Override
    public void finishUpdate(ViewGroup container) {
        if (mCurTransaction != null) {
            mCurTransaction.commitAllowingStateLoss();
            mCurTransaction = null;
            mFragmentManager.executePendingTransactions();
        }
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return ((Fragment) object).getView() == view;
    }

    public void setEnableDetachFragment(boolean mEnableDetachFragment) {
        this.mEnableDetachFragment = mEnableDetachFragment;
    }

    public long getItemId(int position) {
        return position;
    }

    public Fragment getFragmentByPosition(int pos){
        String name=getFragmentName(getItemId(pos));
        Fragment fragment = mFragmentManager.findFragmentByTag(name);
        return fragment;
    }

    private String getFragmentName(long id) {
        return "android:switcher:" + mContainerId + ":" + id;
    }
}