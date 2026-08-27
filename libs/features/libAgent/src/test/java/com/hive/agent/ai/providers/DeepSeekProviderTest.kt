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

class DeepSeekProviderTest {

    private val provider = TestDeepSeekProvider()
    private val toolCall = ToolCall(
        id = "call_1",
        function = FunctionCall("demo_tool", JsonObject())
    )

    @Test
    fun `tool fields are role scoped and tool uses content not tool_result`() = runBlocking {
        val request = provider.buildRequest(
            model = "deepseek-chat",
            messages = listOf(
                ChatMessage(
                    role = MessageRole.ASSISTANT,
                    content = null,
                    toolCalls = listOf(toolCall)
                ),
                ChatMessage(
                    role = MessageRole.TOOL,
                    content = "",
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

        assertTrue(assistant.has("tool_calls"))
        assertFalse(assistant.has("tool_call_id"))

        assertEquals("tool", tool["role"].asString)
        assertEquals(toolCall.id, tool["tool_call_id"].asString)
        assertEquals("tool output", tool["content"].asString)
        assertFalse(tool.has("tool_calls"))
        assertFalse(tool.has("tool_result"))
    }

    private class TestDeepSeekProvider : DeepSeekProvider() {
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
