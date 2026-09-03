// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.agent.ai.providers

import com.hive.agent.ai.StreamToolCallAccumulator
import com.hive.agent.utils.AgentToolMapping
import com.hive.plugin.agent.model.AIRequest
import com.hive.plugin.agent.model.AIRequestType
import com.hive.plugin.agent.model.AIResult
import com.hive.plugin.agent.model.AgentError
import com.hive.plugin.agent.model.AgentErrorCode
import com.hive.plugin.agent.model.AgentInput
import com.hive.plugin.agent.model.ChatCompletionResponse
import com.hive.plugin.agent.model.ChatMessage
import com.hive.plugin.agent.model.MessageRole
import com.hive.plugin.agent.model.AIErrorDetail
import com.hive.plugin.agent.model.NetworkErrorType
import com.hive.plugin.agent.model.ParseErrorType
import com.hive.plugin.agent.model.ReasoningOptions
import com.hive.utils.debug.DLog
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.hive.agent.utils.AgentMessageUtils
import com.hive.agent.utils.MessageStatusHelper
import com.hive.utils.extends.getJsonKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean
import com.hive.utils.GlobalApp
import com.hive.agent.ai.ReasoningResponseNormalizer

abstract class AbstractChatProvider : AbstractBaseProvider() {

    open fun getDefaultModelId(): String? = getProviderInfo().defaultModelId

    open fun getDefaultMultiModelId(): String? = getProviderInfo().defaultMultiModelId

    protected open fun getDefaultTemperature(): Float = 0.7f

    protected open fun getDefaultMaxTokens(): Int = 2000

    protected abstract suspend fun buildChatRequest(
        model: String,
        messages: List<ChatMessage>,
        temperature: Float,
        maxTokens: Int,
        stream: Boolean,
        tools: List<Any>?,
        reasoning: ReasoningOptions? = null
    ): String

    protected abstract fun parseChatResponse(responseText: String): ChatCompletionResponse

    protected abstract fun parseStreamResponse(data: String): StreamResponseData

    protected abstract fun isStreamResponseComplete(finishReason: String?): Boolean

    protected abstract fun buildStreamResponse(
        accumulatedContent: String,
        accumulatedReasoningContent: String?,
        model: String?,
        usage: Map<String, Int>,
        toolCalls: List<Any>?,
        cost: Double? = null,
        reasoningDetails: com.google.gson.JsonArray? = null
    ): ChatCompletionResponse

    override suspend fun <T> onInference(request: AIRequest): AIResult<T> {
        return withContext(Dispatchers.IO) {
            val requestId = generateRequestId(request)
            val shouldStop = AtomicBoolean(false)

            try {
                activeRequests[requestId] = shouldStop
                when (request.requestType) {
                    AIRequestType.CHAT_COMPLETION, AIRequestType.FUNCTION_CALL -> {
                        val response = performChatCompletion(request, shouldStop)
                        @Suppress("UNCHECKED_CAST")
                        AIResult.Success(response as T)
                    }
                }
            } catch (e: Exception) {
                // NEW: Classify network exception
                val aiErrorDetail = classifyNetworkException(e, getProviderInfo().name)
                AIResult.Failure(
                    AgentError.create(
                        code = aiErrorDetail?.let { AgentErrorCode.AI_NETWORK_ERROR }
                            ?: AgentErrorCode.AI_REQUEST_ERROR,
                        cause = e,
                        aiErrorDetail = aiErrorDetail
                    )
                )
            } finally {
                activeRequests.remove(requestId)
                activeConnections.remove(requestId)
            }
        }
    }

    override suspend fun <T> onStreamInference(
        request: AIRequest,
        onChunkResponse: ((ChatCompletionResponse) -> Unit)?
    ): AIResult<T> {
        val requestId = generateRequestId(request)
        val shouldStop = AtomicBoolean(false)
        try {
            activeRequests[requestId] = shouldStop
            when (request.requestType) {
                AIRequestType.CHAT_COMPLETION, AIRequestType.FUNCTION_CALL -> {
                    @Suppress("UNCHECKED_CAST")
                    return AIResult.Success(
                        performStreamChatCompletion(
                            request,
                            shouldStop,
                            requestId,
                            onChunkResponse
                        ) as T
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            activeRequests.remove(requestId)

            // NEW: Classify network exception
            val aiErrorDetail = classifyNetworkException(e, getProviderInfo().name)
            return AIResult.Failure(
                AgentError.create(
                    code = aiErrorDetail?.let { AgentErrorCode.AI_NETWORK_ERROR }
                        ?: AgentErrorCode.AI_REQUEST_ERROR,
                    cause = e,
                    aiErrorDetail = aiErrorDetail
                )
            )
        } finally {
            activeRequests.remove(requestId)
        }
    }

    private suspend fun performChatCompletion(
        request: AIRequest,
        shouldStop: AtomicBoolean
    ): ChatCompletionResponse {
        val input = request.input
        val requestMessage = input.messages.map { it.copy() }.toList()
        trimRequestAttachment(requestMessage)

        val chatRequest = buildChatRequest(
            model = request.model.ifEmpty { getDefaultModelId() } ?: "default",
            messages = AgentMessageUtils.processAttachMessage(requestMessage),
            temperature = request.parameters["temperature"]?.toFloatOrNull()
                ?: getDefaultTemperature(),
            maxTokens = request.parameters["max_tokens"]?.toIntOrNull() ?: getDefaultMaxTokens(),
            stream = false,
            tools = AgentToolMapping.map(
                request.tools,
                createDef = { type, func -> createToolDefinition(type, func) },
                createFunc = { name, desc, params -> createFunctionDefinition(name, desc, params) }
            ),
            reasoning = request.reasoning
        )

        val responseText = sendHttpRequest(
            url = "${getChatUrl()}",
            requestBody = chatRequest,
            shouldStop = shouldStop,
            requestId = generateRequestId(request)
        )

        return parseChatResponse(responseText)
    }

    private suspend fun performStreamChatCompletion(
        request: AIRequest,
        shouldStop: AtomicBoolean,
        requestId: String,
        onChunkResponse: ((ChatCompletionResponse) -> Unit)?
    ): ChatCompletionResponse {
        val input = request.input as? AgentInput
            ?: throw IllegalArgumentException(GlobalApp.getString(com.hive.i8n.R.string.agent_invalid_chat_input))
        val requestMessage = input.messages.map { it.copy() }.toList()

        //自动添加提示词前后缀
        requestMessage.forEach {
            val info = getProviderInfo()
            val promptPrefix = info.promptPrefix
            val promptSuffix = info.promptSuffix
            if (!promptPrefix.isNullOrEmpty()) {
                it.content = "$promptPrefix\n$it.content"
            }
            if (!promptSuffix.isNullOrEmpty()) {
                it.content = "$it.content\n$promptSuffix"
            }
        }
        trimRequestAttachment(requestMessage)

        val chatRequest = buildChatRequest(
            model = request.model.ifEmpty { getDefaultModelId() } ?: "default",
            messages = AgentMessageUtils.processAttachMessage(requestMessage),
            temperature = request.parameters["temperature"]?.toFloatOrNull()
                ?: getDefaultTemperature(),
            maxTokens = request.parameters["max_tokens"]?.toIntOrNull() ?: getDefaultMaxTokens(),
            stream = true,
            tools = AgentToolMapping.map(
                request.tools,
                createDef = { type, func -> createToolDefinition(type, func) },
                createFunc = { name, desc, params -> createFunctionDefinition(name, desc, params) }
            ),
            reasoning = request.reasoning
        )

        var accumulatedContent = ""
        var accumulatedReasoningContent = ""
        val accumulatedReasoningDetails = JsonArray()
        val toolCallAccumulator = StreamToolCallAccumulator(getProviderName())
        var finalModel: String? = null
        var finalUsage: Map<String, Int> = emptyMap()
        var finalCost: Double? = null
        var finalResponse: ChatCompletionResponse? = null

        try {
            sendStreamHttpRequest(
                url = "${getChatUrl()}",
                requestBody = chatRequest,
                shouldStop = shouldStop,
                requestId = requestId
            ) { chunk ->
            try {
                var data = chunk
                if (data.startsWith("event:") || data.startsWith(":")) {
                    return@sendStreamHttpRequest true
                }
                if (chunk.startsWith("data: ")) {
                    data = chunk.substring(6).trim()
                } else if (chunk == "data:") {
                    return@sendStreamHttpRequest true
                }
                if (data.isNotEmpty()) {
                    // 部分兼容接口在 stream=true 时仍返回完整 JSON（非 SSE）
                    // 必须同时含 choices+message，避免把 {"error":{"message":...}} 误当成完整响应
                    if (data.startsWith("{") &&
                        data.contains("\"choices\"") &&
                        data.contains("\"message\"") &&
                        !data.contains("\"delta\"")
                    ) {
                        val parsed = parseChatResponse(data)
                        accumulatedContent = parsed.content ?: ""
                        accumulatedReasoningContent = parsed.reasoningContent ?: ""
                        finalModel = parsed.model
                        finalUsage = parsed.usage
                        finalResponse = parsed
                        return@sendStreamHttpRequest false
                    }
                    if (data == "[DONE]" || data.getJsonKey<Boolean>("done") == true) {
                        finalResponse = buildStreamResponse(
                            accumulatedContent, accumulatedReasoningContent, finalModel, finalUsage,
                            toolCallAccumulator.toToolCalls().ifEmpty { null }, finalCost,
                            accumulatedReasoningDetails.takeIf { it.size() > 0 }
                        )
                        return@sendStreamHttpRequest false
                    }
                    val streamData = parseStreamResponse(data)
                    accumulatedContent += streamData.content ?: ""
                    accumulatedReasoningContent += streamData.reasoningContent ?: ""
                    ReasoningResponseNormalizer.appendDetails(
                        accumulatedReasoningDetails,
                        streamData.reasoningDetailsChunk
                    )
                    finalModel = streamData.model
                    streamData.cost?.let { finalCost = it }

                    onChunkResponse?.invoke(
                        buildStreamResponse(
                            accumulatedContent, accumulatedReasoningContent, finalModel, finalUsage,
                            toolCallAccumulator.toToolCalls().ifEmpty { null }, finalCost,
                            accumulatedReasoningDetails.takeIf { it.size() > 0 }
                        )
                    )

                    streamData.usage?.let { usage -> finalUsage = usage }

                    streamData.toolCalls?.forEach { toolCall ->
                        toolCallAccumulator.update(
                            index = toolCall.index,
                            id = toolCall.id,
                            type = toolCall.type,
                            functionName = toolCall.functionName,
                            argumentsChunk = toolCall.arguments
                        )
                    }

                    val currentToolCalls = toolCallAccumulator.toToolCalls()

                    if (isStreamResponseComplete(streamData.finishReason)) {
                        val finalToolCalls = toolCallAccumulator.toToolCalls()
                        val uniqueToolCalls = finalToolCalls.distinctBy { it.id }

                        if (uniqueToolCalls.size != finalToolCalls.size) {
                            DLog.w(
                                getProviderName(),
                                GlobalApp.getString(
                                    com.hive.i8n.R.string.agent_duplicate_tool_calls,
                                    finalToolCalls.size,
                                    uniqueToolCalls.size
                                )
                            )
                        }

                        finalResponse = buildStreamResponse(
                            accumulatedContent, accumulatedReasoningContent, finalModel, finalUsage,
                            uniqueToolCalls.ifEmpty { null }, finalCost,
                            accumulatedReasoningDetails.takeIf { it.size() > 0 }
                        )
                        return@sendStreamHttpRequest false
                    }
                }
                true
            } catch (e: Exception) {
                e.printStackTrace()
                DLog.e(
                    getProviderName(),
                    GlobalApp.getString(
                        com.hive.i8n.R.string.agent_parse_stream_error,
                        e.message ?: ""
                    )
                )
                finalResponse = ChatCompletionResponse(
                    content = e.message, reasoningContent = null, model = finalModel,
                    usage = finalUsage, toolCalls = null, cost = null
                )
                return@sendStreamHttpRequest false
            }
        }
        } catch (e: Exception) {
            if (e is InterruptedException || shouldStop.get()) {
                throw e
            }
            val hasPartialContent = accumulatedContent.isNotEmpty() ||
                accumulatedReasoningContent.isNotEmpty() ||
                accumulatedReasoningDetails.size() > 0 ||
                toolCallAccumulator.toToolCalls().isNotEmpty()
            if (!hasPartialContent || !isBenignStreamTermination(e)) {
                throw e
            }
            DLog.w(
                getProviderName(),
                "流式连接异常结束，使用已接收内容: ${e.message}"
            )
        }

        if (finalResponse == null) {
            val toolCalls = toolCallAccumulator.toToolCalls()
                .filter { it.id.isNotEmpty() && it.function.name.isNotEmpty() }
                .ifEmpty { null }
            if (accumulatedContent.isNotEmpty() ||
                accumulatedReasoningContent.isNotEmpty() ||
                accumulatedReasoningDetails.size() > 0 ||
                toolCalls != null
            ) {
                finalResponse = buildStreamResponse(
                    accumulatedContent,
                    accumulatedReasoningContent.takeIf { it.isNotEmpty() },
                    finalModel,
                    finalUsage,
                    toolCalls,
                    finalCost,
                    accumulatedReasoningDetails.takeIf { it.size() > 0 }
                )
            }
        }

        return finalResponse
            ?: throw Exception(GlobalApp.getString(com.hive.i8n.R.string.agent_stream_response_incomplete))
    }

    protected abstract fun createToolDefinition(type: String, func: Any): Any

    protected abstract fun createFunctionDefinition(
        name: String,
        desc: String,
        params: JsonObject
    ): Any

    private fun trimRequestAttachment(message: List<ChatMessage>) {
        val attachmentMessages = message.filter { it.attachments.isNotEmpty() }
        attachmentMessages.forEachIndexed { index, chatMessage ->
            if (index < attachmentMessages.size - 3) {
                chatMessage.attachments.clear()
            }
        }
    }

    /**
     * Classify network exceptions into specific error types
     */
    private fun classifyNetworkException(
        exception: Throwable,
        providerId: String
    ): AIErrorDetail? {
        return when (exception) {
            is java.net.SocketTimeoutException -> {
                // Determine if connection or read timeout
                val timeoutType = if (exception.message?.contains("connect", ignoreCase = true) == true) {
                    NetworkErrorType.CONNECTION_TIMEOUT
                } else {
                    NetworkErrorType.READ_TIMEOUT
                }
                AIErrorDetail.NetworkError(
                    networkType = timeoutType,
                    originalException = exception,
                    troubleshootingHint = GlobalApp.getString(
                        com.hive.i8n.R.string.ai_error_network_timeout_hint
                    )
                )
            }

            is java.net.UnknownHostException -> AIErrorDetail.NetworkError(
                networkType = NetworkErrorType.DNS_RESOLUTION_FAILED,
                originalException = exception,
                troubleshootingHint = GlobalApp.getString(
                    com.hive.i8n.R.string.ai_error_network_dns_hint
                )
            )

            is java.net.ConnectException -> {
                val networkType = if (exception.message?.contains("refused", ignoreCase = true) == true) {
                    NetworkErrorType.CONNECTION_REFUSED
                } else {
                    NetworkErrorType.NETWORK_UNREACHABLE
                }
                AIErrorDetail.NetworkError(
                    networkType = networkType,
                    originalException = exception,
                    troubleshootingHint = GlobalApp.getString(
                        com.hive.i8n.R.string.ai_error_network_connection_hint
                    )
                )
            }

            is javax.net.ssl.SSLException -> AIErrorDetail.NetworkError(
                networkType = NetworkErrorType.SSL_HANDSHAKE_FAILED,
                originalException = exception,
                troubleshootingHint = GlobalApp.getString(
                    com.hive.i8n.R.string.ai_error_network_ssl_hint
                )
            )

            is AIHttpException -> exception.aiErrorDetail

            else -> null
        }
    }

    protected data class StreamResponseData(
        val content: String?,
        val reasoningContent: String?,
        val model: String?,
        val usage: Map<String, Int>?,
        val toolCalls: List<ToolCallData>?,
        val finishReason: String?,
        val cost: Double? = null,
        val reasoningDetailsChunk: JsonArray? = null
    )

    protected data class ToolCallData(
        val index: Int?,
        val id: String?,
        val type: String?,
        val functionName: String?,
        val arguments: String?
    )
}
