// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.agent.ai.providers

import com.hive.agent.config.ConfigAgentModels
import com.hive.plugin.agent.ModelInfo
import com.hive.plugin.agent.ProviderInfo
import com.hive.utils.GlobalApp
import com.hive.utils.extends.string

/**
 * 用户自定义 OpenAI 兼容 Provider：自行填写 Base URL 与 API Key，模型通过「自定义模型」添加。
 */
class CustomOpenAIProvider : OpenAIProvider() {

    companion object {
        const val PROVIDER_ID = "openai_custom"
    }

    override fun getProviderInfo(): ProviderInfo {
        return ConfigAgentModels.findProviderInfo(PROVIDER_ID) ?: ProviderInfo(
            name = PROVIDER_ID,
            displayName = GlobalApp.getString(com.hive.i8n.R.string.ai_provider_openai_custom),
            description = GlobalApp.getString(com.hive.i8n.R.string.ai_provider_openai_custom_desc),
            defaultModelId = null,
            defaultMultiModelId = null,
            isEnabled = true,
            tags = listOf("LLM", "OpenAI-Compatible", "Custom"),
            apiKeyPrefix = "",
            apiKeyValidateMsg = com.hive.i8n.R.string.api_key_validation_openai_custom.string(),
            apiUrl = "",
            sortIndex = 600,
        )
    }

    override fun supportsEditableBaseUrl(): Boolean = true

    override fun requiresBaseUrl(): Boolean = true

    override fun isProviderReady(): Boolean {
        if (!super.isProviderReady()) return false
        return resolveEffectiveBaseUrl().isNotBlank()
    }

    override suspend fun getBuildInModels(): List<ModelInfo> = emptyList()
}
