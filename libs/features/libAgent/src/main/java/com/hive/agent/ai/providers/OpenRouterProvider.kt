// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.agent.ai.providers

import com.hive.agent.ai.OpenRouterReasoningMetadataParser
import com.hive.agent.ai.ReasoningRequestMapper
import com.hive.agent.ai.ReasoningResponseNormalizer
import com.hive.agent.config.ConfigAgentModels
import com.hive.agent.utils.AgentToolCallUtils
import com.hive.plugin.agent.ProviderInfo
import com.hive.plugin.agent.ModelCapabilities
import com.hive.plugin.agent.ModelInfo
import com.hive.plugin.agent.ModelType
import com.hive.plugin.agent.model.AttachmentType
import com.hive.plugin.agent.model.ChatCompletionResponse
import com.hive.plugin.agent.model.ChatMessage
import com.hive.plugin.agent.model.MessageRole
import com.hive.plugin.agent.model.ReasoningOptions

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.JsonPrimitive
import com.hive.utils.debug.DLog
import com.hive.utils.extends.string
import com.hive.utils.file.FileUtils
import com.hive.utils.utils.GsonHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * OpenRouter AI Provider实现
 */
open class OpenRouterProvider : AbstractChatProvider() {

    override fun getProviderInfo(): ProviderInfo {
        return ConfigAgentModels.findProviderInfo("openrouter") ?: ProviderInfo(
            name = "openrouter",
            displayName = "OpenRouter",
            description = "OpenRouter AI",
            defaultModelId = "anthropic/claude-3.5-sonnet",
            defaultMultiModelId = "anthropic/claude-3.5-sonnet",
            isEnabled = true, // 这里不能调用isEnabled()，会造成循环调用
            tags = listOf("LLM"),
            apiKeyPrefix = "sk-or-",
            apiKeyValidateMsg = com.hive.i8n.R.string.api_key_validation_openrouter.string(),
            apiUrl = "https://openrouter.ai",
            sortIndex = 888
        )
    }

    companion object {
        private const val CACHE_DURATION = 24 * 60 * 60 * 1000L // 24小时缓存
        private var cachedModels: List<ModelInfo>? = null
        private var cacheTimestamp: Long = 0L
    }

    // OpenRouter API响应数据类
    private data class OpenRouterModelsResponse(
        val data: List<OpenRouterModel>
    )

    private data class OpenRouterModel(
        val id: String,
        val name: String,
        val description: String? = null,
        val context_length: Int? = null,
        val architecture: OpenRouterArchitecture? = null,
        val top_provider: OpenRouterTopProvider? = null,
        val pricing: OpenRouterPricing? = null,
        val reasoning: JsonObject? = null
    )

    private data class OpenRouterArchitecture(
        val input_modalities: List<String>? = null,
        val output_modalities: List<String>? = null,
        val tokenizer: String? = null,
        val instruct_type: String? = null
    )

    private data class OpenRouterTopProvider(
        val is_moderated: Boolean? = null,
        val context_length: Int? = null,
        val max_completion_tokens: Int? = null
    )

    private data class OpenRouterPricing(
        val prompt: String? = null,
        val completion: String? = null,
        val image: String? = null,
        val request: String? = null,
        val web_search: String? = null,
        val internal_reasoning: String? = null,
        val input_cache_read: String? = null,
        val input_cache_write: String? = null
    )

    override fun getChatUrl(): String {
        return getProviderInfo().apiUrl + "/api/v1/chat/completions"
    }

    /**
     * 从OpenRouter API获取模型列表，带缓存机制
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
                DLog.w("OpenRouterProvider", "API返回空模型列表，使用默认模型")
                return@withContext getDefaultModels()
            }
        } catch (e: Exception) {
            DLog.e("OpenRouterProvider", "获取模型列表失败: ${e.message}")
            // 返回默认模型
            return@withContext getDefaultModels()
        }
    }

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
     * 默认模型列表（当API请求失败时使用）
     */
    private fun getDefaultModels(): List<ModelInfo> = listOf(
        ModelInfo(
            modelId = "openai/gpt-4o",
            displayName = "GPT-4o",
            providerId = "openrouter",
            buildIn = true,
            capabilities = ModelCapabilities(
                supportsFunctionCall = true,
                supportsVision = true,
                contextWindow = 128000,
                modelType = ModelType.CHAT
            )
        ),
        ModelInfo(
            modelId = "openai/gpt-4o-mini",
            displayName = "GPT-4o Mini",
            providerId = "openrouter",
            buildIn = true,
            capabilities = ModelCapabilities(
                supportsFunctionCall = true,
                supportsVision = true,
                contextWindow = 128000,
                modelType = ModelType.CHAT
            )
        ),
        ModelInfo(
            modelId = "anthropic/claude-3.5-sonnet",
            displayName = "Claude 3.5 Sonnet",
            providerId = "openrouter",
            buildIn = true,
            capabilities = ModelCapabilities(
                supportsFunctionCall = true,
                supportsVision = true,
                contextWindow = 200000,
                modelType = ModelType.CHAT
            )
        ),
        ModelInfo(
            modelId = "anthropic/claude-3.5-haiku",
            displayName = "Claude 3.5 Haiku",
            providerId = "openrouter",
            buildIn = true,
            capabilities = ModelCapabilities(
                supportsFunctionCall = true,
                supportsVision = true,
                contextWindow = 200000,
                modelType = ModelType.CHAT
            )
        ),
        ModelInfo(
            modelId = "meta-llama/llama-3.1-70b-instruct",
            displayName = "Llama 3.1 70B",
            providerId = "openrouter",
            buildIn = true,
            capabilities = ModelCapabilities(
                supportsFunctionCall = true,
                supportsVision = false,
                contextWindow = 8192,
                modelType = ModelType.CHAT
            )
        )
    )

    /**
     * 构建消息内容
     */
    private suspend fun buildMessageContent(
        message: ChatMessage,
        withAttachment: Boolean
    ): com.google.gson.JsonElement {
        val imageAttachment = message.attachments.firstOrNull {
            it.type == AttachmentType.IMAGE
        }

        return if (imageAttachment != null && withAttachment) {
            val imageUrl = processImageAttachment(imageAttachment)
            if (imageUrl != null) {
                val contentParts = JsonArray().apply {
                    add(JsonObject().apply {
                        addProperty("type", "text")
                        addProperty("text", message.content ?: "")
                    })
                    add(JsonObject().apply {
                        addProperty("type", "image_url")
                        add("image_url", JsonObject().apply {
                            addProperty("url", imageUrl)
                        })
                    })
                }
                contentParts
            } else {
                JsonPrimitive(message.content ?: "")
            }
        } else {
            JsonPrimitive(message.content ?: "")
        }
    }

    /**
     * 处理图片附件，支持本地文件路径和base64
     */
    private suspend fun processImageAttachment(attachment: com.hive.plugin.agent.model.ChatAttachment): String? {
        return withContext(Dispatchers.IO) {
            // 如果已经有base64数据，直接使用
            if (!attachment.base64.isNullOrEmpty()) {
                return@withContext attachment.base64//"data:${attachment.mimeType ?: "image/jpeg"};base64,${attachment.base64}"
            }

            // 处理URL
            val url = attachment.url ?: return@withContext null

            return@withContext when {
                // 如果是data URL，直接返回
                url.startsWith("data:") -> url

                // 如果是本地文件路径，尝试转换为base64
                isLocalFilePath(url) -> {
                    attachment.base64 = FileUtils.convertLocalFileToBase64(url, attachment.mimeType)
                    attachment.base64
                }

                // 如果是网络URL，直接返回
                url.startsWith("http://") || url.startsWith("https://") -> url

                // 其他情况，尝试作为本地文件处理
                else -> FileUtils.convertLocalFileToBase64(url, attachment.mimeType)
            }
        }
    }

    /**
     * 判断是否为本地文件路径
     */
    private fun isLocalFilePath(path: String): Boolean {
        return path.startsWith("/") ||
                path.startsWith("file://") ||
                path.startsWith("content://") ||
                !path.contains("://")
    }


    /**
     * 构建工具调用
     */
    private fun buildToolCalls(toolCalls: List<com.hive.plugin.agent.model.ToolCall>): JsonArray {
        val arr = JsonArray()
        toolCalls.forEach { toolCall ->
            arr.add(JsonObject().apply {
                addProperty("id", toolCall.id)
                addProperty("type", toolCall.type)
                add("function", JsonObject().apply {
                    addProperty("name", toolCall.function.name)
                    addProperty("arguments", GsonHelper.getInstance().toJson(toolCall.function.arguments))
                })
            })
        }
        return arr
    }

    /**
     * 构建OpenRouter消息
     */
    private suspend fun buildOpenRouterMessage(message: ChatMessage): List<JsonObject> {
        val list = mutableListOf<JsonObject>()
        val contentMessage = if (message.role == MessageRole.TOOL) {
            message.copy(content = message.toolCallResult ?: message.content)
        } else {
            message
        }
        list.add(JsonObject().apply {
            addProperty("role", contentMessage.role.name.lowercase())
            add("content", buildMessageContent(contentMessage, false))
            when (contentMessage.role) {
                MessageRole.ASSISTANT -> contentMessage.toolCalls
                    ?.takeIf { it.isNotEmpty() }
                    ?.let { add("tool_calls", buildToolCalls(it)) }
                MessageRole.TOOL -> contentMessage.toolCallId
                    ?.takeIf { it.isNotEmpty() }
                    ?.let { addProperty("tool_call_id", it) }
                else -> Unit
            }
        })
        if (message.role == MessageRole.TOOL && message.attachments.isNotEmpty()) {
            val imageMessage = message.copy(content = "工具执行的图片信息如下：")
            list.add(JsonObject().apply {
                addProperty("role", MessageRole.USER.name.lowercase())
                add("content", buildMessageContent(imageMessage, true))
            })
        }
        return list
    }

    private suspend fun buildOpenRouterMessages(messages: List<ChatMessage>): List<JsonObject> {
        val result = mutableListOf<JsonObject>()
        var index = 0
        while (index < messages.size) {
            val message = messages[index]
            if (message.role == MessageRole.ASSISTANT && !message.toolCalls.isNullOrEmpty()) {
                result.addAll(buildOpenRouterMessage(message))
                index++
                val deferredImageMessages = mutableListOf<JsonObject>()
                while (index < messages.size && messages[index].role == MessageRole.TOOL) {
                    val built = buildOpenRouterMessage(messages[index])
                    if (built.isNotEmpty()) {
                        result.add(built.first())
                        if (built.size > 1) {
                            deferredImageMessages.addAll(built.drop(1))
                        }
                    }
                    index++
                }
                result.addAll(deferredImageMessages)
                continue
            }
            result.addAll(buildOpenRouterMessage(message))
            index++
        }
        return result
    }

    override fun getDefaultTemperature(): Float = 0.7f

    override fun getDefaultMaxTokens(): Int = 2000

    override suspend fun buildChatRequest(
        model: String,
        messages: List<ChatMessage>,
        temperature: Float,
        maxTokens: Int,
        stream: Boolean,
        tools: List<Any>?,
        reasoning: ReasoningOptions?
    ): String {
        val openRouterRequest = OpenRouterChatRequest(
            model = model,
            messages = buildOpenRouterMessages(messages),
            temperature = temperature,
            max_tokens = maxTokens,
            stream = stream,
            tools = tools?.map { it as OpenRouterToolDefinition }
        )
        val json = GsonHelper.getInstance().getGson().toJsonTree(openRouterRequest).asJsonObject
        if (reasoning != null) {
            ReasoningRequestMapper.applyForProvider(json, getProviderInfo().name, reasoning)
        }
        return GsonHelper.getInstance().toJson(json)
    }


    override fun parseChatResponse(responseText: String): ChatCompletionResponse {
        val root = JsonParser().parse(responseText).asJsonObject
        val openRouterResponse = GsonHelper.getInstance().fromJson(responseText, OpenRouterChatResponse::class.java)
        val firstChoice = openRouterResponse.choices?.firstOrNull()
        val message = firstChoice?.message
        val providerId = getProviderInfo().name
        val usage = ReasoningResponseNormalizer.extractUsage(root.getAsJsonObject("usage"))
        val details = root.getAsJsonArray("choices")
            ?.firstOrNull()
            ?.takeIf { it.isJsonObject }
            ?.asJsonObject
            ?.getAsJsonObject("message")
            ?.getAsJsonArray("reasoning_details")
        val reasoningText = message?.reasoning ?: message?.reasoning_content
        val normalized = ReasoningResponseNormalizer.normalize(
            content = message?.content,
            reasoningContent = reasoningText,
            reasoningDetailsJson = details,
            usage = usage,
            providerId = providerId,
            modelId = openRouterResponse.model,
            replayFormat = ReasoningResponseNormalizer.replayFormatFor(providerId)
        )

        return ChatCompletionResponse(
            content = normalized.content,
            reasoningContent = normalized.reasoningContent,
            model = openRouterResponse.model,
            usage = normalized.usage,
            cost = openRouterResponse.usage?.cost
                ?: root.getAsJsonObject("usage")?.get("cost")?.takeIf { it.isJsonPrimitive }?.asDouble,
            toolCalls = message?.tool_calls?.mapNotNull { openRouterToolCall ->
                AgentToolCallUtils.buildToolCall(
                    logTag = "OpenRouterProvider",
                    id = openRouterToolCall.id,
                    type = openRouterToolCall.type,
                    functionName = openRouterToolCall.function.name,
                    arguments = openRouterToolCall.function.arguments
                )
            },
            reasoningTrace = normalized.reasoningTrace
        )
    }

    override fun parseStreamResponse(data: String): StreamResponseData {
        val root = runCatching { JsonParser().parse(data).asJsonObject }.getOrNull()
        val streamResponse = GsonHelper.getInstance().fromJson(data, OpenRouterStreamResponse::class.java)
        val choice = streamResponse.choices?.firstOrNull() ?: return StreamResponseData(
            null, null, null, null, null, null, null
        )

        val delta = choice.delta
        val finishReason = choice.finish_reason
        val detailsChunk = root?.getAsJsonArray("choices")
            ?.firstOrNull()
            ?.takeIf { it.isJsonObject }
            ?.asJsonObject
            ?.getAsJsonObject("delta")
            ?.getAsJsonArray("reasoning_details")
        val reasoningChunk = delta.reasoning ?: delta.reasoning_content
            ?: detailsChunk?.let { details ->
                details.mapNotNull { el ->
                    val obj = el.takeIf { it.isJsonObject }?.asJsonObject ?: return@mapNotNull null
                    val type = obj.get("type")?.asString.orEmpty()
                    if (type.contains("encrypted", ignoreCase = true)) return@mapNotNull null
                    obj.get("text")?.asString ?: obj.get("summary")?.asString
                }.joinToString("").ifEmpty { null }
            }

        return StreamResponseData(
            content = delta.content,
            reasoningContent = reasoningChunk,
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
            cost = streamResponse.usage?.cost
                ?: root?.getAsJsonObject("usage")?.get("cost")?.takeIf { it.isJsonPrimitive }?.asDouble,
            reasoningDetailsChunk = detailsChunk
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
        reasoningDetails: JsonArray?
    ): ChatCompletionResponse {
        val providerId = getProviderInfo().name
        val normalized = ReasoningResponseNormalizer.normalize(
            content = accumulatedContent.ifEmpty { null },
            reasoningContent = accumulatedReasoningContent?.takeIf { it.isNotEmpty() },
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
        return OpenRouterToolDefinition(
            type = type,
            function = func as OpenRouterFunctionDefinition
        )
    }

    override fun createFunctionDefinition(name: String, desc: String, params: JsonObject): Any {
        return OpenRouterFunctionDefinition(
            name = name,
            description = desc,
            parameters = normalizeParametersSchema(params)
        )
    }

    /**
     * 规范化 function parameters 为 OpenRouter/OpenAI 要求的 JSON Schema：
     * 必须包含 "type": "object" 和 "properties"（可为空），否则会报 invalid_function_parameters。
     */
    private fun normalizeParametersSchema(params: JsonObject): JsonObject {
        val hasType = params.has("type")
        val hasProperties = params.has("properties")
        if (hasType && hasProperties) return params
        val result = JsonParser().parse(GsonHelper.getInstance().toJson(params)).asJsonObject
        if (!hasType) result.addProperty("type", "object")
        if (!hasProperties) result.add("properties", JsonObject())
        return result
    }

    /**
     * 从 OpenRouter 获取支持 tool/function calling 的模型 ID 集合（即有 agent 能力的模型）。
     * 使用官方 API 筛选参数：supported_parameters=tools
     */
    private suspend fun fetchToolCapableModelIds(): Set<String> = withContext(Dispatchers.IO) {
        if (getApiKey().isEmpty()) return@withContext emptySet()
        val url = getProviderInfo().apiUrl + "/api/v1/models?supported_parameters=tools"
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.apply {
                requestMethod = "GET"
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "application/json")
                setRequestProperty("Authorization", "Bearer ${getApiKey()}")
                setRequestProperty("User-Agent", "Automio/1.0")
                connectTimeout = 10000
                readTimeout = 10000
            }
            if (connection.responseCode == 200) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val openRouterResponse = GsonHelper.getInstance().fromJson(response, OpenRouterModelsResponse::class.java)
                return@withContext openRouterResponse.data.map { it.id }.toSet()
            }
        } catch (e: Exception) {
            DLog.e("OpenRouterProvider", "获取支持 tools 的模型列表失败: ${e.message}")
        } finally {
            connection.disconnect()
        }
        emptySet()
    }

    /**
     * 从OpenRouter API获取模型列表
     * 通过 supported_parameters=tools 区分哪些模型具备 agent（function/tool calling）能力
     */
    private suspend fun fetchModelsFromAPI(): List<ModelInfo> {
        if (getApiKey().isEmpty()) {
            return mutableListOf()
        }
        val toolCapableIds = fetchToolCapableModelIds()
        val reasoningById = fetchReasoningMetadataByModelId()
        val url = getProviderInfo().apiUrl + "/api/v1/models/user"
        val connection = URL(url).openConnection() as HttpURLConnection

        return try {
            connection.apply {
                requestMethod = "GET"
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "application/json")
                setRequestProperty("Authorization", "Bearer ${getApiKey()}")
                setRequestProperty("User-Agent", "Automio/1.0")
                connectTimeout = 15000
                readTimeout = 15000
            }

            val responseCode = connection.responseCode

            if (responseCode == 200) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val openRouterResponse = GsonHelper.getInstance().fromJson(response, OpenRouterModelsResponse::class.java)

                return openRouterResponse.data.mapNotNull { model ->
                    try {
                        val supportsVision =
                            model.architecture?.input_modalities?.contains("image") == true
                        val contextWindow =
                            model.context_length ?: model.top_provider?.context_length ?: 8192
                        val supportsFunctionCall = model.id in toolCapableIds
                        val reasoningJson = model.reasoning ?: reasoningById[model.id]

                        ModelInfo(
                            modelId = model.id,
                            displayName = model.name,
                            providerId = "openrouter",
                            buildIn = true,
                            capabilities = ModelCapabilities(
                                supportsFunctionCall = supportsFunctionCall,
                                supportsVision = supportsVision,
                                contextWindow = contextWindow,
                                modelType = ModelType.CHAT,
                                reasoning = OpenRouterReasoningMetadataParser.toCapabilities(reasoningJson)
                            )
                        )
                    } catch (e: Exception) {
                        DLog.e("OpenRouterProvider", "解析模型 ${model.id} 失败: ${e.message}")
                        null
                    }
                }.filter { it.capabilities.supportsFunctionCall }
            } else {
                val errorStream = connection.errorStream
                val errorText = errorStream?.bufferedReader()?.use { it.readText() } ?: "未知错误"
                DLog.e(
                    "OpenRouterProvider",
                    "API请求失败，状态码: $responseCode, 错误信息: $errorText"
                )
                emptyList()
            }
        } catch (e: Exception) {
            DLog.e("OpenRouterProvider", "请求OpenRouter API失败: ${e.message}")
            emptyList()
        } finally {
            connection.disconnect()
        }
    }

    /**
     * `/api/v1/models` 暴露完整 `reasoning` 元数据；与 `/models/user` 列表按 id 合并。
     */
    private suspend fun fetchReasoningMetadataByModelId(): Map<String, JsonObject> =
        withContext(Dispatchers.IO) {
            if (getApiKey().isEmpty()) return@withContext emptyMap()
            val url = getProviderInfo().apiUrl + "/api/v1/models"
            val connection = URL(url).openConnection() as HttpURLConnection
            try {
                connection.apply {
                    requestMethod = "GET"
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("Authorization", "Bearer ${getApiKey()}")
                    setRequestProperty("User-Agent", "Automio/1.0")
                    connectTimeout = 15000
                    readTimeout = 15000
                }
                if (connection.responseCode != 200) return@withContext emptyMap()
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val openRouterResponse =
                    GsonHelper.getInstance().fromJson(response, OpenRouterModelsResponse::class.java)
                openRouterResponse.data.mapNotNull { model ->
                    model.reasoning?.let { model.id to it }
                }.toMap()
            } catch (e: Exception) {
                DLog.e("OpenRouterProvider", "获取 reasoning 元数据失败: ${e.message}")
                emptyMap()
            } finally {
                connection.disconnect()
            }
        }


}

// OpenRouter API数据模型 - 与 OpenRouter API 的 JSON 结构保持一致
private data class OpenRouterChatRequest(
    val model: String,
    val messages: List<JsonObject>,
    val temperature: Float = 0.7f,
    val max_tokens: Int = 2000,
    val stream: Boolean = false,
    val tools: List<OpenRouterToolDefinition>? = null
)

private data class OpenRouterMessage(
    val role: String? = null,
    val content: String? = null,
    val reasoning: String? = null,
    val reasoning_content: String? = null,
    val tool_calls: List<OpenRouterToolCall>? = null,
    val tool_call_id: String? = null,
    val tool_result: String? = null
)

private data class OpenRouterToolDefinition(
    val type: String = "function",
    val function: OpenRouterFunctionDefinition
)

private data class OpenRouterFunctionDefinition(
    val name: String,
    val description: String,
    val parameters: JsonObject
)

private data class OpenRouterToolCall(
    val id: String? = null,
    val index: Int? = null,
    val type: String? = null,
    val function: OpenRouterFunctionCall
)

private data class OpenRouterFunctionCall(
    val name: String? = null,
    val arguments: String? = null
)

private data class OpenRouterChatResponse(
    val id: String,
    val `object`: String,
    val created: Long,
    val model: String,
    val choices: List<OpenRouterChoice>? = null,
    val usage: OpenRouterUsage? = null
)

private data class OpenRouterChoice(
    val index: Int,
    val message: OpenRouterMessage,
    val finish_reason: String? = null
)

private data class OpenRouterUsage(
    val prompt_tokens: Int,
    val completion_tokens: Int,
    val total_tokens: Int,
    val cost: Double? = null,
    val reasoning_tokens: Int? = null
)

// 流式响应数据模型
private data class OpenRouterStreamResponse(
    val id: String,
    val created: Long,
    val model: String,
    val choices: List<OpenRouterStreamChoice>? = null,
    val usage: OpenRouterUsage? = null
)

private data class OpenRouterStreamChoice(
    val index: Int,
    val delta: OpenRouterMessage,
    val finish_reason: String? = null
)
