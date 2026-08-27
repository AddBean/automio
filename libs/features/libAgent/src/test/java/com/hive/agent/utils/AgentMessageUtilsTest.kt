// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.agent.utils

import com.google.gson.JsonObject
import com.hive.plugin.agent.model.ChatMessage
import com.hive.plugin.agent.model.FunctionCall
import com.hive.plugin.agent.model.MessageRole
import com.hive.plugin.agent.model.ToolCall
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AgentMessageUtilsTest {

    private val call1 = ToolCall(
        id = "call_SUMbt87aqSntyNkoBvNyYSSi",
        function = FunctionCall("tool_a", JsonObject())
    )
    private val call2 = ToolCall(
        id = "call_ELhqEazpiOmHCdCec0Z12bXC",
        function = FunctionCall("tool_b", JsonObject())
    )

    @Test
    fun `strips tool_calls when non-tool message appears before tool responses`() = runBlocking {
        val messages = listOf(
            ChatMessage(MessageRole.SYSTEM, "sys"),
            ChatMessage(MessageRole.USER, "hi"),
            ChatMessage(MessageRole.ASSISTANT, "call tools", toolCalls = listOf(call1, call2)),
            ChatMessage(MessageRole.USER, "interleaved"),
            ChatMessage(
                MessageRole.TOOL,
                content = "r1",
                toolCallId = call1.id,
                toolCallResult = "r1"
            ),
            ChatMessage(
                MessageRole.TOOL,
                content = "r2",
                toolCallId = call2.id,
                toolCallResult = "r2"
            )
        )

        val result = AgentMessageUtils.processAndCopyMessages("task", messages)
        val assistant = result.first { it.role == MessageRole.ASSISTANT }

        assertNull(assistant.toolCalls)
        assertTrue(result.none { it.role == MessageRole.TOOL })
    }

    @Test
    fun `strips tool_calls when tool responses are incomplete`() = runBlocking {
        val messages = listOf(
            ChatMessage(MessageRole.SYSTEM, "sys"),
            ChatMessage(MessageRole.USER, "hi"),
            ChatMessage(MessageRole.ASSISTANT, "call tools", toolCalls = listOf(call1, call2)),
            ChatMessage(
                MessageRole.TOOL,
                content = "r1",
                toolCallId = call1.id,
                toolCallResult = "r1"
            )
        )

        val result = AgentMessageUtils.processAndCopyMessages("task", messages)
        val assistant = result.first { it.role == MessageRole.ASSISTANT }

        assertNull(assistant.toolCalls)
        assertTrue(result.none { it.role == MessageRole.TOOL })
    }

    @Test
    fun `keeps paired tool_calls and immediate tool responses`() = runBlocking {
        val messages = listOf(
            ChatMessage(MessageRole.SYSTEM, "sys"),
            ChatMessage(MessageRole.USER, "hi"),
            ChatMessage(MessageRole.ASSISTANT, "call tools", toolCalls = listOf(call1, call2)),
            ChatMessage(
                MessageRole.TOOL,
                content = "r1",
                toolCallId = call1.id,
                toolCallResult = "r1"
            ),
            ChatMessage(
                MessageRole.TOOL,
                content = "r2",
                toolCallId = call2.id,
                toolCallResult = "r2"
            ),
            ChatMessage(MessageRole.USER, "next")
        )

        val result = AgentMessageUtils.processAndCopyMessages("task", messages)
        val assistant = result.first { it.role == MessageRole.ASSISTANT && !it.toolCalls.isNullOrEmpty() }

        assertEquals(listOf(call1.id, call2.id), assistant.toolCalls!!.map { it.id })
        assertEquals(
            listOf(call1.id, call2.id),
            result.filter { it.role == MessageRole.TOOL }.map { it.toolCallId }
        )
    }
}
