// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.views.widgets.dialog.listener;

import android.view.View;

import com.hive.views.widgets.dialog.TDialog;
import com.hive.views.widgets.dialog.base.BindViewHolder;

public interface OnViewClickListener {
    void onViewClick(BindViewHolder viewHolder, View view, TDialog tDialog);
}
