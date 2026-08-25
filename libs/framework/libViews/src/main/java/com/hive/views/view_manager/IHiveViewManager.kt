// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.views.view_manager

import android.view.View

interface IHiveViewManager {
    fun updateViewLayout(view: View?)

    fun addView(view: View?)

    fun removeView(view: View?)

    fun updateLayoutParams(layoutParams: Any)
}