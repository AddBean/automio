// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.carlos.ui.header;

import android.view.View;

public interface CommonHeaderListener {
    void onLeftClick(View v);
    default void onRightClick(View v){}
}
