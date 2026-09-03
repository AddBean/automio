// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.agent.ai

import com.hive.plugin.agent.ReasoningAvailability
import com.hive.plugin.agent.ReasoningCapabilities
import com.hive.plugin.agent.model.ReasoningEffort
import com.hive.plugin.agent.model.ReasoningOptions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReasoningPrivacyTest {

    @Test
    fun `skill summary and tool reason exclude reasoning content`() {
        val reasoning = "secret internal chain of thought"
        val finalContent = "最终答复"

        val publicText = ReasoningPrivacy.publicAssistantText(
            content = finalContent,
            reasoningContent = reasoning
        )
        assertEquals(finalContent, publicText)
        assertFalse(publicText.contains(reasoning))

        val emptyPublic = ReasoningPrivacy.publicAssistantText(
            content = null,
            reasoningContent = reasoning
        )
        assertEquals("思考完成", emptyPublic)
        assertFalse(emptyPublic.contains(reasoning))

        val blankPublic = ReasoningPrivacy.publicAssistantText(
            content = "   ",
            reasoningContent = reasoning
        )
        assertEquals("思考完成", blankPublic)

        val toolReason = ReasoningPrivacy.toolReasonLog(publicText)
        assertFalse(toolReason.contains(reasoning))
        assertEquals(finalContent, toolReason)
    }

    @Test
    fun `safe meta log records capability flags without reasoning body`() {
        val resolved = ResolvedReasoning(
            capabilities = ReasoningCapabilities(
                availability = ReasoningAvailability.OPTIONAL,
                supportedEfforts = setOf(ReasoningEffort.MEDIUM),
                defaultEffort = ReasoningEffort.MEDIUM
            ),
            effectiveOptions = ReasoningOptions(enabled = true, effort = ReasoningEffort.HIGH),
            wireDialect = ReasoningWireDialect.DEEPSEEK,
            replayFormat = com.hive.plugin.agent.model.ReasoningReplayFormat.REASONING_CONTENT
        )
        val meta = ReasoningPrivacy.safeMetaLog(
            providerId = "deepseek",
            modelId = "deepseek-chat",
            resolved = resolved
        )
        assertTrue(meta.contains("provider=deepseek"))
        assertTrue(meta.contains("model=deepseek-chat"))
        assertTrue(meta.contains("availability=OPTIONAL"))
        assertTrue(meta.contains("enabled=true"))
        assertTrue(meta.contains("effort=HIGH"))
        assertFalse(meta.contains("secret"))
        assertFalse(meta.contains("chain of thought"))
    }
}
