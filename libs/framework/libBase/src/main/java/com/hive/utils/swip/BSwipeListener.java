// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.utils.swip;

public interface BSwipeListener {

    void onScroll(float percent, int px);

    void onEdgeTouch();

    void onScrollToClose();
}