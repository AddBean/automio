// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.agent.ai.providers

import com.google.gson.JsonParser
import com.hive.agent.ai.ModelCapabilityDetector
import com.hive.agent.config.ConfigAgentModels
import com.hive.plugin.agent.ModelInfo
import com.hive.plugin.agent.ProviderInfo
import com.hive.utils.GlobalApp
import com.hive.utils.debug.DLog
import com.hive.utils.extends.string
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * 用户自定义 OpenAI 兼容 Provider：自行填写 Base URL 与 API Key，模型通过「自定义模型」添加。
 */
class CustomOpenAIProvider : OpenAIProvider() {

    companion object {
        const val PROVIDER_ID = "openai_custom"
        private const val CACHE_DURATION = 5 * 60 * 1000L
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
            sortIndex = 10_000,
        )
    }

    override fun supportsEditableBaseUrl(): Boolean = true

    override fun requiresBaseUrl(): Boolean = true

    override fun isProviderReady(): Boolean {
        if (!super.isProviderReady()) return false
        return resolveEffectiveBaseUrl().isNotBlank()
    }

    @Volatile
    private var discoveredModels: List<ModelInfo>? = null

    @Volatile
    private var discoveredAt: Long = 0L

    override suspend fun getBuildInModels(): List<ModelInfo> = withContext(Dispatchers.IO) {
        val cached = discoveredModels
        if (cached != null && System.currentTimeMillis() - discoveredAt < CACHE_DURATION) {
            return@withContext cached
        }

        val customIds = serviceManager?.getProviderCustomModels(PROVIDER_ID)
            ?.mapTo(mutableSetOf()) { it.modelId }
            .orEmpty()
        val models = discoverModels().filterNot { it.modelId in customIds }
        if (models.isNotEmpty()) {
            discoveredModels = models
            discoveredAt = System.currentTimeMillis()
        }
        models
    }

    fun clearModelCache() {
        discoveredModels = null
        discoveredAt = 0L
    }

    private fun discoverModels(): List<ModelInfo> {
        if (!isProviderReady()) return emptyList()
        val endpoint = OpenAiUrlHelper.modelsUrl(resolveEffectiveBaseUrl())
        if (endpoint.isBlank()) return emptyList()
        val connection = URL(endpoint).openConnection() as HttpURLConnection
        return try {
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("Authorization", "Bearer ${getApiKey()}")
            connection.setRequestProperty("User-Agent", "Automio/1.0")
            connection.connectTimeout = 10_000
            connection.readTimeout = 15_000
            if (connection.responseCode !in 200..299) return emptyList()

            val body = connection.inputStream.bufferedReader().use { it.readText() }
            val data = JsonParser().parse(body).asJsonObject.getAsJsonArray("data")
                ?: return emptyList()
            data.mapNotNull { item ->
                val id = item.asJsonObject.get("id")?.asString?.trim().orEmpty()
                if (id.isEmpty()) null else ModelInfo(
                    modelId = id,
                    displayName = id,
                    providerId = PROVIDER_ID,
                    buildIn = true,
                    capabilities = ModelCapabilityDetector.detect(id)
                )
            }.distinctBy { it.modelId }.sortedBy { it.displayName.lowercase() }
        } catch (e: Exception) {
            DLog.w("CustomOpenAIProvider", "模型自动发现失败: ${e.message}")
            emptyList()
        } finally {
            connection.disconnect()
        }
    }
}
