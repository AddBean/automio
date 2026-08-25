// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.views.widgets;

import android.app.Dialog;
import android.content.Context;

import androidx.annotation.NonNull;

import com.hive.views.R;

public class DialogLoading extends Dialog {
    public DialogLoading(@NonNull Context context) {
        super(context, R.style.base_dialog);
        setContentView(R.layout.dialog_loading);
    }

}
