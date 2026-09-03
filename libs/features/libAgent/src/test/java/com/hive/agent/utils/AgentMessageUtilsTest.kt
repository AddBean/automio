// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.agent.utils

import com.google.gson.JsonObject
import com.hive.plugin.agent.model.ChatMessage
import com.hive.plugin.agent.model.FunctionCall
import com.hive.plugin.agent.model.MessageRole
import com.hive.plugin.agent.model.ReasoningReplayFormat
import com.hive.plugin.agent.model.ReasoningTrace
import com.hive.plugin.agent.model.ToolCall
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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

    @Test
    fun `strips all image attachments when allowImageAttachments is false`() = runBlocking {
        val withImage = ChatMessage(
            role = MessageRole.USER,
            content = "see",
            attachments = mutableListOf(
                com.hive.plugin.agent.model.ChatAttachment(
                    type = com.hive.plugin.agent.model.AttachmentType.IMAGE,
                    url = "https://example.com/a.png"
                )
            )
        )
        val messages = listOf(
            ChatMessage(MessageRole.SYSTEM, "sys"),
            withImage
        )

        val result = AgentMessageUtils.processAndCopyMessages(
            "task",
            messages,
            allowImageAttachments = false
        )

        assertTrue(result.all { it.attachments.isEmpty() })
    }

    @Test
    fun `simplifyTextMessages does not invent dash reasoning and nulls old assistant reasoning`() =
        runBlocking {
            val oldTs = 1_000L
            val recentTs = 2_000L
            val older = ChatMessage(
                role = MessageRole.ASSISTANT,
                content = "old answer",
                reasoningContent = "old thought",
                reasoningTrace = ReasoningTrace(
                    rawText = "old thought",
                    sourceProviderId = "deepseek",
                    sourceModelId = "deepseek-reasoner",
                    replayFormat = ReasoningReplayFormat.REASONING_CONTENT
                ),
                timestamp = oldTs
            )
            val recent = ChatMessage(
                role = MessageRole.ASSISTANT,
                content = "new answer",
                reasoningContent = "new thought",
                reasoningTrace = ReasoningTrace(
                    rawText = "new thought",
                    sourceProviderId = "deepseek",
                    sourceModelId = "deepseek-reasoner",
                    replayFormat = ReasoningReplayFormat.REASONING_CONTENT
                ),
                timestamp = recentTs
            )
            // Pad with many assistant messages so older falls outside MAX_NOT_SIMPLIFY_COUNT window.
            val padding = (3..10).map { i ->
                ChatMessage(
                    role = MessageRole.ASSISTANT,
                    content = "pad-$i",
                    reasoningContent = "pad-thought-$i",
                    reasoningTrace = ReasoningTrace(
                        rawText = "pad-thought-$i",
                        sourceProviderId = "deepseek",
                        sourceModelId = "deepseek-reasoner",
                        replayFormat = ReasoningReplayFormat.REASONING_CONTENT
                    ),
                    timestamp = oldTs + i
                )
            }
            val messages = listOf(
                ChatMessage(MessageRole.SYSTEM, "sys", timestamp = 0L),
                ChatMessage(MessageRole.USER, "hi", timestamp = 1L),
                older
            ) + padding + listOf(recent)

            val result = AgentMessageUtils.processAndCopyMessages("task", messages)

            assertTrue(result.none { it.reasoningContent == "-" })
            val olderOut = result.first { it.content == "old answer" }
            assertNull(olderOut.reasoningContent)
            assertNull(olderOut.reasoningTrace)

            val recentOut = result.first { it.content == "new answer" }
            assertEquals("new thought", recentOut.reasoningContent)
            assertNotNull(recentOut.reasoningTrace)
        }

    @Test
    fun `simplify keeps reasoning on older tool_call assistants outside plain window`() =
        runBlocking {
            val call = ToolCall(id = "call_old", function = FunctionCall("tool_x", JsonObject()))
            val toolAssistant = ChatMessage(
                role = MessageRole.ASSISTANT,
                content = null,
                toolCalls = listOf(call),
                reasoningContent = "need-replay",
                reasoningTrace = ReasoningTrace(
                    rawText = "need-replay",
                    sourceProviderId = "deepseek",
                    sourceModelId = "deepseek-reasoner",
                    replayFormat = ReasoningReplayFormat.REASONING_CONTENT
                ),
                timestamp = 100L
            )
            val toolResult = ChatMessage(
                role = MessageRole.TOOL,
                content = "ok",
                toolCallId = call.id,
                toolCallResult = "ok",
                timestamp = 101L
            )
            val plainPadding = (1..5).map { i ->
                ChatMessage(
                    role = MessageRole.ASSISTANT,
                    content = "plain-$i",
                    reasoningContent = "plain-thought-$i",
                    reasoningTrace = ReasoningTrace(
                        rawText = "plain-thought-$i",
                        sourceProviderId = "deepseek",
                        sourceModelId = "deepseek-reasoner",
                        replayFormat = ReasoningReplayFormat.REASONING_CONTENT
                    ),
                    timestamp = 200L + i
                )
            }
            val messages = listOf(
                ChatMessage(MessageRole.SYSTEM, "sys", timestamp = 0L),
                ChatMessage(MessageRole.USER, "hi", timestamp = 1L),
                toolAssistant,
                toolResult
            ) + plainPadding

            val result = AgentMessageUtils.processAndCopyMessages("task", messages)
            val kept = result.first { it.toolCalls?.any { tc -> tc.id == call.id } == true }
            assertEquals("need-replay", kept.reasoningContent)
            assertNotNull(kept.reasoningTrace)
        }

    @Test
    fun `history trim keeps assistant tool_calls with their tool results together`() {
        // Window would start on a TOOL if we naively takeLast(3); helper must snap back to assistant.
        val callA = ToolCall(id = "call_a", function = FunctionCall("tool_a", JsonObject()))
        val callB = ToolCall(id = "call_b", function = FunctionCall("tool_b", JsonObject()))
        val callC = ToolCall(id = "call_c", function = FunctionCall("tool_c", JsonObject()))
        val assistantToolMsgs = listOf(
            ChatMessage(MessageRole.ASSISTANT, null, toolCalls = listOf(callA), timestamp = 1L),
            ChatMessage(
                MessageRole.TOOL, "a", toolCallId = callA.id, toolCallResult = "a", timestamp = 2L
            ),
            ChatMessage(MessageRole.ASSISTANT, null, toolCalls = listOf(callB), timestamp = 3L),
            ChatMessage(
                MessageRole.TOOL, "b", toolCallId = callB.id, toolCallResult = "b", timestamp = 4L
            ),
            ChatMessage(MessageRole.ASSISTANT, null, toolCalls = listOf(callC), timestamp = 5L),
            ChatMessage(
                MessageRole.TOOL, "c", toolCallId = callC.id, toolCallResult = "c", timestamp = 6L
            )
        )

        val kept = AgentMessageUtils.takeLatestAssistantToolMessages(assistantToolMsgs, maxCount = 3)
        // takeLast(3) would be [tool_b, assistant_c, tool_c]; snap includes assistant_b.
        assertEquals(
            listOf(callB.id, callC.id),
            kept.filter { it.role == MessageRole.ASSISTANT }.map { it.toolCalls!!.single().id }
        )
        assertEquals(
            listOf(callB.id, callC.id),
            kept.filter { it.role == MessageRole.TOOL }.map { it.toolCallId }
        )
        // No orphan tool at the front
        assertTrue(kept.first().role == MessageRole.ASSISTANT)
    }
}
