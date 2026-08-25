// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.views.view_pager;

import android.view.View;

import com.hive.views.fragment.PagerTag;

public interface IPagerLayout {

    void setUserVisibleHint(boolean isVisibleToUser);

    void setPagerTag(PagerTag mPagerTag);

    PagerTag getLayoutTag();

    View getLayout();
}
