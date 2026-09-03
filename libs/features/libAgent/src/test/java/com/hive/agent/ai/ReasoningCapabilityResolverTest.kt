// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.agent.ai

import com.hive.agent.config.ReasoningRunPolicy
import com.google.gson.Gson
import com.hive.plugin.agent.ModelCapabilities
import com.hive.plugin.agent.ReasoningAvailability
import com.hive.plugin.agent.ReasoningCapabilities
import com.hive.plugin.agent.reasoningCapabilitiesOrUnknown
import com.hive.plugin.agent.model.ReasoningEffort
import com.hive.plugin.agent.model.ReasoningOptions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReasoningCapabilityResolverTest {

    @Test
    fun `public defaults and legacy null capability are safe`() {
        assertEquals(ReasoningEffort.MEDIUM, ReasoningOptions().effort)
        assertFalse(ReasoningOptions().enabled)
        assertEquals(ReasoningAvailability.UNKNOWN, null.reasoningCapabilitiesOrUnknown().availability)
        val legacy = Gson().fromJson("{}", ModelCapabilities::class.java)
        assertEquals(ReasoningAvailability.UNKNOWN, legacy.reasoningCapabilitiesOrUnknown().availability)
        assertNull(ReasoningCapabilityResolver.resolve("openrouter", "openai/gpt-5", null, ReasoningRunPolicy(true, ReasoningEffort.HIGH)).effectiveOptions)
    }

    @Test
    fun `catalog applies only approved provider gates`() {
        fun availability(provider: String, model: String) =
            ReasoningModelCatalog.capabilityFor(provider, model).availability

        assertEquals(ReasoningAvailability.UNSUPPORTED, availability("openai", "gpt-4.1-mini"))
        assertEquals(ReasoningAvailability.OPTIONAL, availability("openai", "gpt-5-mini"))
        assertEquals(ReasoningAvailability.REQUIRED, availability("openai", "o3-mini"))
        assertEquals(ReasoningAvailability.OPTIONAL, availability("kimi", "kimi-k2.6"))
        assertEquals(ReasoningAvailability.REQUIRED, availability("bailian", "qwen3-32b-thinking"))
        assertEquals(ReasoningAvailability.OPTIONAL, availability("bailian", "qwen3.5-plus"))
        assertEquals(ReasoningAvailability.REQUIRED, availability("siliconflow", "deepseek-ai/DeepSeek-R1"))
        assertEquals(ReasoningAvailability.UNKNOWN, availability("siliconflow", "deepseek-ai/DeepSeek-V3"))
        assertEquals(ReasoningAvailability.REQUIRED, availability("minimax", "MiniMax-M2.5"))
        assertEquals(ReasoningAvailability.REQUIRED, availability("stepfun", "step-3.7-flash"))
        assertEquals(ReasoningAvailability.UNKNOWN, availability("mimo", "mimo-v2.5-pro"))
        assertEquals(ReasoningAvailability.UNKNOWN, availability("ark", "deepseek-r1"))
    }

    @Test
    fun `run policy is an immutable snapshot`() {
        val original = ReasoningOptions(enabled = false, effort = ReasoningEffort.LOW)
        val snapshot = ReasoningRunPolicy.from(original)

        assertFalse(snapshot.enabled)
        assertEquals(ReasoningEffort.LOW, snapshot.effort)
        assertEquals(ReasoningEffort.MEDIUM, ReasoningRunPolicy(false).effort)
        assertTrue(ReasoningRunPolicy.from(original.copy(enabled = true)).enabled)
        assertFalse(snapshot.enabled)
    }

    @Test
    fun `resolver handles all availability states`() {
        val disabled = ReasoningRunPolicy(enabled = false, effort = ReasoningEffort.HIGH)

        val unsupported = ReasoningCapabilityResolver.resolve("openai", "gpt-4o", null, disabled)
        val unknown = ReasoningCapabilityResolver.resolve("unknown", "model", null, disabled)
        val optional = ReasoningCapabilityResolver.resolve("deepseek", "deepseek-chat", null, disabled)
        val required = ReasoningCapabilityResolver.resolve("deepseek", "deepseek-reasoner", null, disabled)

        assertEquals(ReasoningAvailability.UNSUPPORTED, unsupported.capabilities.availability)
        assertEquals(ReasoningAvailability.UNKNOWN, unknown.capabilities.availability)
        assertEquals(ReasoningAvailability.OPTIONAL, optional.capabilities.availability)
        assertEquals(ReasoningAvailability.REQUIRED, required.capabilities.availability)
        assertNull(unsupported.effectiveOptions)
        assertNull(unknown.effectiveOptions)
        assertNull(optional.effectiveOptions)
        assertTrue(required.effectiveOptions!!.enabled)
        assertFalse(disabled.enabled)
    }

    @Test
    fun `dynamic metadata overrides static capability and requested effort falls back deterministically`() {
        val dynamic = DynamicReasoningMetadata(
            capabilities = ReasoningCapabilities(
                availability = ReasoningAvailability.OPTIONAL,
                supportedEfforts = setOf(ReasoningEffort.LOW, ReasoningEffort.MEDIUM),
                defaultEffort = ReasoningEffort.LOW
            )
        )

        val metadataResult = ReasoningCapabilityResolver.resolve(
            providerId = "deepseek",
            modelId = "deepseek-reasoner",
            dynamicMetadata = dynamic,
            requestedPolicy = ReasoningRunPolicy(true, ReasoningEffort.HIGH)
        )
        assertEquals(ReasoningAvailability.OPTIONAL, metadataResult.capabilities.availability)
        assertEquals(ReasoningEffort.LOW, metadataResult.effectiveOptions!!.effort)

        val defaultResult = ReasoningCapabilityResolver.resolve(
            "openai", "gpt-5", null, ReasoningRunPolicy(true, ReasoningEffort.HIGH)
        )
        assertEquals(ReasoningEffort.HIGH, defaultResult.effectiveOptions!!.effort)

        val orderedFallback = ReasoningCapabilityResolver.resolve(
            providerId = "custom_openai",
            modelId = "declared-model",
            dynamicMetadata = DynamicReasoningMetadata(
                capabilities = ReasoningCapabilities(
                    availability = ReasoningAvailability.OPTIONAL,
                    supportedEfforts = setOf(ReasoningEffort.HIGH)
                )
            ),
            requestedPolicy = ReasoningRunPolicy(true, ReasoningEffort.LOW)
        )
        assertEquals(ReasoningEffort.HIGH, orderedFallback.effectiveOptions!!.effort)
    }

    @Test
    fun `resolver returns provider wire and replay policy`() {
        val resolved = ReasoningCapabilityResolver.resolve(
            "deepseek", "deepseek-reasoner", null, ReasoningRunPolicy(false)
        )

        assertEquals(ReasoningWireDialect.DEEPSEEK, resolved.wireDialect)
        assertEquals(com.hive.plugin.agent.model.ReasoningReplayFormat.REASONING_CONTENT, resolved.replayFormat)
    }

    @Test
    fun `custom and dynamic router model ids are never inferred`() {
        assertEquals(
            ReasoningAvailability.UNKNOWN,
            ReasoningCapabilityResolver.resolve("custom_openai", "gpt-5-thinking", null, ReasoningRunPolicy(true)).capabilities.availability
        )
        assertEquals(
            ReasoningAvailability.UNKNOWN,
            ReasoningCapabilityResolver.resolve("openrouter", "openai/o3", null, ReasoningRunPolicy(true)).capabilities.availability
        )
    }
}
