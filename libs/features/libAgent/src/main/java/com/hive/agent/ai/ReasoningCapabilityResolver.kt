// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.agent.ai

import com.hive.agent.config.ReasoningRunPolicy
import com.hive.plugin.agent.ReasoningAvailability
import com.hive.plugin.agent.ReasoningCapabilities
import com.hive.plugin.agent.model.ReasoningEffort
import com.hive.plugin.agent.model.ReasoningOptions
import com.hive.plugin.agent.model.ReasoningReplayFormat
import java.util.Locale

/** Provider-side request shape for reasoning options. */
enum class ReasoningWireDialect {
    OPENAI,
    OPENROUTER,
    DEEPSEEK,
    BAILIAN,
    KIMI,
    SILICONFLOW,
    NONE
}

/**
 * Dynamically fetched capability metadata. Supplying this object is an explicit declaration and
 * therefore takes precedence over the intentionally conservative built-in catalog.
 */
data class DynamicReasoningMetadata(
    val capabilities: ReasoningCapabilities? = null,
    val wireDialect: ReasoningWireDialect? = null,
    val replayFormat: ReasoningReplayFormat? = null
)

/** Resolved capability and the request/replay behavior for one immutable task policy. */
data class ResolvedReasoning(
    val capabilities: ReasoningCapabilities,
    val effectiveOptions: ReasoningOptions?,
    val wireDialect: ReasoningWireDialect,
    val replayFormat: ReasoningReplayFormat
)

/**
 * Single source of truth for the initial verified model gates. Do not broaden these rules with
 * model-name guesses: providers that route arbitrary third-party models need dynamic metadata.
 */
object ReasoningModelCatalog {

    private val allEfforts = setOf(
        ReasoningEffort.LOW,
        ReasoningEffort.MEDIUM,
        ReasoningEffort.HIGH
    )

    private val siliconFlowOptionalModels = setOf(
        "qwen/qwen3-8b",
        "qwen/qwen3-14b",
        "qwen/qwen3-32b",
        "qwen/qwen3-30b-a3b",
        "qwen/qwen3-235b-a22b"
    )

    private val stepFunReasoningModels = setOf(
        "step-2-mini",
        "step-3.5-flash",
        "step-3.7-flash",
        "step-r1-v-mini"
    )

    private val bailianHybridPrefixes = setOf(
        "qwen3-0.6b",
        "qwen3-1.7b",
        "qwen3-4b",
        "qwen3-8b",
        "qwen3-14b",
        "qwen3-30b-a3b",
        "qwen3-32b",
        "qwen3-235b-a22b",
        "qwen3-max",
        "qwen3.5-plus",
        "qwen3.5-flash"
    )

    fun capabilityFor(providerId: String, modelId: String): ReasoningCapabilities {
        val provider = providerId.trim().lowercase(Locale.ROOT)
        val model = modelId.trim().lowercase(Locale.ROOT)
        return when (provider) {
            "openai" -> openAiCapability(model)
            "deepseek" -> when (model) {
                "deepseek-chat" -> optional()
                "deepseek-reasoner" -> required()
                "deepseek-coder" -> unknown()
                else -> unknown()
            }
            "kimi" -> when (model) {
                "kimi-k2.5", "kimi-k2.6" -> optional()
                else -> unknown()
            }
            "bailian", "bailian_code" -> bailianCapability(model)
            "siliconflow" -> siliconFlowCapability(model)
            "minimax" -> if (model.matches(Regex("minimax-m2(?:\\.[0-9]+)*"))) required() else unknown()
            "stepfun" -> if (model in stepFunReasoningModels) required() else unknown()
            // MiMo, Ark, custom OpenAI endpoints and routing providers remain metadata-only.
            "mimo", "ark", "ark_agent_plan", "ark_coding_plan", "openrouter",
            "custom_openai", "openai_custom" -> unknown()
            else -> unknown()
        }
    }

    fun wireDialectFor(providerId: String): ReasoningWireDialect = when (
        providerId.trim().lowercase(Locale.ROOT)
    ) {
        "openai" -> ReasoningWireDialect.OPENAI
        "openrouter" -> ReasoningWireDialect.OPENROUTER
        "deepseek" -> ReasoningWireDialect.DEEPSEEK
        "bailian", "bailian_code" -> ReasoningWireDialect.BAILIAN
        "kimi" -> ReasoningWireDialect.KIMI
        "siliconflow" -> ReasoningWireDialect.SILICONFLOW
        // MiniMax / StepFun auto-reason; do not send control params.
        else -> ReasoningWireDialect.NONE
    }

    fun replayFormatFor(providerId: String): ReasoningReplayFormat {
        val provider = providerId.trim().lowercase(Locale.ROOT)
        return when (provider) {
            "minimax" -> ReasoningReplayFormat.CONTENT_THINK_TAG
            "stepfun" -> ReasoningReplayFormat.REASONING_CONTENT
            else -> wireDialectFor(provider).defaultReplayFormat()
        }
    }

    private fun ReasoningWireDialect.defaultReplayFormat(): ReasoningReplayFormat = when (this) {
        ReasoningWireDialect.OPENROUTER -> ReasoningReplayFormat.REASONING_DETAILS
        ReasoningWireDialect.SILICONFLOW -> ReasoningReplayFormat.CONTENT_THINK_TAG
        ReasoningWireDialect.OPENAI,
        ReasoningWireDialect.DEEPSEEK,
        ReasoningWireDialect.BAILIAN,
        ReasoningWireDialect.KIMI -> ReasoningReplayFormat.REASONING_CONTENT
        ReasoningWireDialect.NONE -> ReasoningReplayFormat.NONE
    }

    private fun openAiCapability(model: String): ReasoningCapabilities = when {
        model.startsWith("gpt-4o") || model.startsWith("gpt-4.1") -> unsupported()
        model.matches(Regex("gpt-5(?:[.-].*)?")) -> optional()
        model.matches(Regex("o[1-9](?:[.-].*)?")) -> required()
        else -> unknown()
    }

    private fun bailianCapability(model: String): ReasoningCapabilities = when {
        model in bailianHybridPrefixes -> optional()
        bailianHybridPrefixes.any { prefix ->
            model == "$prefix-thinking" || model.startsWith("$prefix-thinking-")
        } -> required()
        else -> unknown()
    }

    private fun siliconFlowCapability(model: String): ReasoningCapabilities = when (model) {
        "deepseek-ai/deepseek-r1" -> required()
        in siliconFlowOptionalModels -> optional()
        else -> unknown()
    }

    private fun unknown() = ReasoningCapabilities()

    private fun unsupported() = ReasoningCapabilities(availability = ReasoningAvailability.UNSUPPORTED)

    private fun optional() = ReasoningCapabilities(
        availability = ReasoningAvailability.OPTIONAL,
        supportedEfforts = allEfforts,
        defaultEffort = ReasoningEffort.MEDIUM
    )

    private fun required() = ReasoningCapabilities(
        availability = ReasoningAvailability.REQUIRED,
        supportedEfforts = allEfforts,
        defaultEffort = ReasoningEffort.MEDIUM
    )
}

/** Resolves static/dynamic capability declarations without modifying persisted user settings. */
object ReasoningCapabilityResolver {

    fun resolve(
        providerId: String,
        modelId: String,
        dynamicMetadata: DynamicReasoningMetadata? = null,
        requestedPolicy: ReasoningRunPolicy
    ): ResolvedReasoning {
        val capability = dynamicMetadata?.capabilities
            ?: ReasoningModelCatalog.capabilityFor(providerId, modelId)
        val dialect = dynamicMetadata?.wireDialect
            ?: ReasoningModelCatalog.wireDialectFor(providerId)
        val replayFormat = dynamicMetadata?.replayFormat
            ?: ReasoningModelCatalog.replayFormatFor(providerId)

        val resolvedEffort = resolveEffort(requestedPolicy.effort, capability)
        val effectiveOptions = when (capability.availability) {
            ReasoningAvailability.OPTIONAL -> ReasoningOptions(
                enabled = requestedPolicy.enabled,
                effort = resolvedEffort
            )
            ReasoningAvailability.REQUIRED -> ReasoningOptions(
                enabled = true,
                effort = resolvedEffort
            )
            ReasoningAvailability.UNKNOWN,
            ReasoningAvailability.UNSUPPORTED -> null
        }

        return ResolvedReasoning(
            capabilities = capability,
            effectiveOptions = effectiveOptions,
            wireDialect = dialect,
            replayFormat = replayFormat
        )
    }

    private fun resolveEffort(
        requested: ReasoningEffort,
        capabilities: ReasoningCapabilities
    ): ReasoningEffort {
        val supported = capabilities.supportedEfforts
        if (requested in supported) return requested
        capabilities.defaultEffort?.let { return it }
        return listOf(ReasoningEffort.MEDIUM, ReasoningEffort.LOW, ReasoningEffort.HIGH)
            .firstOrNull { it in supported }
            ?: ReasoningEffort.MEDIUM
    }
}
