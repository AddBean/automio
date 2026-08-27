// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.agent.ai.providers

import com.hive.agent.config.ConfigAgentModels
import com.hive.agent.utils.AgentToolCallUtils
import com.hive.plugin.agent.ModelCapabilities
import com.hive.plugin.agent.ModelInfo
import com.hive.plugin.agent.ModelType
import com.hive.plugin.agent.ProviderInfo
import com.hive.plugin.agent.model.AttachmentType
import com.hive.plugin.agent.model.ChatCompletionResponse
import com.hive.plugin.agent.model.ChatMessage
import com.hive.plugin.agent.model.MessageRole
import com.hive.utils.extends.string
import com.hive.utils.file.FileUtils
import com.hive.utils.utils.GsonHelper
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.JsonPrimitive
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * OpenAI 兼容 Provider 实现。
 * 其他兼容 OpenAI Chat Completions / SSE 协议的 Provider 可直接继承。
 */
open class OpenAIProvider : AbstractChatProvider() {

    override fun getProviderInfo(): ProviderInfo {
        return ConfigAgentModels.findProviderInfo("openai") ?: ProviderInfo(
            name = "openai",
            displayName = "OpenAI",
            description = "OpenAI GPT",
            defaultModelId = "gpt-4o-mini",
            defaultMultiModelId = "gpt-4o",
            isEnabled = true,
            tags = listOf("LLM", "OpenAI-Compatible"),
            apiKeyPrefix = "",
            apiKeyValidateMsg = com.hive.i8n.R.string.api_key_validation_openai.string(),
            apiUrl = "https://api.openai.com/v1",
            sortIndex = 700
        )
    }

    override fun supportsEditableBaseUrl(): Boolean = true

    protected open fun resolveEffectiveBaseUrl(): String {
        return OpenAiUrlHelper.resolveBaseUrl(
            override = serviceManager?.getProviderBaseUrl(getProviderInfo().name),
            defaultApiUrl = getProviderInfo().apiUrl
        )
    }

    override fun getChatUrl(): String {
        return OpenAiUrlHelper.chatCompletionsUrl(resolveEffectiveBaseUrl())
    }

    protected open suspend fun buildMessageContent(
        message: ChatMessage,
        withAttachment: Boolean
    ): com.google.gson.JsonElement {
        val imageAttachment = message.attachments.firstOrNull { it.type == AttachmentType.IMAGE }
        return if (imageAttachment != null && withAttachment) {
            val imageUrl = processImageAttachment(imageAttachment)
            if (imageUrl != null) {
                JsonArray().apply {
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
            } else {
                JsonPrimitive(message.content ?: "")
            }
        } else {
            JsonPrimitive(message.content ?: "")
        }
    }

    protected open suspend fun processImageAttachment(
        attachment: com.hive.plugin.agent.model.ChatAttachment
    ): String? {
        return withContext(Dispatchers.IO) {
            if (!attachment.base64.isNullOrEmpty()) {
                return@withContext attachment.base64
            }
            val url = attachment.url ?: return@withContext null
            return@withContext when {
                url.startsWith("data:") -> url
                isLocalFilePath(url) -> {
                    attachment.base64 = FileUtils.convertLocalFileToBase64(url, attachment.mimeType)
                    attachment.base64
                }

                url.startsWith("http://") || url.startsWith("https://") -> url
                else -> FileUtils.convertLocalFileToBase64(url, attachment.mimeType)
            }
        }
    }

    protected open fun isLocalFilePath(path: String): Boolean {
        return path.startsWith("/") ||
            path.startsWith("file://") ||
            path.startsWith("content://") ||
            !path.contains("://")
    }

    protected open fun buildToolCalls(
        toolCalls: List<com.hive.plugin.agent.model.ToolCall>
    ): JsonArray {
        return JsonArray().apply {
            toolCalls.forEach { toolCall ->
                add(JsonObject().apply {
                    addProperty("id", toolCall.id)
                    addProperty("type", toolCall.type)
                    add("function", JsonObject().apply {
                        addProperty("name", toolCall.function.name)
                        addProperty(
                            "arguments",
                            GsonHelper.getInstance().toJson(toolCall.function.arguments)
                        )
                    })
                })
            }
        }
    }

    protected open suspend fun buildOpenAIMessage(message: ChatMessage): List<JsonObject> {
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
            val imageMessage = message.copy(content = "Tool output image:")
            list.add(JsonObject().apply {
                addProperty("role", MessageRole.USER.name.lowercase())
                add("content", buildMessageContent(imageMessage, true))
            })
        }
        return list
    }

    /**
     * 将 ChatMessage 转为 API messages，保证同一批 tool_calls 的 tool 响应连续，
     * 工具附件对应的 user 图片消息延后到整批 tool 之后，避免打断 tool_call_id 配对。
     */
    protected open suspend fun buildOpenAIMessages(messages: List<ChatMessage>): List<JsonObject> {
        val result = mutableListOf<JsonObject>()
        var index = 0
        while (index < messages.size) {
            val message = messages[index]
            if (message.role == MessageRole.ASSISTANT && !message.toolCalls.isNullOrEmpty()) {
                result.addAll(buildOpenAIMessage(message))
                index++
                val deferredImageMessages = mutableListOf<JsonObject>()
                while (index < messages.size && messages[index].role == MessageRole.TOOL) {
                    val built = buildOpenAIMessage(messages[index])
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
            result.addAll(buildOpenAIMessage(message))
            index++
        }
        return result
    }

    override suspend fun buildChatRequest(
        model: String,
        messages: List<ChatMessage>,
        temperature: Float,
        maxTokens: Int,
        stream: Boolean,
        tools: List<Any>?
    ): String {
        val request = OpenAIChatRequest(
            model = model,
            messages = buildOpenAIMessages(messages),
            temperature = temperature,
            max_tokens = maxTokens,
            stream = stream,
            tools = tools?.map { it as OpenAIToolDefinition }
        )
        return GsonHelper.getInstance().toJson(request)
    }

    override fun parseChatResponse(responseText: String): ChatCompletionResponse {
        val response =
            GsonHelper.getInstance().fromJson(responseText, OpenAIChatResponse::class.java)
        val firstChoice = response.choices?.firstOrNull()
        val message = firstChoice?.message
        return ChatCompletionResponse(
            content = message?.content,
            reasoningContent = message?.reasoning_content?.takeIf { it.isNotEmpty() },
            model = response.model,
            usage = response.usage?.let { usage ->
                mapOf(
                    "prompt_tokens" to usage.prompt_tokens,
                    "completion_tokens" to usage.completion_tokens,
                    "total_tokens" to usage.total_tokens
                )
            } ?: emptyMap(),
            toolCalls = message?.tool_calls?.mapNotNull { toolCall ->
                AgentToolCallUtils.buildToolCall(
                    logTag = "OpenAIProvider",
                    id = toolCall.id,
                    type = toolCall.type,
                    functionName = toolCall.function.name,
                    arguments = toolCall.function.arguments
                )
            }
        )
    }

    override fun parseStreamResponse(data: String): StreamResponseData {
        val response =
            GsonHelper.getInstance().fromJson(data, OpenAIStreamResponse::class.java)
        val choice = response.choices?.firstOrNull() ?: return StreamResponseData(
            null, null, null, null, null, null, null
        )
        val delta = choice.delta
        return StreamResponseData(
            content = delta.content,
            reasoningContent = delta.reasoning_content,
            model = response.model,
            usage = response.usage?.let { usage ->
                mapOf(
                    "prompt_tokens" to usage.prompt_tokens,
                    "completion_tokens" to usage.completion_tokens,
                    "total_tokens" to usage.total_tokens
                )
            },
            toolCalls = delta.tool_calls?.map { toolCall ->
                ToolCallData(
                    index = toolCall.index,
                    id = toolCall.id,
                    type = toolCall.type,
                    functionName = toolCall.function.name,
                    arguments = toolCall.function.arguments
                )
            },
            finishReason = choice.finish_reason,
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
        cost: Double?
    ): ChatCompletionResponse {
        return ChatCompletionResponse(
            content = accumulatedContent.ifEmpty { null },
            reasoningContent = accumulatedReasoningContent?.takeIf { it.isNotEmpty() },
            model = model,
            usage = usage,
            toolCalls = toolCalls as? List<com.hive.plugin.agent.model.ToolCall>,
            cost = cost
        )
    }

    override fun createToolDefinition(type: String, func: Any): Any {
        return OpenAIToolDefinition(type = type, function = func as OpenAIFunctionDefinition)
    }

    override fun createFunctionDefinition(name: String, desc: String, params: JsonObject): Any {
        return OpenAIFunctionDefinition(
            name = name,
            description = desc,
            parameters = normalizeParametersSchema(params)
        )
    }

    protected open fun normalizeParametersSchema(params: JsonObject): JsonObject {
        val hasType = params.has("type")
        val hasProperties = params.has("properties")
        if (hasType && hasProperties) return params
        val result = JsonParser().parse(GsonHelper.getInstance().toJson(params)).asJsonObject
        if (!hasType) result.addProperty("type", "object")
        if (!hasProperties) result.add("properties", JsonObject())
        return result
    }

    override suspend fun getBuildInModels(): List<ModelInfo> = listOf(
        ModelInfo(
            modelId = "gpt-4o-mini",
            displayName = "GPT-4o Mini",
            providerId = getProviderInfo().name,
            buildIn = true,
            capabilities = ModelCapabilities(
                supportsFunctionCall = true,
                supportsVision = true,
                contextWindow = 128000,
                modelType = ModelType.CHAT
            )
        ),
        ModelInfo(
            modelId = "gpt-4o",
            displayName = "GPT-4o",
            providerId = getProviderInfo().name,
            buildIn = true,
            capabilities = ModelCapabilities(
                supportsFunctionCall = true,
                supportsVision = true,
                contextWindow = 128000,
                modelType = ModelType.CHAT
            )
        ),
        ModelInfo(
            modelId = "gpt-4.1",
            displayName = "GPT-4.1",
            providerId = getProviderInfo().name,
            buildIn = true,
            capabilities = ModelCapabilities(
                supportsFunctionCall = true,
                supportsVision = true,
                contextWindow = 128000,
                modelType = ModelType.CHAT
            )
        ),
        ModelInfo(
            modelId = "gpt-4.1-mini",
            displayName = "GPT-4.1 Mini",
            providerId = getProviderInfo().name,
            buildIn = true,
            capabilities = ModelCapabilities(
                supportsFunctionCall = true,
                supportsVision = true,
                contextWindow = 128000,
                modelType = ModelType.CHAT
            )
        )
    )
}

private data class OpenAIChatRequest(
    val model: String,
    val messages: List<JsonObject>,
    val temperature: Float = 0.7f,
    val max_tokens: Int = 2000,
    val stream: Boolean = false,
    val tools: List<OpenAIToolDefinition>? = null
)

private data class OpenAIMessage(
    val role: String? = null,
    val content: String? = null,
    val reasoning_content: String? = null,
    val tool_calls: List<OpenAIToolCall>? = null,
    val tool_call_id: String? = null,
    val tool_result: String? = null
)

private data class OpenAIToolDefinition(
    val type: String = "function",
    val function: OpenAIFunctionDefinition
)

private data class OpenAIFunctionDefinition(
    val name: String,
    val description: String,
    val parameters: JsonObject
)

private data class OpenAIToolCall(
    val id: String? = null,
    val index: Int? = null,
    val type: String? = null,
    val function: OpenAIFunctionCall
)

private data class OpenAIFunctionCall(
    val name: String? = null,
    val arguments: String? = null
)

private data class OpenAIChatResponse(
    val id: String? = null,
    val `object`: String? = null,
    val created: Long? = null,
    val model: String? = null,
    val choices: List<OpenAIChoice>? = null,
    val usage: OpenAIUsage? = null
)

private data class OpenAIChoice(
    val index: Int? = null,
    val message: OpenAIMessage,
    val finish_reason: String? = null
)

private data class OpenAIUsage(
    val prompt_tokens: Int = 0,
    val completion_tokens: Int = 0,
    val total_tokens: Int = 0
)

private data class OpenAIStreamResponse(
    val id: String? = null,
    val created: Long? = null,
    val model: String? = null,
    val choices: List<OpenAIStreamChoice>? = null,
    val usage: OpenAIUsage? = null
)

private data class OpenAIStreamChoice(
    val index: Int? = null,
    val delta: OpenAIMessage,
    val finish_reason: String? = null
)
