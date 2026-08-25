// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.utils.statusbar;

import android.view.Window;

interface IStatusBar {
    void setStatusBarColor(Window window, int color, boolean lightStatusBar);
    void toggleStatusBarVisible(boolean show);
    void clearStatusBarColor();
    void setStatusBarImmersion(Window window);
}
