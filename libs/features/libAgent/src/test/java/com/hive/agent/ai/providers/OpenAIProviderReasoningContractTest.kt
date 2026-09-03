// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.agent.ai.providers

import com.google.gson.JsonArray
import com.google.gson.JsonParser
import com.hive.plugin.agent.ProviderInfo
import com.hive.plugin.agent.model.ChatMessage
import com.hive.plugin.agent.model.MessageRole
import com.hive.plugin.agent.model.ReasoningEffort
import com.hive.plugin.agent.model.ReasoningOptions
import com.hive.plugin.agent.model.ReasoningReplayFormat
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OpenAIProviderReasoningContractTest {

    @Test
    fun `custom openai never emits reasoning_effort even with options`() = runBlocking {
        val provider = TestCustomOpenAIProvider()
        val request = provider.buildRequest(
            model = "gpt-5",
            messages = listOf(ChatMessage(MessageRole.USER, "hi")),
            temperature = 0.7f,
            maxTokens = 100,
            stream = false,
            tools = null,
            reasoning = ReasoningOptions(enabled = true, effort = ReasoningEffort.HIGH)
        )
        val json = JsonParser().parse(request).asJsonObject
        assertFalse(json.has("reasoning_effort"))
        assertFalse(json.has("reasoning"))
        assertFalse(json.has("enable_thinking"))
        assertTrue(json.has("temperature"))
        assertTrue(json.has("max_tokens"))
    }

    @Test
    fun `minimax think tag split fills reasoningContent and raw trace`() {
        val provider = TestMiniMaxProvider()
        val raw = "<think>inner plan</think>visible answer"
        val response = provider.parseResponse(
            """
            {
              "model": "MiniMax-M2.5",
              "choices": [{
                "message": {"role":"assistant","content":"$raw"},
                "finish_reason":"stop"
              }],
              "usage": {
                "prompt_tokens": 1,
                "completion_tokens": 2,
                "total_tokens": 3,
                "completion_tokens_details": { "reasoning_tokens": 7 }
              }
            }
            """.trimIndent()
        )

        assertEquals("visible answer", response.content)
        assertEquals("inner plan", response.reasoningContent)
        assertNotNull(response.reasoningTrace)
        assertEquals(raw, response.reasoningTrace!!.rawText)
        assertEquals(ReasoningReplayFormat.CONTENT_THINK_TAG, response.reasoningTrace!!.replayFormat)
        assertEquals("minimax", response.reasoningTrace!!.sourceProviderId)
        assertEquals(7, response.usage["reasoning_tokens"])
    }

    private class TestCustomOpenAIProvider : OpenAIProvider() {
        override fun getProviderInfo(): ProviderInfo = ProviderInfo(
            name = "openai_custom",
            displayName = "Custom",
            description = "Custom",
            defaultModelId = "any",
            defaultMultiModelId = null,
            isEnabled = true,
            tags = emptyList(),
            apiKeyPrefix = "",
            apiKeyValidateMsg = "",
            apiUrl = "https://example.com/v1",
            sortIndex = 1
        )

        suspend fun buildRequest(
            model: String,
            messages: List<ChatMessage>,
            temperature: Float,
            maxTokens: Int,
            stream: Boolean,
            tools: List<Any>?,
            reasoning: ReasoningOptions?
        ): String = buildChatRequest(model, messages, temperature, maxTokens, stream, tools, reasoning)
    }

    private class TestMiniMaxProvider : OpenAIProvider() {
        override fun getProviderInfo(): ProviderInfo = ProviderInfo(
            name = "minimax",
            displayName = "MiniMax",
            description = "MiniMax",
            defaultModelId = "MiniMax-M2.5",
            defaultMultiModelId = null,
            isEnabled = true,
            tags = emptyList(),
            apiKeyPrefix = "",
            apiKeyValidateMsg = "",
            apiUrl = "https://api.minimaxi.com/v1",
            sortIndex = 1
        )

        fun parseResponse(responseText: String) = parseChatResponse(responseText)
    }
}
