// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.views.fragment;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

/**
 * @author jiadou
 * @date 2021/12/22
 */
public class PageData {
    public final String PAGE_TAG="page_tag";
    public PagerTag pagerTag;
    public Class fragmentClass;

    public PageData(Class fragmentClass) {
        this.fragmentClass = fragmentClass;
    }

    public PageData(PagerTag pagerTag, Class<IPagerFragment> fragmentClass) {
        this.pagerTag = pagerTag;
        this.fragmentClass = fragmentClass;
    }

    public Fragment newFragment() {
        try {
            Fragment fragment = (Fragment) fragmentClass.newInstance();
            if (fragment instanceof IPagerFragment) {
                ((IPagerFragment) fragment).setPagerTag(pagerTag);
            }
            Bundle bundle = new Bundle();
            bundle.putSerializable(PAGE_TAG, pagerTag);
            fragment.setArguments(bundle);
            return fragment;
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        }
        return null;
    }
}
