// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.agent.utils

import android.text.TextUtils
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.hive.plugin.agent.model.FunctionCall
import com.hive.plugin.agent.model.ToolCall
import com.hive.utils.debug.DLog

object AgentToolCallUtils {
    /**
     * Build a unified ToolCall from provider-specific fields.
     */
    fun buildToolCall(
        logTag: String,
        id: String?,
        type: String?,
        functionName: String?,
        arguments: String?
    ): ToolCall? {
        if (functionName == null || id == null || type == null) return null

        var argumentsJson: JsonObject? = null
        if (arguments != null && !TextUtils.isEmpty(arguments)) {
            try {
                argumentsJson = JsonParser().parse(arguments).asJsonObject
            } catch (e: Exception) {
                DLog.w(logTag, "解析工具调用参数失败: $arguments, 错误: ${e.message}")
                argumentsJson = JsonObject()
            }
        }

        return ToolCall(
            id = id,
            type = type,
            function = FunctionCall(
                name = functionName,
                arguments = argumentsJson ?: JsonObject()
            )
        )
    }
}


