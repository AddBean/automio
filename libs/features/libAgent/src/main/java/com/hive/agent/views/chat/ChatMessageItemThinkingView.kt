// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.agent.views.chat

import android.content.Context
import com.hive.agent.R
import com.hive.views.list_view.ListRecyclerItemView
import com.wang.avi.AVLoadingIndicatorView

/**
 * 思考中列表尾 loading。
 */
class ChatMessageItemThinkingView(context: Context) : ListRecyclerItemView(context) {

    private lateinit var thinkingIndicator: AVLoadingIndicatorView

    init {
        inflate(context, R.layout.chat_message_thinking_item, this)
        thinkingIndicator = findViewById(R.id.thinkingIndicator)
    }

    override fun bindData(data: Any?) {
        thinkingIndicator.show()
    }
}
