// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.views.widgets;

import android.content.Context;

import com.hive.views.R;

public class DialogMessages extends CustomBaseDialog {

    public DialogMessages(Context context, String title, boolean isTouch) {
        super(context, title, isTouch);
    }

    public DialogMessages(Context context, String title) {
        super(context, title);
    }

    @Override
    protected void initView() {

    }

    @Override
    protected int getChildResId() {
        return R.layout.dialog_messages;
    }


}
