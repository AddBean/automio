// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.agent.views.chat

import com.hive.plugin.agent.model.ChatMessage
import com.hive.plugin.agent.model.MessageRole
import com.hive.plugin.agent.model.MessageStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AgentChatDisplayHelperTest {

    @Test
    fun `empty WAITING assistant is omitted but thinking footer is added`() {
        val messages = listOf(
            ChatMessage(MessageRole.USER, "hi"),
            ChatMessage(MessageRole.ASSISTANT, "", status = MessageStatus.WAITING)
        )
        val display = AgentChatDisplayHelper.buildDisplayData(messages, isCompressingMemory = false)
        assertEquals(2, display.size)
        assertEquals(AgentChatDisplayHelper.TYPE_USER_MESSAGE, display[0].first)
        assertEquals(AgentChatDisplayHelper.TYPE_THINKING_LOADING, display[1].first)
    }

    @Test
    fun `WAITING assistant with content shows bubble and thinking footer`() {
        val messages = listOf(
            ChatMessage(MessageRole.USER, "hi"),
            ChatMessage(MessageRole.ASSISTANT, "partial", status = MessageStatus.WAITING)
        )
        val display = AgentChatDisplayHelper.buildDisplayData(messages, isCompressingMemory = false)
        assertEquals(3, display.size)
        assertEquals(AgentChatDisplayHelper.TYPE_ASSISTANT_MESSAGE, display[1].first)
        assertEquals(AgentChatDisplayHelper.TYPE_THINKING_LOADING, display[2].first)
    }

    @Test
    fun `FINISH assistant has no thinking footer`() {
        val messages = listOf(
            ChatMessage(MessageRole.USER, "hi"),
            ChatMessage(MessageRole.ASSISTANT, "done", status = MessageStatus.FINISH)
        )
        val display = AgentChatDisplayHelper.buildDisplayData(messages, isCompressingMemory = false)
        assertEquals(2, display.size)
        assertFalse(display.any { it.first == AgentChatDisplayHelper.TYPE_THINKING_LOADING })
    }

    @Test
    fun `compressing memory takes priority over thinking footer`() {
        val messages = listOf(
            ChatMessage(MessageRole.ASSISTANT, "", status = MessageStatus.WAITING)
        )
        val display = AgentChatDisplayHelper.buildDisplayData(messages, isCompressingMemory = true)
        assertEquals(1, display.size)
        assertEquals(AgentChatDisplayHelper.TYPE_COMPRESSING_MEMORY, display[0].first)
    }

    @Test
    fun `system messages are filtered`() {
        val messages = listOf(
            ChatMessage(MessageRole.SYSTEM, "sys"),
            ChatMessage(MessageRole.USER, "hi")
        )
        val display = AgentChatDisplayHelper.buildDisplayData(messages, isCompressingMemory = false)
        assertEquals(1, display.size)
        assertEquals(AgentChatDisplayHelper.TYPE_USER_MESSAGE, display[0].first)
    }

    @Test
    fun `tool message after TOOL_RUNNING has no thinking footer`() {
        val messages = listOf(
            ChatMessage(MessageRole.ASSISTANT, "call", status = MessageStatus.TOOL_RUNNING),
            ChatMessage(MessageRole.TOOL, "result", toolCallResult = "ok")
        )
        val display = AgentChatDisplayHelper.buildDisplayData(messages, isCompressingMemory = false)
        assertTrue(display.none { it.first == AgentChatDisplayHelper.TYPE_THINKING_LOADING })
        assertEquals(AgentChatDisplayHelper.TYPE_TOOL_MESSAGE, display.last().first)
    }
}
