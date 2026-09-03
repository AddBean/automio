// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.agent.ai

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.hive.plugin.agent.model.ReasoningDetail
import com.hive.plugin.agent.model.ReasoningReplayFormat
import com.hive.plugin.agent.model.ReasoningTrace

data class NormalizedReasoningResponse(
    val content: String?,
    val reasoningContent: String?,
    val usage: Map<String, Int>,
    val reasoningTrace: ReasoningTrace?
)

/**
 * Normalizes provider-specific reasoning payloads into display text + raw [ReasoningTrace].
 */
object ReasoningResponseNormalizer {

    private val thinkTagRegex = Regex(
        """(?s)<think>(.*?)</think>""",
        setOf(RegexOption.IGNORE_CASE)
    )

    fun normalize(
        content: String?,
        reasoningContent: String?,
        reasoningDetailsJson: JsonArray? = null,
        usage: Map<String, Int> = emptyMap(),
        providerId: String,
        modelId: String?,
        replayFormat: ReasoningReplayFormat
    ): NormalizedReasoningResponse {
        return when (replayFormat) {
            ReasoningReplayFormat.CONTENT_THINK_TAG -> normalizeThinkTag(
                content, usage, providerId, modelId
            )
            ReasoningReplayFormat.REASONING_DETAILS -> normalizeDetails(
                content, reasoningContent, reasoningDetailsJson, usage, providerId, modelId
            )
            ReasoningReplayFormat.REASONING_CONTENT -> normalizeReasoningContent(
                content, reasoningContent, usage, providerId, modelId
            )
            ReasoningReplayFormat.NONE -> NormalizedReasoningResponse(
                content = content,
                reasoningContent = reasoningContent?.takeIf { it.isNotEmpty() },
                usage = usage,
                reasoningTrace = null
            )
        }
    }

    fun extractUsage(usageObj: JsonObject?): Map<String, Int> {
        if (usageObj == null) return emptyMap()
        val map = mutableMapOf<String, Int>()
        usageObj.get("prompt_tokens")?.asIntOrNull()?.let { map["prompt_tokens"] = it }
        usageObj.get("completion_tokens")?.asIntOrNull()?.let { map["completion_tokens"] = it }
        usageObj.get("total_tokens")?.asIntOrNull()?.let { map["total_tokens"] = it }
        usageObj.get("reasoning_tokens")?.asIntOrNull()?.let { map["reasoning_tokens"] = it }
        usageObj.getAsJsonObject("completion_tokens_details")
            ?.get("reasoning_tokens")
            ?.asIntOrNull()
            ?.let { map["reasoning_tokens"] = it }
        return map
    }

    fun replayFormatFor(providerId: String): ReasoningReplayFormat =
        ReasoningModelCatalog.replayFormatFor(providerId)

    private fun normalizeThinkTag(
        content: String?,
        usage: Map<String, Int>,
        providerId: String,
        modelId: String?
    ): NormalizedReasoningResponse {
        val raw = content ?: return NormalizedReasoningResponse(null, null, usage, null)
        val match = thinkTagRegex.find(raw)
        if (match == null) {
            return NormalizedReasoningResponse(
                content = raw,
                reasoningContent = null,
                usage = usage,
                reasoningTrace = null
            )
        }
        val thinking = match.groupValues[1].trim()
        val displayContent = raw.replace(match.value, "").trim().ifEmpty { null }
        return NormalizedReasoningResponse(
            content = displayContent,
            reasoningContent = thinking.takeIf { it.isNotEmpty() },
            usage = usage,
            reasoningTrace = ReasoningTrace(
                rawText = raw,
                sourceProviderId = providerId,
                sourceModelId = modelId,
                replayFormat = ReasoningReplayFormat.CONTENT_THINK_TAG
            )
        )
    }

    private fun normalizeReasoningContent(
        content: String?,
        reasoningContent: String?,
        usage: Map<String, Int>,
        providerId: String,
        modelId: String?
    ): NormalizedReasoningResponse {
        val thinking = reasoningContent?.takeIf { it.isNotEmpty() }
        return NormalizedReasoningResponse(
            content = content,
            reasoningContent = thinking,
            usage = usage,
            reasoningTrace = thinking?.let {
                ReasoningTrace(
                    rawText = it,
                    sourceProviderId = providerId,
                    sourceModelId = modelId,
                    replayFormat = ReasoningReplayFormat.REASONING_CONTENT
                )
            }
        )
    }

    private fun normalizeDetails(
        content: String?,
        reasoningContent: String?,
        reasoningDetailsJson: JsonArray?,
        usage: Map<String, Int>,
        providerId: String,
        modelId: String?
    ): NormalizedReasoningResponse {
        val details = reasoningDetailsJson?.mapNotNull { element ->
            val obj = element.takeIf { it.isJsonObject }?.asJsonObject ?: return@mapNotNull null
            val type = obj.get("type")?.asStringOrNull()
            val text = obj.get("text")?.asStringOrNull()
                ?: obj.get("summary")?.asStringOrNull()
            val id = obj.get("id")?.asStringOrNull()
            val data = mutableMapOf<String, String>()
            obj.entrySet().forEach { (key, value) ->
                if (key == "type" || key == "text" || key == "summary" || key == "id") return@forEach
                if (value.isJsonPrimitive) data[key] = value.asString
            }
            ReasoningDetail(type = type, text = text, id = id, data = data)
        }.orEmpty()

        val display = reasoningContent?.takeIf { it.isNotEmpty() }
            ?: details.firstOrNull { !it.text.isNullOrEmpty() }?.text

        val trace = if (details.isNotEmpty() || !display.isNullOrEmpty()) {
            ReasoningTrace(
                rawText = display,
                details = details,
                sourceProviderId = providerId,
                sourceModelId = modelId,
                replayFormat = ReasoningReplayFormat.REASONING_DETAILS
            )
        } else null

        return NormalizedReasoningResponse(
            content = content,
            reasoningContent = display,
            usage = usage,
            reasoningTrace = trace
        )
    }

    private fun JsonElement.asIntOrNull(): Int? =
        takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isNumber }?.asInt

    private fun JsonElement.asStringOrNull(): String? =
        takeIf { it.isJsonPrimitive }?.asString
}
