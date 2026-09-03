// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.agent.ai

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.hive.plugin.agent.model.ChatMessage
import com.hive.plugin.agent.model.MessageRole
import com.hive.plugin.agent.model.ReasoningDetail
import com.hive.plugin.agent.model.ReasoningReplayFormat
import com.hive.plugin.agent.model.ReasoningTrace

/**
 * Decides whether historical assistant reasoning may be replayed on the next request,
 * and builds provider-specific wire payloads when the source provider/model match.
 */
object ReasoningReplayHelper {

    fun shouldReplay(
        message: ChatMessage,
        currentProviderId: String,
        currentModelId: String
    ): Boolean {
        if (message.role != MessageRole.ASSISTANT) return false
        return matchesSource(message.reasoningTrace, currentProviderId, currentModelId)
    }

    fun matchesSource(
        trace: ReasoningTrace?,
        currentProviderId: String,
        currentModelId: String
    ): Boolean {
        if (trace == null) return false
        val sourceProvider = trace.sourceProviderId?.takeIf { it.isNotBlank() } ?: return false
        val sourceModel = trace.sourceModelId?.takeIf { it.isNotBlank() } ?: return false
        return sourceProvider.equals(currentProviderId, ignoreCase = true) &&
            sourceModel.equals(currentModelId, ignoreCase = true)
    }

    /** Wire value for `reasoning_content` (DeepSeek / Kimi / similar). */
    fun reasoningContentForWire(
        message: ChatMessage,
        currentProviderId: String,
        currentModelId: String
    ): String? {
        if (!shouldReplay(message, currentProviderId, currentModelId)) return null
        message.reasoningContent?.takeIf { it.isNotEmpty() }?.let { return it }
        val trace = message.reasoningTrace ?: return null
        return when (trace.replayFormat) {
            ReasoningReplayFormat.REASONING_CONTENT,
            ReasoningReplayFormat.CONTENT_THINK_TAG ->
                trace.rawText?.takeIf { it.isNotEmpty() && !it.contains("<think>", ignoreCase = true) }
            else -> null
        }
    }

    /**
     * Message content for the wire. For MiniMax [ReasoningReplayFormat.CONTENT_THINK_TAG],
     * rebuilds the original `<think>...` form when replaying.
     */
    fun contentForWire(
        message: ChatMessage,
        currentProviderId: String,
        currentModelId: String
    ): String? {
        val base = when (message.role) {
            MessageRole.TOOL -> message.toolCallResult ?: message.content
            else -> message.content
        }
        if (!shouldReplay(message, currentProviderId, currentModelId)) return base
        val trace = message.reasoningTrace ?: return base
        if (trace.replayFormat != ReasoningReplayFormat.CONTENT_THINK_TAG) return base

        val raw = trace.rawText
        if (!raw.isNullOrEmpty() && raw.contains("<think>", ignoreCase = true)) {
            return raw
        }
        // MiniMax tool-turn continuity: rebuild think tags from display-split fields.
        if (!currentProviderId.equals("minimax", ignoreCase = true)) return base
        val thinking = message.reasoningContent?.takeIf { it.isNotEmpty() }
            ?: raw?.takeIf { it.isNotEmpty() }
            ?: return base
        return "<think>$thinking</think>${base.orEmpty()}"
    }

    /** OpenRouter `reasoning_details` array reconstructed from the stored trace. */
    fun reasoningDetailsForWire(
        message: ChatMessage,
        currentProviderId: String,
        currentModelId: String
    ): JsonArray? {
        if (!shouldReplay(message, currentProviderId, currentModelId)) return null
        val details = message.reasoningTrace?.details?.takeIf { it.isNotEmpty() } ?: return null
        return detailsToJsonArray(details)
    }

    fun detailsToJsonArray(details: List<ReasoningDetail>): JsonArray {
        val arr = JsonArray()
        for (detail in details) {
            arr.add(JsonObject().apply {
                detail.type?.let { addProperty("type", it) }
                detail.text?.let { addProperty("text", it) }
                detail.id?.let { addProperty("id", it) }
                detail.data.forEach { (key, value) -> addProperty(key, value) }
            })
        }
        return arr
    }
}
