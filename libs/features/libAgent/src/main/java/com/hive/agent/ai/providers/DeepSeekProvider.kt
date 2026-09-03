// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.agent.ai.providers

import com.hive.agent.ai.ReasoningRequestMapper
import com.hive.agent.ai.ReasoningReplayHelper
import com.hive.agent.ai.ReasoningResponseNormalizer
import com.hive.agent.config.ConfigAgentModels
import com.hive.agent.utils.AgentToolCallUtils
import com.hive.plugin.agent.ProviderInfo
import com.hive.plugin.agent.ModelCapabilities
import com.hive.plugin.agent.ModelInfo
import com.hive.plugin.agent.ModelType
import com.hive.plugin.agent.model.ChatCompletionResponse
import com.hive.plugin.agent.model.MessageRole
import com.hive.plugin.agent.model.ReasoningOptions

import com.hive.utils.debug.DLog
import com.hive.utils.extends.string
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.annotations.SerializedName
import com.hive.utils.utils.GsonHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * DeepSeek AI Provider实现
 */
open class DeepSeekProvider : AbstractChatProvider() {

    companion object {
        private const val CACHE_DURATION = 24 * 60 * 60 * 1000L // 24小时缓存
        var cachedModels: List<ModelInfo>? = null
        var cacheTimestamp: Long = 0L
    }

    override fun getProviderInfo(): ProviderInfo {
        return ConfigAgentModels.findProviderInfo("deepseek") ?: ProviderInfo(
            name = "deepseek",
            displayName = "DeepSeek",
            description = "DeepSeek AI",
            defaultModelId = "deepseek-reasoner",
            defaultMultiModelId = null,
            isEnabled = true,
            tags = listOf("LLM"),
            apiKeyPrefix = "",
            apiKeyValidateMsg = com.hive.i8n.R.string.api_key_validation_deepseek.string(),
            apiUrl = "https://api.deepseek.com",
            sortIndex = 999
        )
    }

    // DeepSeek API响应数据类（Gson 序列化）
    private data class DeepSeekModelsResponse(
        val `object`: String,
        val data: List<DeepSeekModelData>
    )

    private data class DeepSeekModelData(
        val id: String,
        val `object`: String,
        val owned_by: String
    )

    /**
     * 获取缓存的模型列表
     */
    private fun getCachedModels(): List<ModelInfo>? {
        return cachedModels
    }

    /**
     * 检查缓存是否有效
     */
    private fun isCacheValid(): Boolean {
        return cachedModels != null && (System.currentTimeMillis() - cacheTimestamp) < CACHE_DURATION
    }

    /**
     * 缓存模型列表
     */
    private fun cacheModels(models: List<ModelInfo>) {
        cachedModels = models
        cacheTimestamp = System.currentTimeMillis()
    }

    /**
     * 清除模型缓存
     */
    fun clearModelCache() {
        cachedModels = null
        cacheTimestamp = 0L
    }

    /**
     * 获取缓存状态信息
     */
    fun getCacheStatus(): String {
        return if (cachedModels != null) {
            val age = System.currentTimeMillis() - cacheTimestamp
            val ageHours = age / (60 * 60 * 1000)
            "缓存中有 ${cachedModels!!.size} 个模型，缓存时间: ${ageHours}小时前"
        } else {
            "无缓存"
        }
    }


    /**
     * 从DeepSeek API获取模型列表，带缓存机制
     */
    override suspend fun getBuildInModels(): List<ModelInfo> = withContext(Dispatchers.IO) {
        try {
            // 检查缓存
            if (isCacheValid()) {
                val cached = getCachedModels()
                return@withContext cached!!
            }

            // 从API获取模型列表
            val models = fetchModelsFromAPI().sortedBy { it.displayName }
            if (models.isNotEmpty()) {
                // 缓存结果
                cacheModels(models)
                return@withContext models
            } else {
                DLog.w("DeepSeekProvider", "API返回空模型列表，使用默认模型")
                return@withContext getDefaultModels()
            }
        } catch (e: Exception) {
            DLog.e("DeepSeekProvider", "获取模型列表失败: ${e.message}")
            // 返回默认模型
            return@withContext getDefaultModels()
        }
    }

    /**
     * 默认模型列表（当API请求失败时使用）
     */
    private fun getDefaultModels(): List<ModelInfo> = listOf(
        ModelInfo(
            modelId = "deepseek-chat",
            displayName = "DeepSeek V3",
            providerId = "deepseek",
            buildIn = true,
            capabilities = ModelCapabilities(
                supportsFunctionCall = true,
                supportsVision = false,
                contextWindow = 128000,
                modelType = ModelType.CHAT
            )
        ),
        ModelInfo(
            modelId = "deepseek-coder",
            displayName = "DeepSeekCoder",
            providerId = "deepseek",
            buildIn = true,
            capabilities = ModelCapabilities(
                supportsFunctionCall = true,
                supportsVision = false,
                contextWindow = 128000,
                modelType = ModelType.CHAT
            )
        ),
        ModelInfo(
            modelId = "deepseek-reasoner",
            displayName = "DeepSeek R1",
            providerId = "deepseek",
            buildIn = true,
            capabilities = ModelCapabilities(
                supportsFunctionCall = true,
                supportsVision = false,
                contextWindow = 128000,
                modelType = ModelType.CHAT
            )
        )
    )

    override fun getDefaultTemperature(): Float = 0.3f

    override fun getDefaultMaxTokens(): Int = 2000

    override fun getChatUrl(): String {
        return getProviderInfo().apiUrl + "/chat/completions"
    }


    override suspend fun buildChatRequest(
        model: String,
        messages: List<com.hive.plugin.agent.model.ChatMessage>,
        temperature: Float,
        maxTokens: Int,
        stream: Boolean,
        tools: List<Any>?,
        reasoning: ReasoningOptions?
    ): String {
        val providerId = getProviderInfo().name
        val deepSeekRequest = DeepSeekChatRequest(
            model = model,
            messages = messages.map { message ->
                DeepSeekMessage(
                    role = message.role.name.lowercase(),
                    content = when (message.role) {
                        MessageRole.TOOL -> message.toolCallResult ?: message.content
                        else -> message.content
                    },
                    reasoning_content = if (message.role == MessageRole.ASSISTANT) {
                        if (ReasoningReplayHelper.shouldReplay(message, providerId, model)) {
                            // DeepSeek thinking 模式下，匹配的 assistant（尤其含 tool_calls）需带回 reasoning_content
                            ReasoningReplayHelper.reasoningContentForWire(message, providerId, model)
                                ?: ""
                        } else {
                            null
                        }
                    } else null,
                    tool_calls = if (message.role == MessageRole.ASSISTANT) {
                        message.toolCalls
                            ?.takeIf { it.isNotEmpty() }
                            ?.map { toolCall ->
                                DeepSeekToolCall(
                                    id = toolCall.id,
                                    type = toolCall.type,
                                    function = DeepSeekFunctionCall(
                                        name = toolCall.function.name,
                                        arguments = GsonHelper.getInstance().toJson(toolCall.function.arguments)
                                    )
                                )
                            }
                    } else null,
                    tool_call_id = if (message.role == MessageRole.TOOL) {
                        message.toolCallId?.takeIf { it.isNotEmpty() }
                    } else null
                )
            },
            temperature = temperature,
            max_tokens = maxTokens,
            stream = stream,
            tools = tools?.map { it as DeepSeekToolDefinition }
        )
        val json = GsonHelper.getInstance().getGson().toJsonTree(deepSeekRequest).asJsonObject
        if (reasoning != null) {
            ReasoningRequestMapper.applyForProvider(json, getProviderInfo().name, reasoning)
        }
        return GsonHelper.getInstance().toJson(json)
    }

    override fun parseChatResponse(responseText: String): ChatCompletionResponse {
        val root = JsonParser().parse(responseText).asJsonObject
        val deepSeekResponse = GsonHelper.getInstance().fromJson(responseText, DeepSeekChatResponse::class.java)
        val firstChoice = deepSeekResponse.choices?.firstOrNull()
        val message = firstChoice?.message
        val providerId = getProviderInfo().name
        val usage = ReasoningResponseNormalizer.extractUsage(root.getAsJsonObject("usage"))
        val normalized = ReasoningResponseNormalizer.normalize(
            content = message?.content,
            reasoningContent = message?.reasoning_content,
            usage = usage,
            providerId = providerId,
            modelId = deepSeekResponse.model,
            replayFormat = ReasoningResponseNormalizer.replayFormatFor(providerId)
        )

        return ChatCompletionResponse(
            content = normalized.content,
            reasoningContent = normalized.reasoningContent,
            model = deepSeekResponse.model,
            usage = normalized.usage,
            toolCalls = message?.tool_calls?.mapNotNull { deepSeekToolCall ->
                AgentToolCallUtils.buildToolCall(
                    logTag = "DeepSeekProvider",
                    id = deepSeekToolCall.id,
                    type = deepSeekToolCall.type,
                    functionName = deepSeekToolCall.function.name,
                    arguments = deepSeekToolCall.function.arguments
                )
            },
            reasoningTrace = normalized.reasoningTrace
        )
    }

    override fun parseStreamResponse(data: String): StreamResponseData {
        val root = runCatching { JsonParser().parse(data).asJsonObject }.getOrNull()
        val streamResponse = GsonHelper.getInstance().fromJson(data, DeepSeekStreamResponse::class.java)
        val choice = streamResponse.choices?.firstOrNull() ?: return StreamResponseData(
            null, null, null, null, null, null, null
        )

        val delta = choice.delta
        val finishReason = choice.finish_reason

        return StreamResponseData(
            content = delta.content,
            reasoningContent = delta.reasoning_content,
            model = streamResponse.model,
            usage = ReasoningResponseNormalizer.extractUsage(root?.getAsJsonObject("usage")),
            toolCalls = delta.tool_calls?.map { toolCall ->
                ToolCallData(
                    index = toolCall.index,
                    id = toolCall.id,
                    type = toolCall.type,
                    functionName = toolCall.function.name,
                    arguments = toolCall.function.arguments
                )
            },
            finishReason = finishReason,
            cost = null
        )
    }

    override fun isStreamResponseComplete(finishReason: String?): Boolean {
        return finishReason != null && finishReason != "null"
    }

    override fun buildStreamResponse(
        accumulatedContent: String,
        accumulatedReasoningContent: String?,
        model: String?,
        usage: Map<String, Int>,
        toolCalls: List<Any>?,
        cost: Double?,
        reasoningDetails: com.google.gson.JsonArray?
    ): ChatCompletionResponse {
        val providerId = getProviderInfo().name
        val normalized = ReasoningResponseNormalizer.normalize(
            content = accumulatedContent.ifEmpty { null },
            reasoningContent = accumulatedReasoningContent?.ifEmpty { null },
            reasoningDetailsJson = reasoningDetails,
            usage = usage,
            providerId = providerId,
            modelId = model,
            replayFormat = ReasoningResponseNormalizer.replayFormatFor(providerId)
        )
        return ChatCompletionResponse(
            content = normalized.content,
            reasoningContent = normalized.reasoningContent,
            model = model,
            usage = normalized.usage,
            cost = cost,
            toolCalls = toolCalls as? List<com.hive.plugin.agent.model.ToolCall>,
            reasoningTrace = normalized.reasoningTrace
        )
    }

    override fun createToolDefinition(type: String, func: Any): Any {
        return DeepSeekToolDefinition(
            toolType = type,
            function = func as DeepSeekFunctionDefinition
        )
    }

    override fun createFunctionDefinition(name: String, desc: String, params: JsonObject): Any {
        return DeepSeekFunctionDefinition(
            name = name,
            description = desc,
            parameters = params
        )
    }

    /**
     * 从DeepSeek API获取模型列表
     */
    private suspend fun fetchModelsFromAPI(): List<ModelInfo> {
        if (getApiKey().isEmpty()) {
            return mutableListOf()
        }
        val url = getProviderInfo().apiUrl + "/models"
        val connection = URL(url).openConnection() as HttpURLConnection

        return try {
            connection.apply {
                requestMethod = "GET"
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "Automio/1.0")
                val apiKey = getApiKey()
                if (apiKey.isNotEmpty()) {
                    setRequestProperty("Authorization", "Bearer $apiKey")
                } else {
                    DLog.w("DeepSeekProvider", "未找到API密钥，请先设置API密钥")
                }
                connectTimeout = 15000
                readTimeout = 15000
            }

            val responseCode = connection.responseCode

            if (responseCode == 200) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val deepSeekResponse = GsonHelper.getInstance().fromJson(response, DeepSeekModelsResponse::class.java)

                return deepSeekResponse.data.mapNotNull { model ->
                    try {
                        val supportsVision = false
                        val contextWindow = 128000
                        val supportsFunctionCall = true

                        ModelInfo(
                            modelId = model.id,
                            displayName = model.id.split("-").joinToString(" ") { it.capitalize() },
                            providerId = "deepseek",
                            buildIn = true,
                            capabilities = ModelCapabilities(
                                supportsFunctionCall = supportsFunctionCall,
                                supportsVision = supportsVision,
                                contextWindow = contextWindow,
                                modelType = ModelType.CHAT
                            )
                        )
                    } catch (e: Exception) {
                        DLog.e("DeepSeekProvider", "解析模型 ${model.id} 失败: ${e.message}")
                        null
                    }
                }
            } else {
                val errorStream = connection.errorStream
                val errorText = errorStream?.bufferedReader()?.use { it.readText() } ?: "未知错误"
                DLog.e(
                    "DeepSeekProvider",
                    "API请求失败，状态码: $responseCode, 错误信息: $errorText"
                )
                emptyList()
            }
        } catch (e: Exception) {
            DLog.e("DeepSeekProvider", "请求DeepSeek API失败: ${e.message}")
            emptyList()
        } finally {
            connection.disconnect()
        }
    }


}

// DeepSeek API数据模型 - 与 DeepSeek API 的 JSON 结构保持一致（Gson 序列化）
private data class DeepSeekChatRequest(
    val model: String,
    val messages: List<DeepSeekMessage>,
    val temperature: Float = 0.3f,
    val max_tokens: Int = 2000,
    val stream: Boolean = false,
    val tools: List<DeepSeekToolDefinition>? = null
)

private data class DeepSeekMessage(
    val role: String? = null,
    val content: String? = null,
    val reasoning_content: String? = null,
    val tool_calls: List<DeepSeekToolCall>? = null,
    val tool_call_id: String? = null
)

private data class DeepSeekToolDefinition(
    @SerializedName("type") val toolType: String = "function",
    val function: DeepSeekFunctionDefinition
)

private data class DeepSeekFunctionDefinition(
    val name: String,
    val description: String,
    val parameters: JsonObject
)

private data class DeepSeekToolCall(
    val id: String? = null,
    val index: Int? = null,
    val type: String? = null,
    val function: DeepSeekFunctionCall
)

private data class DeepSeekFunctionCall(
    val name: String? = null,
    val arguments: String? = null
)

private data class DeepSeekChatResponse(
    val id: String,
    val `object`: String,
    val created: Long,
    val model: String,
    val choices: List<DeepSeekChoice>? = null,
    val usage: DeepSeekUsage? = null
)

private data class DeepSeekChoice(
    val index: Int,
    val message: DeepSeekMessage,
    val finish_reason: String? = null
)

private data class DeepSeekUsage(
    val prompt_tokens: Int,
    val completion_tokens: Int,
    val total_tokens: Int
)

// 流式响应数据模型
private data class DeepSeekStreamResponse(
    val id: String,
    val created: Long,
    val model: String,
    val choices: List<DeepSeekStreamChoice>? = null,
    val usage: DeepSeekUsage? = null
)

private data class DeepSeekStreamChoice(
    val index: Int,
    val delta: DeepSeekMessage,
    val finish_reason: String? = null
)

