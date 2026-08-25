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
 * Claude Provider实现
 */
class ClaudeProvider : AbstractBaseProvider() {

    override fun getProviderInfo(): ProviderInfo {
        return ConfigAgentModels.findProviderInfo("claude") ?: ProviderInfo(
            name = "claude",
            displayName = "Claude",
            description = "Anthropic Claude",
            defaultModelId = "claude-3-sonnet-20240229",
            defaultMultiModelId = "claude-3-5-sonnet-20241022",
            isEnabled = false, // 这里不能调用isEnabled()，会造成循环调用
            tags = listOf("LLM"),
            apiKeyPrefix = "sk-ant-",
            apiKeyValidateMsg = com.hive.i8n.R.string.api_key_validation_claude.string(),
            apiUrl = "https://api.anthropic.com",
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
            modelId = "claude-3-haiku-20240307",
            displayName = "Claude 3 Haiku",
            providerId = "claude",
            buildIn = true,
            capabilities = ModelCapabilities(
                supportsFunctionCall = true,
                supportsVision = true,
                contextWindow = 200000,
                modelType = ModelType.CHAT
            )
        ),
        ModelInfo(
            modelId = "claude-3-sonnet-20240229",
            displayName = "Claude 3 Sonnet",
            providerId = "claude",
            buildIn = true,
            capabilities = ModelCapabilities(
                supportsFunctionCall = true,
                supportsVision = true,
                contextWindow = 200000,
                modelType = ModelType.CHAT
            )
        ),
        ModelInfo(
            modelId = "claude-3-opus-20240229",
            displayName = "Claude 3 Opus",
            providerId = "claude",
            buildIn = true,
            capabilities = ModelCapabilities(
                supportsFunctionCall = true,
                supportsVision = true,
                contextWindow = 200000,
                modelType = ModelType.CHAT
            )
        ),
        ModelInfo(
            modelId = "claude-3-5-sonnet-20241022",
            displayName = "Claude 3.5 Sonnet",
            providerId = "claude",
            buildIn = true,
            capabilities = ModelCapabilities(
                supportsFunctionCall = true,
                supportsVision = true,
                contextWindow = 200000,
                modelType = ModelType.CHAT
            )
        )
    )
} 