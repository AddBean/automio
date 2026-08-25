// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.richeditor.views

import android.content.Context
import com.hive.editor.R
import com.hive.views.popmenu.PopMenuView

class EncodePopMenuView(context: Context):PopMenuView<String>(context) {
    override fun getLayoutId(): Int = R.layout.encode_pop_menu_view
}