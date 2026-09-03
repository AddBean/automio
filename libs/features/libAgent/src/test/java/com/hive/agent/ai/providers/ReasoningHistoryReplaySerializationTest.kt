// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.agent.ai.providers

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.hive.plugin.agent.ProviderInfo
import com.hive.plugin.agent.model.ChatMessage
import com.hive.plugin.agent.model.FunctionCall
import com.hive.plugin.agent.model.MessageRole
import com.hive.plugin.agent.model.ReasoningDetail
import com.hive.plugin.agent.model.ReasoningReplayFormat
import com.hive.plugin.agent.model.ReasoningTrace
import com.hive.plugin.agent.model.ToolCall
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReasoningHistoryReplaySerializationTest {

    private val toolCall = ToolCall(
        id = "call_1",
        function = FunctionCall("demo_tool", JsonObject())
    )

    @Test
    fun `deepseek same provider model keeps reasoning_content on tool-turn assistant`() = runBlocking {
        val request = TestDeepSeek().buildRequest(
            model = "deepseek-reasoner",
            messages = multiTurnWithReasoning(
                provider = "deepseek",
                model = "deepseek-reasoner",
                format = ReasoningReplayFormat.REASONING_CONTENT,
                reasoning = "plan A"
            )
        )
        val assistant = JsonParser().parse(request).asJsonObject["messages"].asJsonArray[0].asJsonObject
        assertEquals("plan A", assistant["reasoning_content"].asString)
        assertTrue(assistant.has("tool_calls"))
    }

    @Test
    fun `deepseek mismatched model does not serialize reasoning_content`() = runBlocking {
        val request = TestDeepSeek().buildRequest(
            model = "deepseek-chat",
            messages = multiTurnWithReasoning(
                provider = "deepseek",
                model = "deepseek-reasoner",
                format = ReasoningReplayFormat.REASONING_CONTENT,
                reasoning = "plan A"
            )
        )
        val assistant = JsonParser().parse(request).asJsonObject["messages"].asJsonArray[0].asJsonObject
        assertFalse(assistant.has("reasoning_content") && !assistant["reasoning_content"].isJsonNull &&
            assistant["reasoning_content"].asString.isNotEmpty())
        // Prefer: field absent or null / empty when mismatched
        val hasNonEmpty = assistant.has("reasoning_content") &&
            !assistant["reasoning_content"].isJsonNull &&
            assistant["reasoning_content"].asString.isNotEmpty()
        assertFalse(hasNonEmpty)
    }

    @Test
    fun `kimi same provider model keeps reasoning_content`() = runBlocking {
        val request = TestKimi().buildRequest(
            model = "kimi-k2.5",
            messages = multiTurnWithReasoning(
                provider = "kimi",
                model = "kimi-k2.5",
                format = ReasoningReplayFormat.REASONING_CONTENT,
                reasoning = "kimi plan"
            )
        )
        val assistant = JsonParser().parse(request).asJsonObject["messages"].asJsonArray[0].asJsonObject
        assertEquals("kimi plan", assistant["reasoning_content"].asString)
    }

    @Test
    fun `openrouter same provider model keeps reasoning_details`() = runBlocking {
        val request = TestOpenRouter().buildRequest(
            model = "openai/o3",
            messages = listOf(
                ChatMessage(
                    role = MessageRole.ASSISTANT,
                    content = "call",
                    reasoningContent = "step1",
                    toolCalls = listOf(toolCall),
                    reasoningTrace = ReasoningTrace(
                        rawText = "step1",
                        details = listOf(
                            ReasoningDetail(type = "reasoning.summary", text = "step1", id = "1"),
                            ReasoningDetail(
                                type = "reasoning.encrypted",
                                id = "2",
                                data = mapOf("data" to "ENC")
                            )
                        ),
                        sourceProviderId = "openrouter",
                        sourceModelId = "openai/o3",
                        replayFormat = ReasoningReplayFormat.REASONING_DETAILS
                    )
                ),
                ChatMessage(
                    role = MessageRole.TOOL,
                    content = "out",
                    toolCallId = toolCall.id,
                    toolCallResult = "out"
                )
            )
        )
        val assistant = JsonParser().parse(request).asJsonObject["messages"].asJsonArray[0].asJsonObject
        assertTrue(assistant.has("reasoning_details"))
        assertEquals(2, assistant["reasoning_details"].asJsonArray.size())
    }

    @Test
    fun `openrouter mismatched provider does not serialize reasoning_details`() = runBlocking {
        val request = TestOpenRouter().buildRequest(
            model = "openai/o3",
            messages = listOf(
                ChatMessage(
                    role = MessageRole.ASSISTANT,
                    content = "call",
                    reasoningContent = "step1",
                    toolCalls = listOf(toolCall),
                    reasoningTrace = ReasoningTrace(
                        rawText = "step1",
                        details = listOf(
                            ReasoningDetail(type = "reasoning.summary", text = "step1", id = "1")
                        ),
                        sourceProviderId = "deepseek",
                        sourceModelId = "deepseek-reasoner",
                        replayFormat = ReasoningReplayFormat.REASONING_DETAILS
                    )
                ),
                ChatMessage(
                    role = MessageRole.TOOL,
                    content = "out",
                    toolCallId = toolCall.id,
                    toolCallResult = "out"
                )
            )
        )
        val assistant = JsonParser().parse(request).asJsonObject["messages"].asJsonArray[0].asJsonObject
        assertFalse(assistant.has("reasoning_details"))
    }

    @Test
    fun `minimax rebuilds think tag content for matching tool-turn replay`() = runBlocking {
        val request = TestMiniMax().buildRequest(
            model = "MiniMax-M2.5",
            messages = listOf(
                ChatMessage(
                    role = MessageRole.ASSISTANT,
                    content = "visible",
                    reasoningContent = "inner plan",
                    toolCalls = listOf(toolCall),
                    reasoningTrace = ReasoningTrace(
                        rawText = "<think>inner plan</think>visible",
                        sourceProviderId = "minimax",
                        sourceModelId = "MiniMax-M2.5",
                        replayFormat = ReasoningReplayFormat.CONTENT_THINK_TAG
                    )
                ),
                ChatMessage(
                    role = MessageRole.TOOL,
                    content = "out",
                    toolCallId = toolCall.id,
                    toolCallResult = "out"
                )
            )
        )
        val assistant = JsonParser().parse(request).asJsonObject["messages"].asJsonArray[0].asJsonObject
        val content = assistant["content"]
        val contentText = when {
            content.isJsonPrimitive -> content.asString
            content.isJsonArray -> content.asJsonArray.joinToString("") {
                it.asJsonObject.get("text")?.asString.orEmpty()
            }
            else -> content.toString()
        }
        assertTrue(contentText.contains("<think>inner plan</think>"))
        assertTrue(contentText.contains("visible"))
    }

    private fun multiTurnWithReasoning(
        provider: String,
        model: String,
        format: ReasoningReplayFormat,
        reasoning: String
    ) = listOf(
        ChatMessage(
            role = MessageRole.ASSISTANT,
            content = null,
            reasoningContent = reasoning,
            toolCalls = listOf(toolCall),
            reasoningTrace = ReasoningTrace(
                rawText = reasoning,
                sourceProviderId = provider,
                sourceModelId = model,
                replayFormat = format
            )
        ),
        ChatMessage(
            role = MessageRole.TOOL,
            content = "tool output",
            toolCallId = toolCall.id,
            toolCallResult = "tool output"
        )
    )

    private class TestDeepSeek : DeepSeekProvider() {
        override fun getProviderInfo(): ProviderInfo = ProviderInfo(
            name = "deepseek",
            displayName = "DeepSeek",
            description = "DeepSeek",
            defaultModelId = "deepseek-reasoner",
            defaultMultiModelId = null,
            isEnabled = true,
            tags = emptyList(),
            apiKeyPrefix = "",
            apiKeyValidateMsg = "",
            apiUrl = "https://api.deepseek.com",
            sortIndex = 1
        )

        suspend fun buildRequest(
            model: String,
            messages: List<ChatMessage>
        ): String = buildChatRequest(model, messages, 0.3f, 100, false, null, null)
    }

    private class TestKimi : OpenAIProvider() {
        override fun getProviderInfo(): ProviderInfo = ProviderInfo(
            name = "kimi",
            displayName = "Kimi",
            description = "Kimi",
            defaultModelId = "kimi-k2.5",
            defaultMultiModelId = null,
            isEnabled = true,
            tags = emptyList(),
            apiKeyPrefix = "",
            apiKeyValidateMsg = "",
            apiUrl = "https://api.moonshot.cn/v1",
            sortIndex = 1
        )

        suspend fun buildRequest(
            model: String,
            messages: List<ChatMessage>
        ): String = buildChatRequest(model, messages, 0.7f, 100, false, null, null)
    }

    private class TestMiniMax : OpenAIProvider() {
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

        suspend fun buildRequest(
            model: String,
            messages: List<ChatMessage>
        ): String = buildChatRequest(model, messages, 0.7f, 100, false, null, null)
    }

    private class TestOpenRouter : OpenRouterProvider() {
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
            apiUrl = "https://openrouter.ai/api/v1",
            sortIndex = 1
        )

        suspend fun buildRequest(
            model: String,
            messages: List<ChatMessage>
        ): String = buildChatRequest(model, messages, 0.7f, 100, false, null, null)
    }
}
