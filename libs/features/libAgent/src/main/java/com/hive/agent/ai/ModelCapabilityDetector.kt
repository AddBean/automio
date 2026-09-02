// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.agent.ai

import com.hive.plugin.agent.ModelCapabilities
import com.hive.plugin.agent.ModelType
import java.util.Locale

/**
 * Conservative capability inference for model APIs that only return an ID.
 * Provider-supplied metadata should always take precedence over these rules.
 */
object ModelCapabilityDetector {

    private val nonChatMarkers = listOf(
        "embedding", "embed-", "rerank", "moderation", "whisper", "tts", "image-generation"
    )

    private val visionMarkers = listOf(
        "vision", "-vl", "vl-", "llava", "bakllava", "moondream", "pixtral",
        "gpt-4o", "gpt-4.1", "gemini", "claude-3", "claude-sonnet", "claude-opus"
    )

    fun detect(modelId: String): ModelCapabilities {
        val normalized = modelId.lowercase(Locale.ROOT)
        val isChatModel = nonChatMarkers.none(normalized::contains)
        return ModelCapabilities(
            supportsFunctionCall = isChatModel,
            supportsVision = isChatModel && visionMarkers.any(normalized::contains),
            contextWindow = detectContextWindow(normalized),
            modelType = ModelType.CHAT
        )
    }

    private fun detectContextWindow(modelId: String): Int = when {
        modelId.contains("gemini") -> 1_000_000
        modelId.contains("claude") -> 200_000
        modelId.contains("gpt-4") || modelId.contains("deepseek") ||
            modelId.contains("qwen") || modelId.contains("llama-3") ||
            modelId.contains("llama3") -> 128_000
        else -> 0
    }
}
