// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.agent.ai

import com.hive.agent.config.ReasoningRunPolicy
import com.hive.plugin.agent.ModelCapabilities
import com.hive.plugin.agent.ModelInfo
import com.hive.plugin.agent.ModelType
import com.hive.plugin.agent.ReasoningAvailability
import com.hive.plugin.agent.ReasoningCapabilities
import com.hive.plugin.agent.model.AIRequest
import com.hive.plugin.agent.model.AIRequestType
import com.hive.plugin.agent.model.AgentInput
import com.hive.plugin.agent.model.ReasoningEffort
import com.hive.plugin.agent.model.ReasoningOptions
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ReasoningRunContextTest {

    @Before
    fun setUp() {
        ReasoningRunContexts.clear()
    }

    @After
    fun tearDown() {
        ReasoningRunContexts.clear()
    }

    @Test
    fun `main agent context applies frozen policy to effective options`() {
        val ctx = ReasoningRunContext(ReasoningRunPolicy(true, ReasoningEffort.HIGH))
        val options = ctx.resolveOptions("deepseek", "deepseek-chat")
        assertEquals(ReasoningOptions(enabled = true, effort = ReasoningEffort.HIGH), options)
    }

    @Test
    fun `nested skill inherits parent task context`() {
        val parent = ReasoningRunContext(ReasoningRunPolicy(true, ReasoningEffort.LOW))
        ReasoningRunContexts.bind("root-task", parent)

        val nested = ReasoningRunContexts.resolveForSkill(
            rootTaskId = "root-task",
            isStandalone = false,
            createSnapshot = {
                ReasoningRunContext(ReasoningRunPolicy(false, ReasoningEffort.HIGH))
            }
        )
        assertSame(parent, nested.first)
        assertEquals(false, nested.second)
        assertEquals(ReasoningEffort.LOW, nested.first.policy.effort)
        assertTrue(nested.first.policy.enabled)
    }

    @Test
    fun `nested skill without parent snapshot does not bind`() {
        ReasoningRunContexts.clear()
        val nested = ReasoningRunContexts.resolveForSkill(
            rootTaskId = "orphan-root",
            isStandalone = false,
            createSnapshot = {
                ReasoningRunContext(ReasoningRunPolicy(true, ReasoningEffort.MEDIUM))
            }
        )
        assertTrue(nested.first.policy.enabled)
        assertEquals(false, nested.second)
        assertNull(ReasoningRunContexts.get("orphan-root"))
    }

    @Test
    fun `independent skill creates its own snapshot`() {
        val parent = ReasoningRunContext(ReasoningRunPolicy(true, ReasoningEffort.LOW))
        ReasoningRunContexts.bind("other-root", parent)

        val standalone = ReasoningRunContexts.resolveForSkill(
            rootTaskId = "skill-alone",
            isStandalone = true,
            createSnapshot = {
                ReasoningRunContext(ReasoningRunPolicy(false, ReasoningEffort.MEDIUM))
            }
        )
        assertEquals(false, standalone.first.policy.enabled)
        assertEquals(ReasoningEffort.MEDIUM, standalone.first.policy.effort)
        assertTrue(standalone.second)
        assertSame(standalone.first, ReasoningRunContexts.get("skill-alone"))
    }

    @Test
    fun `task snapshot is immutable against later policy values`() {
        val frozen = ReasoningRunPolicy(enabled = true, effort = ReasoningEffort.LOW)
        val ctx = ReasoningRunContext(frozen)

        val liveChanged = ReasoningRunPolicy(enabled = false, effort = ReasoningEffort.HIGH)
        assertTrue(ctx.policy.enabled)
        assertEquals(ReasoningEffort.LOW, ctx.policy.effort)
        assertEquals(false, liveChanged.enabled)

        val optionsAfterLiveChange = ctx.resolveOptions("deepseek", "deepseek-chat")
        assertEquals(true, optionsAfterLiveChange?.enabled)
        assertEquals(ReasoningEffort.LOW, optionsAfterLiveChange?.effort)
    }

    @Test
    fun `openrouter uses model capabilities reasoning as dynamic metadata`() {
        val caps = ReasoningCapabilities(
            availability = ReasoningAvailability.OPTIONAL,
            supportedEfforts = setOf(ReasoningEffort.LOW, ReasoningEffort.HIGH),
            defaultEffort = ReasoningEffort.LOW
        )
        val model = ModelInfo(
            modelId = "openai/gpt-5",
            displayName = "gpt-5",
            providerId = "openrouter",
            buildIn = false,
            capabilities = ModelCapabilities(
                supportsFunctionCall = true,
                supportsVision = false,
                contextWindow = 128000,
                modelType = ModelType.LLM,
                reasoning = caps
            )
        )
        val ctx = ReasoningRunContext(ReasoningRunPolicy(true, ReasoningEffort.HIGH))
        val options = ReasoningRequestFactory.optionsFor(ctx, model.providerId, model)
        assertNotNull(options)
        assertEquals(true, options!!.enabled)
        assertEquals(ReasoningEffort.HIGH, options.effort)

        // Without dynamic metadata OpenRouter catalog is UNKNOWN → null
        assertNull(ctx.resolveOptions("openrouter", "openai/gpt-5", null))
    }

    @Test
    fun `lightweight AIRequest constructions keep reasoning null`() {
        val input = AgentInput(emptyList())
        val summaryLike = AIRequest(
            model = "m",
            requestType = AIRequestType.CHAT_COMPLETION,
            input = input,
            inputOrigin = input
        )
        val scriptNameLike = AIRequest(
            model = "m",
            requestType = AIRequestType.CHAT_COMPLETION,
            input = input,
            inputOrigin = input
        )
        assertNull(summaryLike.reasoning)
        assertNull(scriptNameLike.reasoning)
        assertNull(ReasoningRequestFactory.lightweightOptions())
    }
}
