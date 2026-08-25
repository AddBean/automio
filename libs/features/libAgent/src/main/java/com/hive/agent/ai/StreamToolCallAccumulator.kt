// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.agent.ai

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.hive.plugin.agent.model.FunctionCall
import com.hive.plugin.agent.model.ToolCall
import com.hive.utils.debug.DLog
import java.util.UUID

/**
 * Shared helper to accumulate streaming tool_calls across chunks and build final ToolCall list
 */
class StreamToolCallAccumulator(private val logTag: String) {

    private data class MutableToolCall(
        var id: String = "",
        var type: String = "function",
        var functionName: String = "",
        var accumulatedArguments: String = "",
        var isArgumentsComplete: Boolean = false
    )

    private val toolCallsInProgress: MutableMap<Int, MutableToolCall> = mutableMapOf()

    fun update(
        index: Int?,
        id: String?,
        type: String?,
        functionName: String?,
        argumentsChunk: String?
    ) {
        if (index == null) return
        val toolCall = toolCallsInProgress.getOrPut(index) { MutableToolCall() }

        if (id != null && id.isNotEmpty()) {
            if (toolCall.id != id) {
                toolCall.id = id
            }
        }

        if (type != null && type.isNotEmpty()) {
            if (toolCall.type != type) {
                toolCall.type = type
            }
        }

        // 流式 tool_calls 中，只有首个 chunk 含完整 name，后续 chunk 的 name 为空，仅增量 arguments
        // 若用空字符串覆盖则会导致方法名丢失（如 qwen3-max 等模型）
        if (functionName != null && functionName.isNotEmpty()) {
            if (toolCall.functionName != functionName) {
                toolCall.functionName = functionName
            }
        }

        argumentsChunk?.let { args ->
            if (args.isNotEmpty()) {
                toolCall.accumulatedArguments += args
                if (args.trim().endsWith("}")) {
                    toolCall.isArgumentsComplete = true
                }
            }
        }
    }

    fun toToolCalls(): List<ToolCall> {
        return toolCallsInProgress.values.map { it.toToolCall(logTag) }
    }

    fun toDistinctToolCalls(): List<ToolCall> {
        return toToolCalls().distinctBy { it.id }
    }

    /**
     * 从 tool call id 解析完整工具名，兼容 DashScope/kimi 等返回不完整 name 的情况。
     * id 格式: functions.buildin.open:0 -> 解析出 buildin.open
     */
    private fun resolveToolNameFromId(id: String, functionName: String, logTag: String): String {
        if (id.isEmpty()) return functionName
        val match = Regex("""^functions\.([^:]+):\d+$""").find(id)
        val fromId = match?.groupValues?.getOrNull(1)?.takeIf { it.isNotEmpty() }
        return when {
            fromId != null -> {
                if (fromId != functionName) {
                    DLog.d(logTag, "从 id 解析完整工具名: $functionName -> $fromId")
                }
                fromId
            }
            else -> functionName
        }
    }

    private fun MutableToolCall.toToolCall(logTag: String): ToolCall {
        var argumentsJson: JsonObject? = null
        if (accumulatedArguments.isNotEmpty() && isArgumentsComplete) {
            try {
                val trimmedArgs = accumulatedArguments.trim()
                if (trimmedArgs.startsWith("{") && trimmedArgs.endsWith("}")) {
                    argumentsJson = JsonParser().parse(trimmedArgs).asJsonObject
                } else {
                    DLog.w(logTag, "参数不是有效的JSON对象: $trimmedArgs")
                    argumentsJson = JsonObject()
                }
            } catch (e: Exception) {
                DLog.w(logTag, "解析工具调用参数失败: $accumulatedArguments, 错误: ${e.message}")
                argumentsJson = JsonObject()
            }
        }

        val finalId = if (id.isEmpty()) "tool_call_${UUID.randomUUID().toString().take(8)}" else id

        // 兼容 DashScope/kimi 等模型流式返回不完整 name（如 "buildin" 而非 "buildin.open"）的情况，
        // 从 id 格式 functions.{fullName}:{index} 解析完整工具名
        val resolvedName = resolveToolNameFromId(finalId, functionName, logTag)

        return ToolCall(
            id = finalId,
            type = type,
            function = FunctionCall(
                name = resolvedName,
                arguments = argumentsJson ?: JsonObject()
            )
        )
    }
}

