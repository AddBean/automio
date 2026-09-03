// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.agent.ai

import com.google.gson.JsonObject
import com.hive.plugin.agent.ReasoningAvailability
import com.hive.plugin.agent.ReasoningCapabilities
import com.hive.plugin.agent.model.ReasoningEffort

/**
 * Parses OpenRouter `/api/v1/models` `reasoning` metadata into [ReasoningCapabilities].
 */
object OpenRouterReasoningMetadataParser {

    fun toCapabilities(reasoning: JsonObject?): ReasoningCapabilities? {
        if (reasoning == null || reasoning.isJsonNull) return null

        val mandatory = reasoning.get("mandatory")
            ?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isBoolean }
            ?.asBoolean == true

        val efforts = mutableSetOf<ReasoningEffort>()
        reasoning.getAsJsonArray("supported_efforts")?.forEach { element ->
            if (!element.isJsonPrimitive) return@forEach
            parseEffort(element.asString)?.let { efforts.add(it) }
        }

        val defaultEffort = reasoning.get("default_effort")
            ?.takeIf { it.isJsonPrimitive }
            ?.asString
            ?.let { parseEffort(it) }
            ?: efforts.firstOrNull()

        return ReasoningCapabilities(
            availability = if (mandatory) {
                ReasoningAvailability.REQUIRED
            } else {
                ReasoningAvailability.OPTIONAL
            },
            supportedEfforts = efforts,
            defaultEffort = defaultEffort
        )
    }

    private fun parseEffort(raw: String): ReasoningEffort? = when (raw.trim().lowercase()) {
        "low" -> ReasoningEffort.LOW
        "medium" -> ReasoningEffort.MEDIUM
        "high" -> ReasoningEffort.HIGH
        else -> null
    }
}
