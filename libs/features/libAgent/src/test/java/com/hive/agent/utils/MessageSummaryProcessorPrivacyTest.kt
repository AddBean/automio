// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.agent.utils

import com.hive.plugin.agent.model.ChatMessage
import com.hive.plugin.agent.model.MessageRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class MessageSummaryProcessorPrivacyTest {

    @Test
    fun `memory summary input never contains reasoningContent`() {
        val reasoningOnly = ChatMessage(
            role = MessageRole.ASSISTANT,
            content = null,
            reasoningContent = "secret chain of thought"
        )
        assertEquals("", MessageSummaryProcessor.textForMemorySummary(reasoningOnly))

        val blankContent = ChatMessage(
            role = MessageRole.ASSISTANT,
            content = "   ",
            reasoningContent = "secret chain of thought"
        )
        assertEquals("", MessageSummaryProcessor.textForMemorySummary(blankContent))

        val withFinal = ChatMessage(
            role = MessageRole.ASSISTANT,
            content = "最终答复",
            reasoningContent = "secret chain of thought"
        )
        val text = MessageSummaryProcessor.textForMemorySummary(withFinal)
        assertEquals("最终答复", text)
        assertFalse(text.contains("secret"))
        assertFalse(text.contains("chain of thought"))
    }
}
