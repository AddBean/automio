// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.agent.views.chat

import com.hive.plugin.agent.model.AttachmentType
import com.hive.plugin.agent.model.ChatMessage
import com.hive.plugin.agent.model.MessageRole
import com.hive.plugin.agent.model.MessageStatus

/**
 * 将原始 ChatMessage 列表转为 RecyclerView 展示数据。
 * 负责过滤 system / 空 WAITING，以及追加 thinking / compressing 尾占位。
 */
object AgentChatDisplayHelper {

    const val TYPE_USER_MESSAGE = 0
    const val TYPE_ASSISTANT_MESSAGE = 1
    const val TYPE_SYSTEM_MESSAGE = 2
    const val TYPE_TOOL_MESSAGE = 3
    const val TYPE_COMPRESSING_MEMORY = 4
    const val TYPE_THINKING_LOADING = 5

    fun buildDisplayData(
        source: List<ChatMessage>,
        isCompressingMemory: Boolean
    ): List<Pair<Int, Any?>> {
        val list = source.mapNotNull { message ->
            val viewType = when (message.role) {
                MessageRole.USER -> TYPE_USER_MESSAGE
                MessageRole.ASSISTANT -> TYPE_ASSISTANT_MESSAGE
                MessageRole.SYSTEM -> TYPE_SYSTEM_MESSAGE
                MessageRole.TOOL -> TYPE_TOOL_MESSAGE
            }
            if (viewType == TYPE_SYSTEM_MESSAGE) return@mapNotNull null
            if (viewType == TYPE_ASSISTANT_MESSAGE && isEmptyAssistantBubble(message)) {
                return@mapNotNull null
            }
            viewType to (message as Any?)
        }.toMutableList()

        if (isCompressingMemory) {
            list.add(TYPE_COMPRESSING_MEMORY to null)
        } else if (shouldShowThinkingLoading(source)) {
            list.add(TYPE_THINKING_LOADING to null)
        }
        return list
    }

    fun shouldShowThinkingLoading(source: List<ChatMessage>): Boolean {
        val last = source.lastOrNull { it.role != MessageRole.SYSTEM } ?: return false
        return last.role == MessageRole.ASSISTANT && last.status == MessageStatus.WAITING
    }

    fun isEmptyAssistantBubble(message: ChatMessage): Boolean {
        if (message.role != MessageRole.ASSISTANT) return false
        val hasContent = !message.content?.trim().isNullOrEmpty()
        val hasThinking = !message.reasoningContent.isNullOrEmpty()
        val hasImage = message.attachments.any {
            it.type == AttachmentType.IMAGE && !it.url.isNullOrEmpty()
        }
        return !hasContent && !hasThinking && !hasImage
    }
}
