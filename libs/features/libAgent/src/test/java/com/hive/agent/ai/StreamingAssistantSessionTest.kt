// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.agent.ai

import com.hive.plugin.agent.model.ChatCompletionResponse
import com.hive.plugin.agent.model.ChatMessage
import com.hive.plugin.agent.model.MessageRole
import com.hive.plugin.agent.model.ReasoningReplayFormat
import com.hive.plugin.agent.model.ReasoningTrace
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class StreamingAssistantSessionTest {

    @Test
    fun `onChunk and finalizeWith persist reasoningTrace on assistant message`() {
        val messages = mutableListOf<ChatMessage>()
        val session = StreamingAssistantSession.start(
            messages = messages,
            normalNotify = {},
            streamNotify = {},
            throttleMs = 0L
        )

        val chunkTrace = ReasoningTrace(
            rawText = "partial",
            sourceProviderId = "deepseek",
            sourceModelId = "deepseek-reasoner",
            replayFormat = ReasoningReplayFormat.REASONING_CONTENT
        )
        session.onChunk(
            ChatCompletionResponse(
                content = "hi",
                reasoningContent = "partial",
                reasoningTrace = chunkTrace
            )
        )

        val afterChunk = session.message()
        assertEquals("partial", afterChunk.reasoningContent)
        assertNotNull(afterChunk.reasoningTrace)
        assertEquals("deepseek", afterChunk.reasoningTrace!!.sourceProviderId)
        assertEquals("deepseek-reasoner", afterChunk.reasoningTrace!!.sourceModelId)

        val finalTrace = ReasoningTrace(
            rawText = "full thought",
            sourceProviderId = "deepseek",
            sourceModelId = "deepseek-reasoner",
            replayFormat = ReasoningReplayFormat.REASONING_CONTENT
        )
        session.finalizeWith(
            ChatCompletionResponse(
                content = "done",
                reasoningContent = "full thought",
                reasoningTrace = finalTrace
            )
        )

        val finalized = session.message()
        assertEquals(MessageRole.ASSISTANT, finalized.role)
        assertEquals("done", finalized.content)
        assertEquals("full thought", finalized.reasoningContent)
        assertEquals(finalTrace, finalized.reasoningTrace)
        assertEquals(1, messages.size)
        assertEquals(finalTrace, messages[0].reasoningTrace)
    }

    @Test
    fun `finalizeWith clears reasoningTrace when response has none`() {
        val messages = mutableListOf<ChatMessage>()
        val session = StreamingAssistantSession.start(
            messages = messages,
            normalNotify = {},
            streamNotify = {},
            throttleMs = 0L
        )
        session.onChunk(
            ChatCompletionResponse(
                content = "x",
                reasoningContent = "t",
                reasoningTrace = ReasoningTrace(
                    rawText = "t",
                    sourceProviderId = "kimi",
                    sourceModelId = "kimi-k2.5",
                    replayFormat = ReasoningReplayFormat.REASONING_CONTENT
                )
            )
        )
        session.finalizeWith(
            ChatCompletionResponse(content = "y", reasoningContent = null, reasoningTrace = null)
        )
        assertNull(session.message().reasoningTrace)
    }
}
