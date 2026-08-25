// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.ui.mcp

import android.content.Context
import android.view.LayoutInflater
import android.widget.TextView
import com.hive.app.script.R
import com.hive.views.list_view.ListRecyclerItemView

class ToolHeaderItemView(context: Context) : ListRecyclerItemView(context) {

    private val tvDesc: TextView

    init {
        LayoutInflater.from(context).inflate(R.layout.item_tool_header, this, true)
        tvDesc = findViewById(R.id.tv_tool_desc)
    }

    fun setDescription(text: String) {
        tvDesc.text = text
    }

    override fun bindData(data: Any?) {
        // No data binding needed for header
    }
}
