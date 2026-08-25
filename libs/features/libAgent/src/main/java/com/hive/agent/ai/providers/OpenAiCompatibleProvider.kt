// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.agent.ai.providers

import com.hive.agent.config.ConfigAgentModels
import com.hive.plugin.agent.ModelInfo
import com.hive.plugin.agent.ProviderInfo

/**
 * 基于 [OpenAiCompatiblePreset] 的 OpenAI Chat Completions 兼容 Provider。
 */
open class OpenAiCompatibleProvider(
    private val preset: OpenAiCompatiblePreset,
) : OpenAIProvider() {

    override fun getProviderInfo(): ProviderInfo {
        return ConfigAgentModels.findProviderInfo(preset.id) ?: ProviderInfo(
            name = preset.id,
            displayName = preset.displayName(),
            description = preset.description(),
            defaultModelId = preset.defaultModelId,
            defaultMultiModelId = preset.defaultMultiModelId,
            isEnabled = true,
            tags = preset.tags,
            apiKeyPrefix = preset.apiKeyPrefix,
            apiKeyValidateMsg = preset.apiKeyValidateMsg(),
            apiUrl = preset.apiUrl,
            sortIndex = preset.sortIndex,
        )
    }

    override fun getDefaultModelId(): String? = preset.defaultModelId

    override fun getDefaultMultiModelId(): String? = preset.defaultMultiModelId

    override suspend fun getBuildInModels(): List<ModelInfo> = preset.toModelInfoList()
}
