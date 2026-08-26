// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.agent.ai.providers

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.hive.plugin.agent.model.ChatMessage
import com.hive.plugin.agent.model.FunctionCall
import com.hive.plugin.agent.model.MessageRole
import com.hive.plugin.agent.model.ToolCall
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OpenAIProviderTest {

    private val provider = TestOpenAIProvider()
    private val toolCall = ToolCall(
        id = "call_1",
        function = FunctionCall("demo_tool", JsonObject())
    )

    @Test
    fun `tool calls are only serialized on assistant messages`() = runBlocking {
        val request = provider.buildRequest(
            model = "custom-model",
            messages = listOf(
                ChatMessage(
                    role = MessageRole.ASSISTANT,
                    content = null,
                    toolCalls = listOf(toolCall)
                ),
                ChatMessage(
                    role = MessageRole.TOOL,
                    content = "tool output",
                    toolCallId = toolCall.id,
                    toolCalls = listOf(toolCall),
                    toolCallResult = "tool output"
                )
            ),
            temperature = 0.7f,
            maxTokens = 100,
            stream = false,
            tools = null
        )

        val messages = JsonParser().parse(request)
            .asJsonObject["messages"]
            .asJsonArray
        val assistant = messages[0].asJsonObject
        val tool = messages[1].asJsonObject

        assertEquals("assistant", assistant["role"].asString)
        assertTrue(assistant.has("tool_calls"))
        assertFalse(assistant.has("tool_call_id"))

        assertEquals("tool", tool["role"].asString)
        assertEquals(toolCall.id, tool["tool_call_id"].asString)
        assertFalse(tool.has("tool_calls"))
        assertFalse(tool.has("tool_result"))
    }

    @Test
    fun `non assistant messages never serialize tool calls`() = runBlocking {
        val request = provider.buildRequest(
            model = "custom-model",
            messages = listOf(
                ChatMessage(MessageRole.SYSTEM, "system", toolCalls = listOf(toolCall)),
                ChatMessage(MessageRole.USER, "user", toolCalls = listOf(toolCall))
            ),
            temperature = 0.7f,
            maxTokens = 100,
            stream = false,
            tools = null
        )

        val messages = JsonParser().parse(request)
            .asJsonObject["messages"]
            .asJsonArray

        messages.forEach { message ->
            assertFalse(message.asJsonObject.has("tool_calls"))
            assertFalse(message.asJsonObject.has("tool_call_id"))
        }
    }

    private class TestOpenAIProvider : OpenAIProvider() {
        suspend fun buildRequest(
            model: String,
            messages: List<ChatMessage>,
            temperature: Float,
            maxTokens: Int,
            stream: Boolean,
            tools: List<Any>?
        ): String = buildChatRequest(model, messages, temperature, maxTokens, stream, tools)
    }
}
