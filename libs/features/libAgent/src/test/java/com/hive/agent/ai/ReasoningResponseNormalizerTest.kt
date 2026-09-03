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
    fun `content think tag prefers explicit reasoning_content over think tag split`() {
        val normalized = ReasoningResponseNormalizer.normalize(
            content = "<think>ignored-tag</think>answer",
            reasoningContent = "api-reasoning",
            reasoningDetailsJson = null,
            usage = emptyMap(),
            providerId = "siliconflow",
            modelId = "Qwen/Qwen3-8B",
            replayFormat = ReasoningReplayFormat.CONTENT_THINK_TAG
        )

        assertEquals("<think>ignored-tag</think>answer", normalized.content)
        assertEquals("api-reasoning", normalized.reasoningContent)
        assertEquals("api-reasoning", normalized.reasoningTrace?.rawText)
        assertEquals(ReasoningReplayFormat.CONTENT_THINK_TAG, normalized.reasoningTrace?.replayFormat)
    }

    @Test
    fun `content think tag falls back to split when reasoning_content absent`() {
        val raw = "<think>from-tag</think>body"
        val normalized = ReasoningResponseNormalizer.normalize(
            content = raw,
            reasoningContent = null,
            usage = emptyMap(),
            providerId = "siliconflow",
            modelId = "Qwen/Qwen3-8B",
            replayFormat = ReasoningReplayFormat.CONTENT_THINK_TAG
        )
        assertEquals("body", normalized.content)
        assertEquals("from-tag", normalized.reasoningContent)
        assertEquals(raw, normalized.reasoningTrace?.rawText)
    }

    @Test
    fun `openrouter details preserve arrival order and store encrypted without using for UI`() {
        val details = JsonParser().parse(
            """
            [
              {"type":"reasoning.text","text":"first","id":"1"},
              {"type":"reasoning.encrypted","data":"enc-payload","id":"2","format":"anthropic-claude-v1"},
              {"type":"reasoning.text","text":"second","id":"3"}
            ]
            """.trimIndent()
        ).asJsonArray

        val normalized = ReasoningResponseNormalizer.normalize(
            content = "final",
            reasoningContent = null,
            reasoningDetailsJson = details,
            usage = emptyMap(),
            providerId = "openrouter",
            modelId = "openai/o3",
            replayFormat = ReasoningReplayFormat.REASONING_DETAILS
        )

        val traceDetails = normalized.reasoningTrace!!.details
        assertEquals(3, traceDetails.size)
        assertEquals("reasoning.text", traceDetails[0].type)
        assertEquals("first", traceDetails[0].text)
        assertEquals("reasoning.encrypted", traceDetails[1].type)
        assertNull(traceDetails[1].text)
        assertEquals("enc-payload", traceDetails[1].data["data"])
        assertEquals("anthropic-claude-v1", traceDetails[1].data["format"])
        assertEquals("reasoning.text", traceDetails[2].type)
        assertEquals("second", traceDetails[2].text)
        // UI text must not come from encrypted payload
        assertEquals("first", normalized.reasoningContent)
        assertTrue(normalized.reasoningContent?.contains("enc-payload") != true)
    }

    @Test
    fun `stream details accumulator keeps chunk order including encrypted`() {
        val acc = JsonArray()
        val chunk1 = JsonParser().parse(
            """[{"type":"reasoning.text","text":"a","id":"1"}]"""
        ).asJsonArray
        val chunk2 = JsonParser().parse(
            """[{"type":"reasoning.encrypted","data":"SECRET","id":"2"}]"""
        ).asJsonArray
        val chunk3 = JsonParser().parse(
            """[{"type":"reasoning.text","text":"b","id":"3"}]"""
        ).asJsonArray
        ReasoningResponseNormalizer.appendDetails(acc, chunk1)
        ReasoningResponseNormalizer.appendDetails(acc, chunk2)
        ReasoningResponseNormalizer.appendDetails(acc, chunk3)

        val normalized = ReasoningResponseNormalizer.normalize(
            content = "ok",
            reasoningContent = "ab",
            reasoningDetailsJson = acc,
            usage = emptyMap(),
            providerId = "openrouter",
            modelId = "x",
            replayFormat = ReasoningReplayFormat.REASONING_DETAILS
        )
        assertEquals(listOf("1", "2", "3"), normalized.reasoningTrace!!.details.map { it.id })
        assertEquals("SECRET", normalized.reasoningTrace!!.details[1].data["data"])
        assertEquals("ab", normalized.reasoningContent)
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

    @Test
    fun `provider parse usage includes nested reasoning_tokens`() {
        val responseJson = """
            {
              "model": "gpt-5",
              "choices": [{"message": {"role":"assistant","content":"hi"}, "finish_reason":"stop"}],
              "usage": {
                "prompt_tokens": 1,
                "completion_tokens": 2,
                "total_tokens": 3,
                "completion_tokens_details": { "reasoning_tokens": 9 }
              }
            }
        """.trimIndent()
        val root = JsonParser().parse(responseJson).asJsonObject
        val usage = ReasoningResponseNormalizer.extractUsage(root.getAsJsonObject("usage"))
        assertEquals(9, usage["reasoning_tokens"])
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
