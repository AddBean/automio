// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.agent.ai.providers

import com.hive.agent.config.ConfigAgentModels
import com.hive.agent.utils.AgentToolCallUtils
import com.hive.plugin.agent.ProviderInfo
import com.hive.plugin.agent.ModelCapabilities
import com.hive.plugin.agent.ModelInfo
import com.hive.plugin.agent.ModelType
import com.hive.plugin.agent.model.ChatCompletionResponse
import com.hive.plugin.agent.model.MessageRole
import com.hive.utils.debug.DLog
import com.hive.utils.extends.string
import com.google.gson.JsonObject
import com.hive.utils.utils.GsonHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * Ollama AI Provider实现
 */
class OllamaProvider : AbstractChatProvider() {

    companion object {
        private const val CACHE_DURATION = 24 * 60 * 60 * 1000L // 24小时缓存
        var cachedModels: List<ModelInfo>? = null
        var cacheTimestamp: Long = 0L
    }


    override fun getProviderInfo(): ProviderInfo {
        return ConfigAgentModels.findProviderInfo("ollama") ?: ProviderInfo(
            name = "ollama",
            displayName = "Ollama",
            description = "Ollama AI",
            defaultModelId = "qwen3:0.6b",
            defaultMultiModelId = "gemini-1.5-turbo",
            isEnabled = true, // 这里不能调用isEnabled()，会造成循环调用
            tags = listOf("LLM"),
            apiKeyPrefix = "",
            apiKeyValidateMsg = com.hive.i8n.R.string.api_key_validation_ollama.string(),
            apikeyEnabled = false,
            apiUrl = "http://78.46.154.201:11434",
            sortIndex = 800
        )
    }

    override fun hasValidApiKey() = true

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
     * 获取基础URL
     */
    override fun getChatUrl(): String {
        return getProviderInfo().apiUrl + "/api/chat"
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
     * 从Ollama API获取模型列表，带缓存机制
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
                DLog.w("OllamaProvider", "API返回空模型列表，使用默认模型")
                return@withContext getDefaultModels()
            }
        } catch (e: Exception) {
            DLog.e("OllamaProvider", "获取模型列表失败: ${e.message}")
            // 返回默认模型
            return@withContext getDefaultModels()
        }
    }

    /**
     * 默认模型列表（当API请求失败时使用）
     */
    private fun getDefaultModels(): List<ModelInfo> = listOf(
        ModelInfo(
            modelId = "qwen3:0.6b",
            displayName = "Qwen3 0.6B",
            providerId = "ollama",
            buildIn = true,
            capabilities = ModelCapabilities(
                supportsFunctionCall = true,
                supportsVision = false,
                contextWindow = 4096,
                modelType = ModelType.CHAT
            )
        )
    )

    override fun getDefaultTemperature(): Float = 0.7f

    override fun getDefaultMaxTokens(): Int = 2048

    override suspend fun buildChatRequest(
        model: String,
        messages: List<com.hive.plugin.agent.model.ChatMessage>,
        temperature: Float,
        maxTokens: Int,
        stream: Boolean,
        tools: List<Any>?,
        reasoning: com.hive.plugin.agent.model.ReasoningOptions?
    ): String {
        val ollamaRequest = OllamaChatRequest(
            model = model,
            messages = messages.map { message ->
                OllamaMessage(
                    role = message.role.name.lowercase(),
                    content = when (message.role) {
                        MessageRole.TOOL -> message.toolCallResult ?: message.content
                        else -> message.content
                    },
                    tool_calls = if (message.role == MessageRole.ASSISTANT) {
                        message.toolCalls
                            ?.takeIf { it.isNotEmpty() }
                            ?.map { toolCall ->
                                OllamaToolCall(
                                    id = toolCall.id,
                                    type = toolCall.type,
                                    function = OllamaFunctionCall(
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
            options = OllamaOptions(
                temperature = temperature,
                num_predict = maxTokens
            ),
            stream = stream,
            tools = tools?.mapNotNull { tool ->
                when (tool) {
                    is OllamaToolDefinition -> tool
                    else -> null
                }
            }
        )
        return GsonHelper.getInstance().toJson(ollamaRequest)
    }

    override fun parseChatResponse(responseText: String): ChatCompletionResponse {
        val ollamaResponse = GsonHelper.getInstance().fromJson(responseText, OllamaChatResponse::class.java)
        val message = ollamaResponse.message

        return ChatCompletionResponse(
            content = message.content,
            reasoningContent = null, // Ollama 不支持 reasoning content
            model = ollamaResponse.model,
            usage = mapOf(
                "prompt_eval_count" to 0,
                "prompt_eval_duration" to 0,
                "eval_count" to 0,
                "eval_duration" to 0
            ),
            toolCalls = message.tool_calls?.mapNotNull { ollamaToolCall ->
                AgentToolCallUtils.buildToolCall(
                    logTag = "OllamaProvider",
                    id = ollamaToolCall.id ?: "tool_call_${System.currentTimeMillis()}",
                    type = ollamaToolCall.type ?: "function",
                    functionName = ollamaToolCall.function.name,
                    arguments = ollamaToolCall.function.arguments?.toString() ?: "{}"
                )
            }
        )
    }

    override fun parseStreamResponse(data: String): StreamResponseData {
        val streamResponse = GsonHelper.getInstance().fromJson(data, OllamaStreamResponse::class.java)
        val message = streamResponse.message
        val done = streamResponse.done

        return StreamResponseData(
            content = message.content,
            reasoningContent = null,
            model = streamResponse.model,
            usage = null,
            toolCalls = message.tool_calls?.map { toolCall ->
                ToolCallData(
                    index = toolCall.index ?: 0,
                    id = toolCall.id ?: "tool_call_${System.currentTimeMillis()}",
                    type = toolCall.type ?: "function",
                    functionName = toolCall.function.name,
                    arguments = toolCall.function.arguments?.toString() ?: "{}"
                )
            },
            finishReason = if (done == true) "stop" else null,
            cost = null
        )
    }

    override fun isStreamResponseComplete(finishReason: String?): Boolean {
        return finishReason == "stop"
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
        val (content, reasoningContent) = separateContentAndReasoning(accumulatedContent)

        return ChatCompletionResponse(
            content = content.ifEmpty { null },
            reasoningContent = reasoningContent?.ifEmpty { null },
            model = model,
            usage = usage,
            cost = cost,
            toolCalls = toolCalls as? List<com.hive.plugin.agent.model.ToolCall>
        )
    }

    /**
     * 从 accumulatedContent 中分离出 content 和 reasoningContent
     * reasoningContent 是从 <think></think> 或 <thinking></thinking> 标签中提取的内容
     * 也支持不完整的标签格式如 <think>xxx 或 <thinking>xxx
     */
    private fun separateContentAndReasoning(accumulatedContent: String): Pair<String, String?> {
        if (accumulatedContent.isEmpty()) {
            return Pair("", null)
        }

        // 定义可能的思考标签模式（完整标签）
        val completeThinkPatterns = listOf(
            Regex("<think>(.*?)</think>", RegexOption.DOT_MATCHES_ALL),
            Regex("<thinking>(.*?)</thinking>", RegexOption.DOT_MATCHES_ALL)
        )

        // 定义不完整的思考标签模式（只有开始标签）
        val incompleteThinkPatterns = listOf(
            Regex("<think>(.*)", RegexOption.DOT_MATCHES_ALL),
            Regex("<thinking>(.*)", RegexOption.DOT_MATCHES_ALL)
        )

        var content = accumulatedContent
        var reasoningContent: String? = null

        // 首先查找完整的标签
        for (pattern in completeThinkPatterns) {
            val match = pattern.find(accumulatedContent)
            if (match != null) {
                reasoningContent = match.groupValues[1].trim()
                // 从原始内容中移除思考标签及其内容
                content = pattern.replace(accumulatedContent, "").trim()
                break
            }
        }

        // 如果没有找到完整标签，查找不完整的标签
        if (reasoningContent == null) {
            for (pattern in incompleteThinkPatterns) {
                val match = pattern.find(accumulatedContent)
                if (match != null) {
                    reasoningContent = match.groupValues[1].trim()
                    // 对于不完整的标签，content 应该为空
                    content = ""
                    break
                }
            }
        }

        // 如果仍然没有找到，尝试查找分割模式
        if (reasoningContent == null) {
            val splitPatterns = listOf(
                Regex("</think>", RegexOption.DOT_MATCHES_ALL),
                Regex("</thinking>", RegexOption.DOT_MATCHES_ALL)
            )

            for (pattern in splitPatterns) {
                val matches = pattern.findAll(accumulatedContent)
                if (matches.count() > 0) {
                    // 找到分割点，提取分割后的内容作为 reasoningContent
                    val parts = pattern.split(accumulatedContent)
                    if (parts.size > 1) {
                        reasoningContent = parts[0].trim()
                        content = parts.drop(1).joinToString("").trim()
                    }
                    break
                }
            }
        }

        return Pair(content, reasoningContent)
    }

    override fun createToolDefinition(type: String, func: Any): Any {
        return OllamaToolDefinition(
            type = type,
            function = func as OllamaFunctionDefinition
        )
    }

    override fun createFunctionDefinition(name: String, desc: String, params: JsonObject): Any {
        return OllamaFunctionDefinition(
            name = name,
            description = desc,
            parameters = params
        )
    }


    /**
     * 从Ollama API获取模型列表
     */
    private suspend fun fetchModelsFromAPI(): List<ModelInfo> {
        val url = getProviderInfo().apiUrl + "/api/tags"
        val connection = URL(url).openConnection() as HttpURLConnection

        return try {
            connection.apply {
                requestMethod = "GET"
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "Automio/1.0")
                connectTimeout = 15000
                readTimeout = 15000
            }

            val responseCode = connection.responseCode

            if (responseCode == 200) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val ollamaResponse = GsonHelper.getInstance().fromJson(response, OllamaModelsResponse::class.java)

                return ollamaResponse.models.mapNotNull { model ->
                    try {
                        // 根据模型名称和详细信息判断能力
                        val modelName = model.name.lowercase()
                        val family = model.details?.family?.lowercase() ?: ""
                        val parameterSize = model.details?.parameter_size ?: ""

                        // 判断是否支持视觉功能
                        val supportsVision = modelName.contains("vision") ||
                                modelName.contains("multimodal") ||
                                modelName.contains("llava") ||
                                modelName.contains("bakllava") ||
                                modelName.contains("moondream") ||
                                modelName.contains("clip")

                        // 根据参数大小和模型名称估算上下文窗口
                        val contextWindow = when {
                            parameterSize.contains("1B") || parameterSize.contains("1.1B") -> 2048
                            parameterSize.contains("3B") || parameterSize.contains("3.8B") -> 4096
                            parameterSize.contains("7B") || parameterSize.contains("8B") -> 8192
                            parameterSize.contains("13B") || parameterSize.contains("15B") || parameterSize.contains(
                                "16B"
                            ) -> 16384

                            parameterSize.contains("32B") || parameterSize.contains("33B") || parameterSize.contains(
                                "34B"
                            ) -> 32768

                            parameterSize.contains("70B") || parameterSize.contains("72B") -> 65536
                            modelName.contains("qwen") -> 32768
                            modelName.contains("deepseek") -> 128000
                            modelName.contains("llama3") -> 128000
                            else -> 4096
                        }

                        // 生成更友好的显示名称
                        val displayName = model.name

                        ModelInfo(
                            modelId = model.name,
                            displayName = displayName,
                            providerId = "ollama",
                            buildIn = true,
                            capabilities = ModelCapabilities(
                                supportsFunctionCall = true,
                                supportsVision = supportsVision,
                                contextWindow = contextWindow,
                                modelType = ModelType.CHAT
                            )
                        )
                    } catch (e: Exception) {
                        DLog.e("OllamaProvider", "解析模型 ${model.name} 失败: ${e.message}")
                        null
                    }
                }
            } else {
                val errorStream = connection.errorStream
                val errorText = errorStream?.bufferedReader()?.use { it.readText() } ?: "未知错误"
                DLog.e(
                    "OllamaProvider",
                    "API请求失败，状态码: $responseCode, 错误信息: $errorText"
                )
                emptyList()
            }
        } catch (e: Exception) {
            DLog.e("OllamaProvider", "请求Ollama API失败: ${e.message}")
            emptyList()
        } finally {
            connection.disconnect()
        }
    }
}

private data class OllamaChatRequest(
    val model: String,
    val messages: List<OllamaMessage>,
    val options: OllamaOptions? = null,
    val stream: Boolean = false,
    val tools: List<OllamaToolDefinition>? = null
)

private data class OllamaMessage(
    val role: String? = null,
    val content: String? = null,
    val tool_calls: List<OllamaToolCall>? = null,
    val tool_call_id: String? = null
)

private data class OllamaOptions(
    val temperature: Float = 0.7f,
    val num_predict: Int = 2048,
    val top_k: Int? = null,
    val top_p: Float? = null,
    val repeat_penalty: Float? = null,
    val seed: Int? = null
)

private data class OllamaChatResponse(
    val model: String,
    val created_at: String,
    val message: OllamaMessage,
    val done: Boolean
    // Note: duration and count fields may not exist in stream responses, so they're not included here
)

// Ollama Function Calling 相关数据模型
private data class OllamaToolDefinition(
    val type: String = "function",
    val function: OllamaFunctionDefinition
)

private data class OllamaFunctionDefinition(
    val name: String,
    val description: String,
    val parameters: JsonObject
)

private data class OllamaToolCall(
    val id: String? = null,
    val index: Int? = null,
    val type: String? = null,
    val function: OllamaFunctionCall
)

private data class OllamaFunctionCall(
    val name: String? = null,
    val arguments: String? = null
)

// 流式响应数据模型 - 简化版本，只包含流式响应中实际存在的字段
private data class OllamaStreamResponse(
    val model: String,
    val created_at: String,
    val message: OllamaMessage,
    val done: Boolean
)

// Ollama API 模型列表响应数据模型
private data class OllamaModelsResponse(
    val models: List<OllamaModelData>
)

private data class OllamaModelData(
    val name: String,
    val model: String,
    val modified_at: String,
    val size: Long,
    val digest: String,
    val details: OllamaModelDetails? = null
)

private data class OllamaModelDetails(
    val parent_model: String? = null,
    val format: String? = null,
    val family: String? = null,
    val families: List<String>? = null,
    val parameter_size: String? = null,
    val quantization_level: String? = null
)
