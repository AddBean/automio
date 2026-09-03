// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.agent.ai.providers

import com.google.gson.JsonArray
import com.hive.plugin.agent.ProviderInfo
import com.hive.plugin.agent.model.ChatCompletionResponse
import com.hive.plugin.agent.model.ReasoningReplayFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OpenRouterStreamReasoningDetailsTest {

    private val provider = TestOpenRouterProvider()

    @Test
    fun `stream chunks accumulate reasoning_details in order including encrypted`() {
        val chunkA = """
            {"model":"openai/o3","choices":[{"delta":{"reasoning_details":[
              {"type":"reasoning.text","text":"step-a","id":"a"}
            ]},"finish_reason":null}]}
        """.trimIndent()
        val chunkB = """
            {"model":"openai/o3","choices":[{"delta":{"reasoning_details":[
              {"type":"reasoning.encrypted","data":"ENCRYPTED","id":"b","format":"x"}
            ]},"finish_reason":null}]}
        """.trimIndent()
        val chunkC = """
            {"model":"openai/o3","choices":[{"delta":{"content":"done","reasoning_details":[
              {"type":"reasoning.text","text":"step-c","id":"c"}
            ]},"finish_reason":"stop"}]}
        """.trimIndent()

        val response = provider.accumulateAndBuild(listOf(chunkA, chunkB, chunkC))
        val details = response.reasoningTrace!!.details
        assertEquals(3, details.size)
        assertEquals(listOf("a", "b", "c"), details.map { it.id })
        assertEquals("ENCRYPTED", details[1].data["data"])
        assertNull(details[1].text)
        assertEquals(ReasoningReplayFormat.REASONING_DETAILS, response.reasoningTrace!!.replayFormat)
        assertTrue(response.reasoningContent?.contains("ENCRYPTED") != true)
        assertEquals(5, response.usage["reasoning_tokens"])
    }

    private class TestOpenRouterProvider : OpenRouterProvider() {
        override fun getProviderInfo(): ProviderInfo = ProviderInfo(
            name = "openrouter",
            displayName = "OpenRouter",
            description = "OpenRouter",
            defaultModelId = "openai/o3",
            defaultMultiModelId = null,
            isEnabled = true,
            tags = emptyList(),
            apiKeyPrefix = "",
            apiKeyValidateMsg = "",
            apiUrl = "https://openrouter.ai",
            sortIndex = 1
        )

        fun accumulateAndBuild(chunks: List<String>): ChatCompletionResponse {
            val acc = JsonArray()
            var reasoningText = ""
            var content = ""
            chunks.forEach { chunk ->
                val parsed = parseStreamResponse(chunk)
                parsed.reasoningDetailsChunk?.forEach { acc.add(it) }
                reasoningText += parsed.reasoningContent.orEmpty()
                content += parsed.content.orEmpty()
            }
            return buildStreamResponse(
                accumulatedContent = content,
                accumulatedReasoningContent = reasoningText.ifEmpty { null },
                model = "openai/o3",
                usage = mapOf("reasoning_tokens" to 5),
                toolCalls = null,
                cost = null,
                reasoningDetails = acc
            )
        }
    }
}
