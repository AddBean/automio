// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.agent.views.chat

import android.content.Context
import android.widget.TextView
import com.hive.agent.R
import com.hive.utils.GlobalApp
import com.hive.views.list_view.ListRecyclerItemView

/**
 * 压缩记忆中占位视图
 */
class ChatMessageItemCompressingView(context: Context) : ListRecyclerItemView(context) {

    private lateinit var tvCompressing: TextView

    init {
        inflate(context, R.layout.chat_message_compressing_item, this)
        tvCompressing = findViewById(R.id.tvCompressing)
    }

    override fun bindData(data: Any?) {
        tvCompressing.text = GlobalApp.getString(com.hive.i8n.R.string.script_top_status_compressing_memory)
    }
}
