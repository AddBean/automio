// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.agent.views.chat

import android.content.Context
import com.hive.views.list_view.ListRecyclerItemView
import com.hive.views.list_view.IListRecyclerViewFactory

/**
 * 聊天消息视图工厂
 * 根据消息类型创建对应的ItemView
 */
class ChatMessageViewFactory(private val context: Context) : IListRecyclerViewFactory {

    companion object {
        const val TYPE_USER_MESSAGE = AgentChatDisplayHelper.TYPE_USER_MESSAGE
        const val TYPE_ASSISTANT_MESSAGE = AgentChatDisplayHelper.TYPE_ASSISTANT_MESSAGE
        const val TYPE_SYSTEM_MESSAGE = AgentChatDisplayHelper.TYPE_SYSTEM_MESSAGE
        const val TYPE_TOOL_MESSAGE = AgentChatDisplayHelper.TYPE_TOOL_MESSAGE
        const val TYPE_COMPRESSING_MEMORY = AgentChatDisplayHelper.TYPE_COMPRESSING_MEMORY
        const val TYPE_THINKING_LOADING = AgentChatDisplayHelper.TYPE_THINKING_LOADING
    }

    override fun createItemView(viewType: Int): ListRecyclerItemView {
        return when (viewType) {
            TYPE_USER_MESSAGE -> ChatMessageItemView(context)
            TYPE_ASSISTANT_MESSAGE -> ChatMessageItemAssistantView(context)
            TYPE_SYSTEM_MESSAGE -> ChatMessageItemView(context)
            TYPE_TOOL_MESSAGE -> ChatMessageItemToolView(context)
            TYPE_COMPRESSING_MEMORY -> ChatMessageItemCompressingView(context)
            TYPE_THINKING_LOADING -> ChatMessageItemThinkingView(context)
            else -> ChatMessageItemView(context)
        }
    }
}
