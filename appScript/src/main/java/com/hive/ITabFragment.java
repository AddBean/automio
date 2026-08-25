// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive;

import com.hive.event.TabEvent;

public interface ITabFragment {
    void onTabEvent(TabEvent event);

    boolean onBackPressed();

}
