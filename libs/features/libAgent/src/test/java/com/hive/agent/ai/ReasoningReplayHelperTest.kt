// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.agent.ai

import com.hive.plugin.agent.model.ChatMessage
import com.hive.plugin.agent.model.MessageRole
import com.hive.plugin.agent.model.ReasoningDetail
import com.hive.plugin.agent.model.ReasoningReplayFormat
import com.hive.plugin.agent.model.ReasoningTrace
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReasoningReplayHelperTest {

    private fun assistant(
        content: String?,
        reasoning: String?,
        provider: String,
        model: String,
        format: ReasoningReplayFormat,
        rawText: String? = reasoning,
        details: List<ReasoningDetail> = emptyList()
    ) = ChatMessage(
        role = MessageRole.ASSISTANT,
        content = content,
        reasoningContent = reasoning,
        reasoningTrace = ReasoningTrace(
            rawText = rawText,
            details = details,
            sourceProviderId = provider,
            sourceModelId = model,
            replayFormat = format
        )
    )

    @Test
    fun `shouldReplay only when provider and model match`() {
        val msg = assistant("a", "think", "deepseek", "deepseek-reasoner", ReasoningReplayFormat.REASONING_CONTENT)
        assertTrue(ReasoningReplayHelper.shouldReplay(msg, "deepseek", "deepseek-reasoner"))
        assertFalse(ReasoningReplayHelper.shouldReplay(msg, "deepseek", "deepseek-chat"))
        assertFalse(ReasoningReplayHelper.shouldReplay(msg, "kimi", "deepseek-reasoner"))
        assertFalse(ReasoningReplayHelper.shouldReplay(msg, "openrouter", "openai/o3"))
    }

    @Test
    fun `mismatched provider returns null wire fields`() {
        val msg = assistant("a", "think", "deepseek", "deepseek-reasoner", ReasoningReplayFormat.REASONING_CONTENT)
        assertNull(ReasoningReplayHelper.reasoningContentForWire(msg, "kimi", "kimi-k2.5"))
        assertNull(ReasoningReplayHelper.reasoningDetailsForWire(msg, "openrouter", "openai/o3"))
        assertEquals("a", ReasoningReplayHelper.contentForWire(msg, "kimi", "kimi-k2.5"))
    }

    @Test
    fun `minimax rebuilds think tag from display split`() {
        val msg = assistant(
            content = "visible",
            reasoning = "plan",
            provider = "minimax",
            model = "MiniMax-M2.5",
            format = ReasoningReplayFormat.CONTENT_THINK_TAG,
            rawText = "<think>plan</think>visible"
        )
        assertEquals(
            "<think>plan</think>visible",
            ReasoningReplayHelper.contentForWire(msg, "minimax", "MiniMax-M2.5")
        )
    }

    @Test
    fun `minimax rebuilds think tag when rawText is display-only reasoning`() {
        val msg = assistant(
            content = "answer",
            reasoning = "inner",
            provider = "minimax",
            model = "MiniMax-M2.5",
            format = ReasoningReplayFormat.CONTENT_THINK_TAG,
            rawText = "inner"
        )
        assertEquals(
            "<think>inner</think>answer",
            ReasoningReplayHelper.contentForWire(msg, "minimax", "MiniMax-M2.5")
        )
    }

    @Test
    fun `openrouter details serialized when matching`() {
        val msg = assistant(
            content = "ok",
            reasoning = "step1",
            provider = "openrouter",
            model = "openai/o3",
            format = ReasoningReplayFormat.REASONING_DETAILS,
            details = listOf(
                ReasoningDetail(type = "reasoning.summary", text = "step1", id = "1"),
                ReasoningDetail(type = "reasoning.encrypted", id = "2", data = mapOf("data" to "SECRET"))
            )
        )
        val arr = ReasoningReplayHelper.reasoningDetailsForWire(msg, "openrouter", "openai/o3")
        assertEquals(2, arr!!.size())
        assertEquals("step1", arr[0].asJsonObject["text"].asString)
        assertEquals("SECRET", arr[1].asJsonObject["data"].asString)
        assertNull(ReasoningReplayHelper.reasoningDetailsForWire(msg, "openrouter", "other/model"))
    }
}
