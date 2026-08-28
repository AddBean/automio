// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.agent.ai

import com.hive.agent.ai.providers.AbstractBaseProvider
import com.hive.agent.ai.providers.AbstractChatProvider
import com.hive.agent.ai.providers.ArkAgentPlanProvider
import com.hive.agent.ai.providers.ArkCodingPlanProvider
import com.hive.agent.ai.providers.BailianCodeProvider
import com.hive.agent.ai.providers.ClaudeProvider
import com.hive.agent.ai.providers.CustomOpenAIProvider
import com.hive.agent.ai.providers.DeepSeekProvider
import com.hive.agent.ai.providers.GeminiProvider
import com.hive.agent.ai.providers.OpenAIProvider
import com.hive.agent.ai.providers.OpenAiCompatiblePresets
import com.hive.agent.ai.providers.OpenRouterProvider
import com.hive.plugin.agent.AIServiceManager
import com.hive.plugin.agent.AIServiceProvider
import com.hive.plugin.agent.InferenceType
import com.hive.plugin.agent.ModelInfo
import com.hive.plugin.agent.ProviderInfo
import com.hive.plugin.agent.model.AIRequest
import com.hive.plugin.agent.model.AIResult
import com.hive.plugin.agent.model.AgentError
import com.hive.plugin.agent.model.AgentErrorCode
import com.hive.plugin.agent.model.ChatCompletionResponse
import com.hive.utils.GlobalApp
import com.hive.utils.extends.toJson
import com.hive.utils.global.SPTools
import com.hive.utils.utils.GsonHelper
import kotlinx.coroutines.DelicateCoroutinesApi
import java.util.concurrent.ConcurrentHashMap

/**
 * 移动端AI服务管理器
 */
class DefaultAIServiceManager(
) : AIServiceManager {

    private val providers = ConcurrentHashMap<String, AIServiceProvider>()
    private val providerOrder = mutableListOf<String>()

    private val enabledProviders = ConcurrentHashMap<String, Boolean>()

    // Configuration management functionality merged from ProviderConfigManager
    private val spTools = SPTools.getInstance()
    private val secureApiKeyStore = SecureApiKeyStore(GlobalApp.getContext())


    override fun registerProvider(provider: AIServiceProvider) {
        val providerId = provider.getProviderInfo().name
        providers[providerId] = provider
        enabledProviders[providerId] = true
        if (!providerOrder.contains(providerId)) {
            providerOrder.add(providerId)
        }
    }

    override fun unregisterProvider(providerId: String) {
        providers.remove(providerId)
        enabledProviders.remove(providerId)
        providerOrder.remove(providerId)
    }

    override fun clearProviders() {
        providers.clear()
        enabledProviders.clear()
        providerOrder.clear()
    }

    override fun enableProvider(providerId: String) {
        enabledProviders[providerId] = true
    }

    override fun disableProvider(providerId: String) {
        enabledProviders[providerId] = false
    }

    override fun getProviderList(): List<AIServiceProvider> {
        return providerOrder.mapNotNull(::getProvider)
    }

    override fun getEnabledProviders(): List<AIServiceProvider> {
        return providerOrder.mapNotNull { providerId ->
            getProvider(providerId)?.takeIf { enabledProviders[providerId] == true }
        }
    }

    override fun getProvider(providerId: String): AIServiceProvider? {
        return providers[providerId]
    }

    override fun isProviderEnabled(providerId: String): Boolean {
        return enabledProviders[providerId] ?: false
    }

    override fun getAvailableProvider(): AIServiceProvider? {
        return getEnabledProviders().firstOrNull { it.isProviderReady() }
            ?: getEnabledProviders().firstOrNull()
    }

    override suspend fun <T> inference(request: AIRequest): AIResult<T> {
        val availableProvider = getAvailableProvider()
            ?: return AIResult.Failure(
                error = AgentError.create(AgentErrorCode.AI_NO_AVAILABLE_PROVIDER)
            )

        return availableProvider.inference(request)
    }

    override suspend fun <T> streamInference(
        request: AIRequest,
        onChunkResponse: ((ChatCompletionResponse) -> Unit)?
    ): AIResult<T> {
        val availableProvider = getAvailableProvider()
            ?: throw Exception(GlobalApp.getString(com.hive.i8n.R.string.ai_no_available_provider))

        return availableProvider.streamInference<T>(request, onChunkResponse)
    }

    override fun stopInference(request: AIRequest?) {
        providers.values.forEach { provider ->
            if (enabledProviders[provider.getProviderInfo().name] == true) {
                provider.stopInference(request)
            }
        }
    }

    /**
     * 初始化AI服务提供者
     */
    override fun initAIServiceProviders() {
        clearProviders()

        val providers = buildList {
            add(DeepSeekProvider())
            add(OpenRouterProvider())
            add(BailianCodeProvider())
            add(ArkAgentPlanProvider())
            add(ArkCodingPlanProvider())
            addAll(OpenAiCompatiblePresets.createProviders())
            add(OpenAIProvider())
            add(CustomOpenAIProvider())
            add(ClaudeProvider())
            add(GeminiProvider())
        }

        providers.filter { it.getProviderInfo().isEnabled }.forEach { provider ->
            registerProvider(provider)
            (provider as? AbstractBaseProvider)?.serviceManager = this@DefaultAIServiceManager
        }
    }


    /**
     * 获取Provider的API Key
     */
    fun getProviderApiKey(providerId: String): String? {
        val key = "${providerId}_api_key"
        secureApiKeyStore.get(providerId)?.let { return ApiKeySanitizer.sanitize(it) }
        // One-time migration from legacy plain SharedPreferences.
        val legacy = spTools.getString(key, null)
        if (!legacy.isNullOrBlank()) {
            val sanitized = ApiKeySanitizer.sanitize(legacy)
            secureApiKeyStore.put(providerId, sanitized)
            spTools.remove(key)
            return sanitized
        }
        return ApiKeySanitizer.sanitize(getProviderInfo(providerId)?.apiKey)
            .takeIf { it.isNotEmpty() }
    }

    /**
     * 设置Provider的API Key
     */
    fun setProviderApiKey(providerId: String, apiKey: String) {
        secureApiKeyStore.put(providerId, ApiKeySanitizer.sanitize(apiKey))
    }

    /**
     * 清除Provider的API Key
     */
    fun clearProviderApiKey(providerId: String) {
        secureApiKeyStore.remove(providerId)
        spTools.remove("${providerId}_api_key")
    }

    fun getProviderBaseUrl(providerId: String): String? =
        spTools.getString("${providerId}_base_url", null)?.trim()?.trimEnd('/')

    fun setProviderBaseUrl(providerId: String, baseUrl: String) {
        spTools.putStringImmediately("${providerId}_base_url", baseUrl.trim().trimEnd('/'))
    }

    fun clearProviderBaseUrl(providerId: String) {
        spTools.remove("${providerId}_base_url")
    }

    /**
     * 检查Provider是否有有效的API Key
     */
    fun hasValidApiKey(providerId: String, apiKeyPrefix: String?): Boolean {
        val apiKey = getProviderApiKey(providerId)
        return !apiKey.isNullOrEmpty() && apiKey.startsWith(apiKeyPrefix?:"")
    }

    /**
     * 获取Provider启用的模型列表
     */
    private fun getProviderEnabledModels(providerId: String): List<String> {
        val key = "${providerId}_enabled_models"
        val modelsJson = spTools.getString(key, "")
        return try {
            modelsJson.split(",").filter { it.isNotEmpty() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * 设置Provider启用的模型列表
     */
    private fun setProviderEnabledModels(providerId: String, models: List<String>) {
        val key = "${providerId}_enabled_models"
        val modelsJson = models.joinToString(",")
        spTools.putStringImmediately(key, modelsJson)
    }

    /**
     * 启用Provider的模型
     */
    override fun enableProviderModel(providerId: String, modelId: String) {
        val currentModels = getProviderEnabledModels(providerId).toMutableList()
        if (!currentModels.contains(modelId)) {
            currentModels.add(modelId)
            setProviderEnabledModels(providerId, currentModels)
        }
    }

    /**
     * 禁用Provider的模型
     */
    override fun disableProviderModel(providerId: String, modelId: String) {
        val currentModels = getProviderEnabledModels(providerId).toMutableList()
        currentModels.remove(modelId)
        setProviderEnabledModels(providerId, currentModels)
    }

    /**
     * 检查Provider的模型是否启用
     */
    override fun isProviderModelEnabled(providerId: String, modelId: String): Boolean {
        return true
    }

    /**
     * 获取Provider的自定义模型列表
     */
    override fun getProviderCustomModels(providerId: String): List<ModelInfo> {
        val key = "${providerId}_custom_models"
        val modelsJson = spTools.getString(key, "[]")
        return try {
            GsonHelper.getInstance().fromListJson(modelsJson, ModelInfo::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * 添加Provider的自定义模型
     */
    override fun addProviderCustomModel(providerId: String, modelInfo: ModelInfo) {
        val currentModels = getProviderCustomModels(providerId).toMutableList()
        // 检查是否已存在相同modelId的模型
        val existingIndex = currentModels.indexOfFirst { it.modelId == modelInfo.modelId }
        if (existingIndex != -1) {
            // 如果已存在，则更新现有模型
            currentModels[existingIndex] = modelInfo
        } else {
            // 如果不存在，则添加新模型
            currentModels.add(modelInfo)
        }
        // 保存到SharedPreferences
        saveProviderCustomModels(providerId, currentModels)

        getProvider(providerId)?.updateCustomModels(currentModels)
    }

    /**
     * 删除Provider的自定义模型
     */
    override fun removeProviderCustomModel(providerId: String, modelId: String) {
        val currentModels = getProviderCustomModels(providerId).toMutableList()
        val removed = currentModels.removeAll { it.modelId == modelId }
        if (removed) {
            // 保存到SharedPreferences
            saveProviderCustomModels(providerId, currentModels)
            getProvider(providerId)?.updateCustomModels(currentModels)
        }
    }

    /**
     * 保存Provider的自定义模型列表到SharedPreferences
     */
    private fun saveProviderCustomModels(providerId: String, models: List<ModelInfo>) {
        val key = "${providerId}_custom_models"
        val modelsJson = GsonHelper.getInstance().toJson(models)
        spTools.putStringImmediately(key, modelsJson)
    }

    /**
     * 检查Provider是否有自定义模型
     */
    override fun hasProviderCustomModels(providerId: String): Boolean {
        return getProviderCustomModels(providerId).isNotEmpty()
    }

    /**
     * 获取Provider的自定义模型数量
     */
    fun getProviderCustomModelsCount(providerId: String): Int {
        return getProviderCustomModels(providerId).size
    }

    /**
     * 根据modelId获取Provider的自定义模型
     */
    fun getProviderCustomModel(providerId: String, modelId: String): ModelInfo? {
        return getProviderCustomModels(providerId).find { it.modelId == modelId }
    }

    /**
     * 清空Provider的所有自定义模型
     */
    override fun clearProviderCustomModels(providerId: String) {
        saveProviderCustomModels(providerId, emptyList())
    }

    override fun setInferenceModel(type: InferenceType, model: ModelInfo?) {
        spTools.putStringImmediately("ai_normal_model_${type.type}", model?.toJson())
    }

    override fun getInferenceModel(type: InferenceType): ModelInfo? {
        val json = spTools.getString("ai_normal_model_${type.type}", null)
        val model = GsonHelper.getInstance().fromJson(json, ModelInfo::class.java)
        return model
    }

    @OptIn(DelicateCoroutinesApi::class)
    override suspend fun loadDefaultInferenceModelIfNeeded(providerId: String) {
        val provider = getProvider(providerId) as? AbstractChatProvider ?: return
        // 未配置好 Provider（如缺 API Key）时不自动写入模型，避免误判为已设置
        if (!provider.isProviderReady()) return
        if (getInferenceModel(InferenceType.TEXT) == null) {
            val modelId = provider.getDefaultModelId()
            provider.getModels().firstOrNull { it.modelId == modelId }?.run {
                setInferenceModel(InferenceType.TEXT, this)
            }
        }
        if (getInferenceModel(InferenceType.IMAGE) == null) {
            val modelId = provider.getDefaultMultiModelId()
            provider.getModels().firstOrNull { it.modelId == modelId }?.run {
                setInferenceModel(InferenceType.IMAGE, this)
            }
        }
    }

    /**
     * 获取特定Provider的信息
     */
    fun getProviderInfo(providerId: String): ProviderInfo? {
        val provider = providers[providerId] as? AbstractBaseProvider ?: return null
        return provider.getProviderInfo()
    }
}
