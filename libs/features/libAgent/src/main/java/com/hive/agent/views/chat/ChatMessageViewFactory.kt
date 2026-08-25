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
        // 消息类型定义
        const val TYPE_USER_MESSAGE = 0
        const val TYPE_ASSISTANT_MESSAGE = 1
        const val TYPE_SYSTEM_MESSAGE = 2
        const val TYPE_TOOL_MESSAGE = 3
        const val TYPE_COMPRESSING_MEMORY = 4
    }

    override fun createItemView(viewType: Int): ListRecyclerItemView {
        return when (viewType) {
            TYPE_USER_MESSAGE -> ChatMessageItemView(context)
            TYPE_ASSISTANT_MESSAGE -> ChatMessageItemAssistantView(context)  // 使用增强版
            TYPE_SYSTEM_MESSAGE -> ChatMessageItemView(context)
            TYPE_TOOL_MESSAGE -> ChatMessageItemToolView(context)  // 使用专用工具消息视图
            TYPE_COMPRESSING_MEMORY -> ChatMessageItemCompressingView(context)
            else -> ChatMessageItemView(context)
        }
    }
} 