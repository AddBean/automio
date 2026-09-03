// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.agent.ai

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.hive.plugin.agent.ReasoningAvailability
import com.hive.plugin.agent.model.ReasoningReplayFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReasoningResponseNormalizerTest {

    @Test
    fun `content think tag splits think block and keeps raw in trace`() {
        val raw = "<think>plan A</think>answer"
        val normalized = ReasoningResponseNormalizer.normalize(
            content = raw,
            reasoningContent = null,
            reasoningDetailsJson = null,
            usage = mapOf("prompt_tokens" to 1, "completion_tokens" to 2, "total_tokens" to 3),
            providerId = "minimax",
            modelId = "MiniMax-M2.5",
            replayFormat = ReasoningReplayFormat.CONTENT_THINK_TAG
        )

        assertEquals("answer", normalized.content)
        assertEquals("plan A", normalized.reasoningContent)
        assertEquals(raw, normalized.reasoningTrace?.rawText)
        assertEquals(ReasoningReplayFormat.CONTENT_THINK_TAG, normalized.reasoningTrace?.replayFormat)
        assertEquals("minimax", normalized.reasoningTrace?.sourceProviderId)
    }

    @Test
    fun `reasoning content passthrough builds trace`() {
        val normalized = ReasoningResponseNormalizer.normalize(
            content = "hello",
            reasoningContent = "thought",
            reasoningDetailsJson = null,
            usage = emptyMap(),
            providerId = "deepseek",
            modelId = "deepseek-reasoner",
            replayFormat = ReasoningReplayFormat.REASONING_CONTENT
        )
        assertEquals("hello", normalized.content)
        assertEquals("thought", normalized.reasoningContent)
        assertEquals("thought", normalized.reasoningTrace?.rawText)
        assertEquals(ReasoningReplayFormat.REASONING_CONTENT, normalized.reasoningTrace?.replayFormat)
    }

    @Test
    fun `openrouter reasoning_details are preserved and display text extracted`() {
        val details = JsonParser().parse(
            """
            [
              {"type":"reasoning.text","text":"step1","id":"r1"},
              {"type":"reasoning.summary","summary":"done","id":"r2"}
            ]
            """.trimIndent()
        ).asJsonArray

        val normalized = ReasoningResponseNormalizer.normalize(
            content = "final",
            reasoningContent = null,
            reasoningDetailsJson = details,
            usage = mapOf("reasoning_tokens" to 11),
            providerId = "openrouter",
            modelId = "openai/o3",
            replayFormat = ReasoningReplayFormat.REASONING_DETAILS
        )

        assertEquals("final", normalized.content)
        assertEquals("step1", normalized.reasoningContent)
        assertEquals(2, normalized.reasoningTrace?.details?.size)
        assertEquals(11, normalized.usage["reasoning_tokens"])
        assertEquals(ReasoningReplayFormat.REASONING_DETAILS, normalized.reasoningTrace?.replayFormat)
    }

    @Test
    fun `none replay leaves content untouched without trace when empty`() {
        val normalized = ReasoningResponseNormalizer.normalize(
            content = "plain",
            reasoningContent = null,
            reasoningDetailsJson = null,
            usage = emptyMap(),
            providerId = "mimo",
            modelId = "mimo-v2.5",
            replayFormat = ReasoningReplayFormat.NONE
        )
        assertEquals("plain", normalized.content)
        assertNull(normalized.reasoningContent)
        assertNull(normalized.reasoningTrace)
    }

    @Test
    fun `usage merges completion_tokens_details reasoning_tokens`() {
        val usageObj = JsonObject().apply {
            addProperty("prompt_tokens", 3)
            addProperty("completion_tokens", 5)
            addProperty("total_tokens", 8)
            add("completion_tokens_details", JsonObject().apply {
                addProperty("reasoning_tokens", 4)
            })
        }
        val usage = ReasoningResponseNormalizer.extractUsage(usageObj)
        assertEquals(3, usage["prompt_tokens"])
        assertEquals(5, usage["completion_tokens"])
        assertEquals(8, usage["total_tokens"])
        assertEquals(4, usage["reasoning_tokens"])
    }
}

class OpenRouterReasoningMetadataParserTest {

    @Test
    fun `null reasoning metadata stays unknown null`() {
        assertNull(OpenRouterReasoningMetadataParser.toCapabilities(null))
    }

    @Test
    fun `mandatory reasoning maps to REQUIRED`() {
        val json = JsonParser().parse(
            """
            {
              "supported_efforts": ["high", "medium", "low"],
              "default_effort": "medium",
              "default_enabled": true,
              "mandatory": true
            }
            """.trimIndent()
        ).asJsonObject

        val caps = OpenRouterReasoningMetadataParser.toCapabilities(json)!!
        assertEquals(ReasoningAvailability.REQUIRED, caps.availability)
        assertTrue(caps.supportedEfforts.containsAll(
            setOf(
                com.hive.plugin.agent.model.ReasoningEffort.LOW,
                com.hive.plugin.agent.model.ReasoningEffort.MEDIUM,
                com.hive.plugin.agent.model.ReasoningEffort.HIGH
            )
        ))
        assertEquals(com.hive.plugin.agent.model.ReasoningEffort.MEDIUM, caps.defaultEffort)
    }

    @Test
    fun `optional reasoning maps efforts and ignores unknown effort names`() {
        val json = JsonParser().parse(
            """
            {
              "supported_efforts": ["xhigh", "high", "minimal", "low"],
              "default_effort": "low",
              "mandatory": false
            }
            """.trimIndent()
        ).asJsonObject

        val caps = OpenRouterReasoningMetadataParser.toCapabilities(json)!!
        assertEquals(ReasoningAvailability.OPTIONAL, caps.availability)
        assertEquals(
            setOf(
                com.hive.plugin.agent.model.ReasoningEffort.HIGH,
                com.hive.plugin.agent.model.ReasoningEffort.LOW
            ),
            caps.supportedEfforts
        )
        assertEquals(com.hive.plugin.agent.model.ReasoningEffort.LOW, caps.defaultEffort)
    }
}
