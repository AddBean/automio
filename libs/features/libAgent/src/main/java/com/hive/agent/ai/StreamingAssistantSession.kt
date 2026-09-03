// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.agent.ai

import com.hive.plugin.agent.model.ChatCompletionResponse
import com.hive.plugin.agent.model.ChatMessage
import com.hive.plugin.agent.model.MessageRole
import com.hive.plugin.agent.model.MessageStatus

/**
 * 统一封装“占位 assistant + 流式 chunk 更新 + 节流通知 + 最终收尾写回”的消息更新逻辑，
 * 供 Agent 主循环与 Skill 子循环复用。
 */
class StreamingAssistantSession private constructor(
    private val assistant: ChatMessage,
    private val streamNotify: () -> Unit,
    private val throttleMs: Long,
    private val nowMs: () -> Long
) {
    private var lastNotifyMs: Long = 0L

    fun message(): ChatMessage = assistant

    fun onChunk(chunk: ChatCompletionResponse) {
        assistant.content = chunk.content ?: ""
        assistant.reasoningContent = chunk.reasoningContent ?: ""
        assistant.reasoningTrace = chunk.reasoningTrace
        if (throttleMs <= 0L) {
            streamNotify()
            return
        }
        val now = nowMs()
        if (now - lastNotifyMs >= throttleMs) {
            lastNotifyMs = now
            streamNotify()
        }
    }

    fun finalizeWith(final: ChatCompletionResponse) {
        assistant.content = final.content
        assistant.reasoningContent = final.reasoningContent
        assistant.reasoningTrace = final.reasoningTrace
        assistant.toolCalls = final.toolCalls
        assistant.status = if (final.toolCalls.isNullOrEmpty()) {
            MessageStatus.FINISH
        } else {
            MessageStatus.TOOL_RUNNING
        }
    }

    companion object {
        fun start(
            messages: MutableList<ChatMessage>,
            normalNotify: () -> Unit,
            streamNotify: () -> Unit,
            throttleMs: Long = 0L,
            nowMs: () -> Long = { System.currentTimeMillis() }
        ): StreamingAssistantSession {
            val assistant = ChatMessage(
                role = MessageRole.ASSISTANT,
                content = "",
                status = MessageStatus.WAITING
            )
            messages.add(assistant)
            normalNotify()
            return StreamingAssistantSession(
                assistant = assistant,
                streamNotify = streamNotify,
                throttleMs = throttleMs,
                nowMs = nowMs
            )
        }
    }
}

