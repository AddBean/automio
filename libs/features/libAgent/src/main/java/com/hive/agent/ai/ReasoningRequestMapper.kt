// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.agent.ai

import com.google.gson.JsonObject
import com.hive.plugin.agent.model.ReasoningEffort
import com.hive.plugin.agent.model.ReasoningOptions

/**
 * Maps resolved [ReasoningOptions] onto provider-specific chat-completion JSON fields.
 * Null options mean "send nothing". Dialects that send no control params (NONE) are no-ops.
 */
object ReasoningRequestMapper {

    fun apply(
        request: JsonObject,
        dialect: ReasoningWireDialect,
        options: ReasoningOptions?
    ) {
        if (options == null) return
        when (dialect) {
            ReasoningWireDialect.OPENAI -> applyOpenAi(request, options)
            ReasoningWireDialect.OPENROUTER -> applyOpenRouter(request, options)
            ReasoningWireDialect.DEEPSEEK -> applyDeepSeek(request, options)
            ReasoningWireDialect.BAILIAN -> applyBailian(request, options)
            ReasoningWireDialect.KIMI -> applyKimi(request, options)
            ReasoningWireDialect.SILICONFLOW -> applySiliconFlow(request, options)
            ReasoningWireDialect.NONE -> Unit
        }
    }

    fun applyForProvider(
        request: JsonObject,
        providerId: String,
        options: ReasoningOptions?
    ) {
        apply(request, ReasoningModelCatalog.wireDialectFor(providerId), options)
    }

    private fun applyOpenAi(request: JsonObject, options: ReasoningOptions) {
        request.addProperty(
            "reasoning_effort",
            if (options.enabled) options.effort.wireName() else "none"
        )
        request.remove("temperature")
        val maxTokens = request.get("max_tokens")?.takeIf { it.isJsonPrimitive }?.asInt
        if (maxTokens != null) {
            request.remove("max_tokens")
            request.addProperty("max_completion_tokens", maxTokens)
        }
    }

    private fun applyOpenRouter(request: JsonObject, options: ReasoningOptions) {
        request.add("reasoning", JsonObject().apply {
            addProperty("enabled", options.enabled)
            addProperty("effort", options.effort.wireName())
            addProperty("exclude", false)
        })
    }

    private fun applyDeepSeek(request: JsonObject, options: ReasoningOptions) {
        request.add("thinking", JsonObject().apply {
            addProperty("type", if (options.enabled) "enabled" else "disabled")
        })
        if (options.enabled) {
            request.addProperty("reasoning_effort", options.effort.wireName())
            request.remove("temperature")
        }
    }

    private fun applyBailian(request: JsonObject, options: ReasoningOptions) {
        request.addProperty("enable_thinking", options.enabled)
        request.remove("reasoning_effort")
        if (!options.enabled) {
            request.remove("thinking_budget")
            return
        }
        when (options.effort) {
            ReasoningEffort.LOW -> request.addProperty("thinking_budget", 4096)
            ReasoningEffort.MEDIUM -> request.addProperty("thinking_budget", 16384)
            ReasoningEffort.HIGH -> request.remove("thinking_budget")
        }
    }

    private fun applyKimi(request: JsonObject, options: ReasoningOptions) {
        request.add("thinking", JsonObject().apply {
            addProperty("type", if (options.enabled) "enabled" else "disabled")
        })
        request.remove("temperature")
    }

    private fun applySiliconFlow(request: JsonObject, options: ReasoningOptions) {
        request.addProperty("enable_thinking", options.enabled)
        if (!options.enabled) {
            request.remove("thinking_budget")
            return
        }
        val budget = when (options.effort) {
            ReasoningEffort.LOW -> 4096
            ReasoningEffort.MEDIUM -> 16384
            ReasoningEffort.HIGH -> 32768
        }.coerceIn(128, 32768)
        request.addProperty("thinking_budget", budget)
    }

    private fun ReasoningEffort.wireName(): String = name.lowercase()
}
