// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.views.fragment;

import androidx.fragment.app.Fragment;

public interface IPagerFragment {
    void setPagerTag(PagerTag pagerTag);

    PagerTag getFragmentTag();

    Fragment getFragment();
}
