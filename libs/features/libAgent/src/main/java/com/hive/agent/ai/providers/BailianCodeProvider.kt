// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.agent.ai.providers

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
import com.hive.utils.file.FileUtils
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.hive.utils.GlobalApp
import com.hive.utils.debug.DLog
import com.hive.utils.extends.string
import com.hive.utils.utils.GsonHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * 阿里云百炼 Coding Plan 专用 Provider
 *
 * 说明：
 * - 兼容 OpenAI API 协议：
 *   - Base URL: https://coding.dashscope.aliyuncs.com/v1
 *   - API Key: Coding Plan 套餐专属 API Key (https://bailian.console.aliyun.com/cn-beijing/?tab=model#/efm/coding_plan)
 *   - Model: 详见 https://help.aliyun.com/zh/model-studio/coding-plan-overview#b01f82a4218kx
 *
 * - 兼容 Anthropic API 协议：
 *   - Base URL: https://coding.dashscope.aliyuncs.com/apps/anthropic
 *   - API Key: 同上
 *   - Model: 同上
 *
 * 本 Provider 使用 OpenAI 兼容接口实现。
 */
class BailianCodeProvider : AbstractChatProvider() {

    companion object {
        private const val CODING_PLAN_KEY_PREFIX = "sk-sp-"
        /** OpenAI 兼容 Base URL */
        private const val CODING_PLAN_API_BASE = "https://coding.dashscope.aliyuncs.com/v1"
        /** Anthropic 兼容 Base URL（备用） */
        private const val CODING_PLAN_ANTHROPIC_BASE = "https://coding.dashscope.aliyuncs.com/apps/anthropic"
        private const val DEFAULT_MODEL = "qwen3.5-plus"
        private const val CACHE_DURATION = 24 * 60 * 60 * 1000L // 24小时缓存
        var cachedModels: List<ModelInfo>? = null
        var cacheTimestamp: Long = 0L
    }

    override fun getDefaultModelId(): String? = DEFAULT_MODEL

    override fun getDefaultMultiModelId(): String? = DEFAULT_MODEL

    override fun getProviderInfo(): ProviderInfo {
        return ConfigAgentModels.findProviderInfo("bailian_code") ?: ProviderInfo(
            name = "bailian_code",
            displayName = GlobalApp.getString(com.hive.i8n.R.string.ai_provider_bailian_code),
            description = GlobalApp.getString(com.hive.i8n.R.string.ai_provider_bailian_code_desc),
            defaultModelId = DEFAULT_MODEL,
            defaultMultiModelId = DEFAULT_MODEL,
            isEnabled = true,
            tags = listOf("LLM", "CodePlan"),
            apiKeyPrefix = CODING_PLAN_KEY_PREFIX,
            apiKeyValidateMsg = com.hive.i8n.R.string.api_key_validation_bailian_code.string(),
            apiUrl = CODING_PLAN_API_BASE,
            sortIndex = 850
        )
    }

    override fun getChatUrl(): String = "$CODING_PLAN_API_BASE/chat/completions"

    /**
     * 百炼 Coding Plan 使用思考模型（如 qwen3.5-plus），推理阶段可能较长，增大读取超时
     */
    override fun getReadTimeout(): Int = 180_000 // 3 分钟

    /**
     * 动态获取模型列表，优先从 API 拉取，失败时使用兜底默认模型
     */
    override suspend fun getBuildInModels(): List<ModelInfo> = withContext(Dispatchers.IO) {
        try {
            if (isCacheValid()) {
                val cached = getCachedModels()
                return@withContext cached!!
            }
            val models = fetchModelsFromAPI().sortedBy { it.displayName }
            if (models.isNotEmpty()) {
                cacheModels(models)
                return@withContext models
            } else {
                DLog.w("BailianCodeProvider", "API返回空模型列表，使用默认模型")
                return@withContext getDefaultModels()
            }
        } catch (e: Exception) {
            DLog.e("BailianCodeProvider", "获取模型列表失败: ${e.message}")
            return@withContext getDefaultModels()
        }
    }

    private fun getCachedModels(): List<ModelInfo>? = cachedModels

    private fun isCacheValid(): Boolean =
        cachedModels != null && (System.currentTimeMillis() - cacheTimestamp) < CACHE_DURATION

    private fun cacheModels(models: List<ModelInfo>) {
        cachedModels = models
        cacheTimestamp = System.currentTimeMillis()
    }

    fun clearModelCache() {
        cachedModels = null
        cacheTimestamp = 0L
    }

    /**
     * 从 Coding Plan API 获取模型列表
     * OpenAI 兼容: GET /v1/models
     */
    private suspend fun fetchModelsFromAPI(): List<ModelInfo> {
        if (getApiKey().isEmpty()) return emptyList()
        val url = "$CODING_PLAN_API_BASE/models"
        val connection = URL(url).openConnection() as HttpURLConnection
        return try {
            connection.apply {
                requestMethod = "GET"
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "Automio/1.0")
                val apiKey = getApiKey()
                if (apiKey.isNotEmpty()) setRequestProperty("Authorization", "Bearer $apiKey")
                connectTimeout = 15000
                readTimeout = 15000
            }
            if (connection.responseCode == 200) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val resp = GsonHelper.getInstance().fromJson(response, BailianModelsResponse::class.java)
                resp.data?.mapNotNull { model ->
                    try {
                        ModelInfo(
                            modelId = model.id,
                            displayName = getDisplayNameForModel(model.id),
                            providerId = "bailian_code",
                            buildIn = true,
                            capabilities = ModelCapabilities(
                                supportsFunctionCall = true,
                                supportsVision = model.id.contains("vl") || model.id.contains("kimi") || model.id.contains("vision"),
                                contextWindow = 1_000_000,
                                modelType = ModelType.CHAT
                            )
                        )
                    } catch (e: Exception) {
                        DLog.e("BailianCodeProvider", "解析模型 ${model.id} 失败: ${e.message}")
                        null
                    }
                } ?: emptyList()
            } else {
                val errorText = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "未知错误"
                DLog.e("BailianCodeProvider", "API请求失败: ${connection.responseCode}, $errorText")
                emptyList()
            }
        } catch (e: Exception) {
            DLog.e("BailianCodeProvider", "请求 Coding Plan API 失败: ${e.message}")
            emptyList()
        } finally {
            connection.disconnect()
        }
    }

    private fun getDisplayNameForModel(modelId: String): String = when (modelId) {
        "qwen3.5-plus" -> GlobalApp.getString(com.hive.i8n.R.string.ai_model_qwen35_plus)
        "qwen3-coder-plus" -> "Qwen3 Coder Plus"
        "qwen3-coder-next" -> "Qwen3 Coder Next"
        "kimi-k2.5" -> "Kimi K2.5"
        "glm-5" -> "GLM-5"
        "MiniMax-M2.5" -> "MiniMax M2.5"
        "glm-4.7" -> "GLM-4.7"
        else -> modelId.split("-").joinToString(" ") { it.replaceFirstChar { c -> c.uppercaseChar() } }
    }

    /**
     * 兜底默认模型（当 API 请求失败或返回空时使用）
     */
    private fun getDefaultModels(): List<ModelInfo> = listOf(
        buildModel("qwen3.5-plus", GlobalApp.getString(com.hive.i8n.R.string.ai_model_qwen35_plus), true),
        buildModel("kimi-k2.5", "Kimi K2.5", true),
        buildModel("glm-5", "GLM-5", false),
        buildModel("MiniMax-M2.5", "MiniMax M2.5", false),
        buildModel("qwen3-max-2026-01-23", "Qwen3 Max", false),
        buildModel("qwen3-coder-next", "Qwen3 Coder Next", false),
        buildModel("qwen3-coder-plus", "Qwen3 Coder Plus", false),
        buildModel("glm-4.7", "GLM-4.7", false)
    )

    private fun buildModel(modelId: String, displayName: String, supportsVision: Boolean): ModelInfo =
        ModelInfo(
            modelId = modelId,
            displayName = displayName,
            providerId = "bailian_code",
            buildIn = true,
            capabilities = ModelCapabilities(
                supportsFunctionCall = true,
                supportsVision = supportsVision,
                contextWindow = 1_000_000,
                modelType = ModelType.CHAT
            )
        )

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
        val validatedMessages = validateAndFixMessageSequence(messages)
        val jsonMessages = buildBailianMessages(validatedMessages)

        val request = BailianChatRequest(
            model = model.ifEmpty { DEFAULT_MODEL },
            messages = jsonMessages,
            temperature = temperature,
            max_tokens = maxTokens,
            stream = stream,
            tools = tools?.map { it as BailianToolDefinition }
        )
        val json = GsonHelper.getInstance().getGson().toJsonTree(request).asJsonObject
        if (reasoning != null) {
            ReasoningRequestMapper.applyForProvider(json, getProviderInfo().name, reasoning)
        }
        return GsonHelper.getInstance().toJson(json)
    }

    private suspend fun buildBailianMessages(messages: List<ChatMessage>): List<JsonObject> {
        val result = mutableListOf<JsonObject>()
        var i = 0
        while (i < messages.size) {
            val msg = messages[i]
            when (msg.role) {
                MessageRole.TOOL -> {
                    val toolMessages = mutableListOf<ChatMessage>()
                    while (i < messages.size && messages[i].role == MessageRole.TOOL) {
                        toolMessages.add(messages[i])
                        i++
                    }
                    val deferredImageMessages = mutableListOf<JsonObject>()
                    toolMessages.forEach { toolMsg ->
                        result.add(JsonObject().apply {
                            addProperty("role", "tool")
                            addProperty("content", toolMsg.toolCallResult ?: toolMsg.content ?: "")
                            toolMsg.toolCallId?.takeIf { it.isNotEmpty() }?.let { addProperty("tool_call_id", it) }
                        })
                        if (toolMsg.attachments.isNotEmpty()) {
                            val msgForImage = toolMsg.copy(content = "工具执行的图片信息如下：")
                            val contentWithImage = buildMessageContent(msgForImage, true)
                            deferredImageMessages.add(JsonObject().apply {
                                addProperty("role", "user")
                                add("content", contentWithImage)
                            })
                        }
                    }
                    result.addAll(deferredImageMessages)
                    continue
                }
                else -> {
                    val hasImageAttachment = msg.attachments.any { it.type == AttachmentType.IMAGE }
                    val contentElement = buildMessageContent(msg, hasImageAttachment)
                    result.add(JsonObject().apply {
                        addProperty("role", msg.role.name.lowercase())
                        add("content", contentElement)
                        if (msg.role == MessageRole.ASSISTANT) {
                            msg.toolCalls?.takeIf { it.isNotEmpty() }?.let { add("tool_calls", buildToolCalls(it)) }
                        }
                    })
                }
            }
            i++
        }
        return result
    }

    private suspend fun buildMessageContent(
        message: ChatMessage,
        withAttachment: Boolean
    ): com.google.gson.JsonElement {
        val imageAttachment = message.attachments.firstOrNull { it.type == AttachmentType.IMAGE }
        return if (imageAttachment != null && withAttachment) {
            val imageUrl = processImageAttachment(imageAttachment)
            if (imageUrl != null) {
                // 百炼 API 官方示例：image 在前、text 在后
                JsonArray().apply {
                    add(JsonObject().apply {
                        addProperty("type", "image_url")
                        add("image_url", JsonObject().apply {
                            addProperty("url", imageUrl)
                        })
                    })
                    add(JsonObject().apply {
                        addProperty("type", "text")
                        addProperty("text", message.content ?: "")
                    })
                }
            } else {
                com.google.gson.JsonPrimitive(message.content ?: "")
            }
        } else {
            com.google.gson.JsonPrimitive(message.content ?: "")
        }
    }

    private suspend fun processImageAttachment(
        attachment: com.hive.plugin.agent.model.ChatAttachment
    ): String? = withContext(Dispatchers.IO) {
        if (!attachment.base64.isNullOrEmpty()) return@withContext attachment.base64
        val url = attachment.url ?: return@withContext null
        when {
            url.startsWith("data:") -> url
            isLocalFilePath(url) -> {
                FileUtils.convertLocalFileToBase64(url, attachment.mimeType)
            }
            url.startsWith("http://") || url.startsWith("https://") -> url
            else -> FileUtils.convertLocalFileToBase64(url, attachment.mimeType)
        }
    }

    private fun isLocalFilePath(path: String): Boolean =
        path.startsWith("/") ||
            path.startsWith("file://") ||
            path.startsWith("content://") ||
            !path.contains("://")

    /**
     * 使用紧邻规则校验：assistant 的 tool_calls 必须有紧接其后的 tool 响应，且 tool_call_id 集合完全匹配。
     */
    private fun validateAndFixMessageSequence(messages: List<ChatMessage>): List<ChatMessage> {
        if (messages.isEmpty()) return messages

        val result = mutableListOf<ChatMessage>()
        var index = 0
        while (index < messages.size) {
            val message = messages[index]
            if (message.role == MessageRole.ASSISTANT && !message.toolCalls.isNullOrEmpty()) {
                val expectedIds = message.toolCalls!!
                    .map { it.id }
                    .filter { it.isNotEmpty() }
                    .toSet()
                val toolResponses = mutableListOf<ChatMessage>()
                var cursor = index + 1
                while (cursor < messages.size && messages[cursor].role == MessageRole.TOOL) {
                    toolResponses.add(messages[cursor])
                    cursor++
                }
                val responseIds = toolResponses.mapNotNull { it.toolCallId?.takeIf { id -> id.isNotEmpty() } }.toSet()
                if (expectedIds.isNotEmpty() && expectedIds == responseIds) {
                    result.add(message)
                    result.addAll(toolResponses)
                } else {
                    result.add(message.copy(toolCalls = null))
                }
                index = cursor
                continue
            }
            if (message.role == MessageRole.TOOL) {
                index++
                continue
            }
            result.add(message)
            index++
        }
        return result
    }

    override fun parseChatResponse(responseText: String): ChatCompletionResponse {
        val root = JsonParser().parse(responseText).asJsonObject
        val resp = GsonHelper.getInstance().fromJson(responseText, BailianChatResponse::class.java)
        val firstChoice = resp.choices?.firstOrNull()
        val message = firstChoice?.message
        val providerId = getProviderInfo().name
        val usage = ReasoningResponseNormalizer.extractUsage(root.getAsJsonObject("usage"))
        val normalized = ReasoningResponseNormalizer.normalize(
            content = message?.content,
            reasoningContent = message?.reasoning_content,
            usage = usage,
            providerId = providerId,
            modelId = resp.model,
            replayFormat = ReasoningResponseNormalizer.replayFormatFor(providerId)
        )
        return ChatCompletionResponse(
            content = normalized.content,
            reasoningContent = normalized.reasoningContent,
            model = resp.model ?: "",
            usage = normalized.usage,
            toolCalls = message?.tool_calls?.mapNotNull { tc ->
                AgentToolCallUtils.buildToolCall("BailianCodeProvider", tc.id, tc.type, tc.function?.name, tc.function?.arguments)
            },
            reasoningTrace = normalized.reasoningTrace
        )
    }

    override fun parseStreamResponse(data: String): StreamResponseData {
        val root = runCatching { JsonParser().parse(data).asJsonObject }.getOrNull()
        val resp = GsonHelper.getInstance().fromJson(data, BailianStreamResponse::class.java)
        val choice = resp.choices?.firstOrNull() ?: return StreamResponseData(null, null, null, null, null, null, null)
        val delta = choice.delta ?: return StreamResponseData(null, null, null, null, null, null, null)
        return StreamResponseData(
            content = delta.content,
            reasoningContent = delta.reasoning_content,
            model = resp.model,
            usage = ReasoningResponseNormalizer.extractUsage(root?.getAsJsonObject("usage")),
            toolCalls = delta.tool_calls?.mapNotNull { tc ->
                val fn = tc.function ?: return@mapNotNull null
                ToolCallData(tc.index, tc.id, tc.type, fn.name, fn.arguments)
            },
            finishReason = choice.finish_reason,
            cost = null
        )
    }

    override fun isStreamResponseComplete(finishReason: String?): Boolean =
        finishReason != null && finishReason != "null"

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

    override fun createToolDefinition(type: String, func: Any): Any =
        BailianToolDefinition(type = type, function = func as BailianFunctionDefinition)

    override fun createFunctionDefinition(name: String, desc: String, params: JsonObject): Any {
        val result = JsonParser().parse(GsonHelper.getInstance().toJson(params)).asJsonObject
        if (!result.has("type")) result.addProperty("type", "object")
        if (!result.has("properties")) result.add("properties", JsonObject())
        return BailianFunctionDefinition(name, desc, result)
    }
}

private data class BailianModelsResponse(
    val `object`: String? = null,
    val data: List<BailianModelData>? = null
)

private data class BailianModelData(
    val id: String,
    val `object`: String? = null,
    val owned_by: String? = null
)

private data class BailianChatRequest(
    val model: String,
    val messages: List<JsonObject>,
    val temperature: Float = 0.7f,
    val max_tokens: Int = 2000,
    val stream: Boolean = false,
    val tools: List<BailianToolDefinition>? = null
)

private data class BailianToolDefinition(val type: String = "function", val function: BailianFunctionDefinition)
private data class BailianFunctionDefinition(val name: String, val description: String, val parameters: JsonObject)

private data class BailianMessage(
    val role: String? = null,
    val content: String? = null,
    val reasoning_content: String? = null,
    val tool_calls: List<BailianToolCall>? = null,
    val tool_call_id: String? = null
)

private data class BailianToolCall(
    val id: String? = null,
    val index: Int? = null,
    val type: String? = null,
    val function: BailianFunctionCall? = null
)

private data class BailianFunctionCall(val name: String? = null, val arguments: String? = null)

private data class BailianChatResponse(
    val id: String? = null,
    val `object`: String? = null,
    val created: Long = 0L,
    val model: String? = null,
    val choices: List<BailianChoice>? = null,
    val usage: BailianUsage? = null
)

private data class BailianChoice(val index: Int = 0, val message: BailianMessage? = null, val finish_reason: String? = null)
private data class BailianUsage(val prompt_tokens: Int, val completion_tokens: Int, val total_tokens: Int)

private data class BailianStreamResponse(
    val id: String? = null,
    val created: Long = 0L,
    val model: String? = null,
    val choices: List<BailianStreamChoice>? = null,
    val usage: BailianUsage? = null
)

private data class BailianStreamChoice(val index: Int = 0, val delta: BailianMessage? = null, val finish_reason: String? = null)
