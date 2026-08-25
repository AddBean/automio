// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.agent.ai.providers

import com.hive.agent.config.ConfigAgentModels
import com.hive.plugin.agent.ProviderInfo
import com.hive.plugin.agent.ModelCapabilities
import com.hive.plugin.agent.ModelInfo
import com.hive.plugin.agent.ModelType
import com.hive.plugin.agent.model.AIRequest
import com.hive.plugin.agent.model.AIResult
import com.hive.plugin.agent.model.AgentError
import com.hive.plugin.agent.model.AgentErrorCode
import com.hive.plugin.agent.model.ChatCompletionResponse
import com.hive.utils.extends.string

/**
 * Gemini Provider实现
 */
class GeminiProvider : AbstractBaseProvider() {

    override fun getProviderInfo(): ProviderInfo {
        return ConfigAgentModels.findProviderInfo("gemini") ?: ProviderInfo(
            name = "gemini",
            displayName = "Gemini",
            description = "Google Gemini",
            defaultModelId = "gemini-1.5-turbo",
            defaultMultiModelId = "gemini-1.5-turbo",
            isEnabled = false, // 这里不能调用isEnabled()，会造成循环调用
            tags = listOf("LLM"),
            apiKeyPrefix = "",
            apiKeyValidateMsg = com.hive.i8n.R.string.api_key_validation_gemini.string(),
            apiUrl = "https://generativelanguage.googleapis.com",
            sortIndex = 0
        )
    }

    override suspend fun <T> onInference(request: AIRequest): AIResult<T> {
        return AIResult.Failure(AgentError.create(code = AgentErrorCode.AI_SERVICE_UNAVAILABLE))
    }


    override suspend fun <T> onStreamInference(
        request: AIRequest,
        onChunkResponse: ((ChatCompletionResponse) -> Unit)?
    ): AIResult<T> {
        return AIResult.Failure(AgentError.create(code = AgentErrorCode.AI_SERVICE_UNAVAILABLE))
    }


    override suspend fun getBuildInModels(): List<ModelInfo> = listOf(
        ModelInfo(
            modelId = "gemini-pro",
            displayName = "Gemini Pro",
            providerId = "gemini",
            buildIn = true,
            capabilities = ModelCapabilities(
                supportsFunctionCall = true,
                supportsVision = false,
                contextWindow = 32768,
                modelType = ModelType.CHAT
            )
        ),
        ModelInfo(
            modelId = "gemini-pro-vision",
            displayName = "Gemini Pro Vision",
            providerId = "gemini",
            buildIn = true,
            capabilities = ModelCapabilities(
                supportsFunctionCall = true,
                supportsVision = true,
                contextWindow = 32768,
                modelType = ModelType.CHAT
            )
        ),
        ModelInfo(
            modelId = "gemini-1.5-pro",
            displayName = "Gemini 1.5 Pro",
            providerId = "gemini",
            buildIn = true,
            capabilities = ModelCapabilities(
                supportsFunctionCall = true,
                supportsVision = true,
                contextWindow = 1000000,
                modelType = ModelType.CHAT
            )
        ),
        ModelInfo(
            modelId = "gemini-1.5-flash",
            displayName = "Gemini 1.5 Flash",
            providerId = "gemini",
            buildIn = true,
            capabilities = ModelCapabilities(
                supportsFunctionCall = true,
                supportsVision = true,
                contextWindow = 1000000,
                modelType = ModelType.CHAT
            )
        )
    )

} 