// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.agent.ai.providers

import android.text.TextUtils
import com.hive.agent.ai.DefaultAIServiceManager
import com.hive.plugin.agent.AIServiceProvider
import com.hive.plugin.agent.ProviderInfo
import com.hive.plugin.agent.ModelInfo
import com.hive.plugin.agent.model.AIRequest
import com.hive.plugin.agent.model.AIResult
import com.hive.plugin.agent.model.AgentError
import com.hive.plugin.agent.model.AgentErrorCode
import com.hive.agent.utils.AIUsageTracker
import com.hive.plugin.agent.model.ChatCompletionResponse
import com.hive.plugin.agent.model.Modality
import com.hive.plugin.agent.model.AIErrorDetail
import com.hive.plugin.agent.model.ApiKeyErrorReason
import com.hive.plugin.agent.model.AuthErrorType
import com.hive.plugin.agent.model.NetworkErrorType
import com.hive.plugin.agent.model.ServiceErrorType
import com.hive.plugin.agent.model.ParseErrorType
import com.hive.plugin.agent.model.ModelErrorReason
import com.hive.utils.debug.DLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import com.hive.utils.GlobalApp
import java.util.concurrent.atomic.AtomicBoolean

/**
 * AI服务提供者基础抽象类
 * 提供公共的HTTP连接管理、请求跟踪、错误处理和资源清理逻辑
 */
abstract class AbstractBaseProvider : AIServiceProvider {

    var serviceManager: DefaultAIServiceManager? = null

    var tryStopChatting = false

    // 跟踪正在进行的推理请求
    protected val activeRequests = ConcurrentHashMap<String, AtomicBoolean>()

    // 跟踪正在进行的HTTP连接
    protected val activeConnections = ConcurrentHashMap<String, HttpURLConnection>()

    // 子类需要实现此方法来提供Provider信息
    abstract override fun getProviderInfo(): ProviderInfo

    // 动态配置管理 - 通过AIServiceManager获取配置
    override fun getApiKey(): String {
        return serviceManager?.getProviderApiKey(getProviderInfo().name) ?: ""
    }

    /**
     * 获取基础URL
     */
    open protected fun getChatUrl(): String {
        return getProviderInfo().apiUrl
    }

    /** 设置页是否允许编辑 Base URL（OpenAI 兼容 Provider） */
    open fun supportsEditableBaseUrl(): Boolean = false

    /** 是否必须配置 Base URL 才算就绪（自定义 Provider） */
    open fun requiresBaseUrl(): Boolean = false

    override fun getTags(): List<String> {
        return getProviderInfo().tags ?: emptyList()
    }

    /**
     * 获取请求头
     */
    protected open fun getRequestHeaders(): Map<String, String> {
        return mapOf(
            "Content-Type" to "application/json",
            "Authorization" to "Bearer ${getApiKey()}",
            "Accept" to "application/json"
        )
    }

    /**
     * 获取性能评分（默认0.8）
     */
    override fun getPerformanceScore(): Float = 0.8f

    /**
     * 声明支持的模态类型（默认只支持文本）
     */
    protected open fun getSupportedModalities(): Set<Modality> {
        return setOf(Modality.TEXT)
    }

    /**
     * 检查是否支持特定模态
     */
    fun supportsModality(modality: Modality): Boolean {
        return getSupportedModalities().contains(modality)
    }


    /**
     * 推理方法（默认实现，子类可以重写）
     */
    override suspend fun <T> inference(request: AIRequest): AIResult<T> {
        tryStopChatting = false

        // NEW: Pre-validate API key (skip if using default model or apikeyEnabled=false)
        val apiKeyError = validateApiKeyBeforeCall(request.model)
        if (apiKeyError != null) {
            return AIResult.Failure(
                AgentError.create(
                    code = AgentErrorCode.AI_AUTHENTICATION_FAILED,
                    message = apiKeyError.reason.name,
                    aiErrorDetail = apiKeyError
                )
            )
        }

        if (TextUtils.isEmpty(request.model)) {
            return AIResult.Failure(
                error = AgentError.create(
                    code = AgentErrorCode.AI_SERVICE_UNAVAILABLE,
                    aiErrorDetail = AIErrorDetail.ModelError(
                        modelId = "",
                        providerId = getProviderInfo().name,
                        reason = ModelErrorReason.NOT_AVAILABLE
                    )
                )
            )
        }
        val result = onInference<T>(request)
        if (!tryStopChatting && result is AIResult.Success && result.data is ChatCompletionResponse) {
            AIUsageTracker.record(result.data as ChatCompletionResponse, getProviderInfo().name, request.model)
        }
        return if (tryStopChatting) {
            AIResult.Failure(
                AgentError.create(code = AgentErrorCode.AI_REQUEST_CANCEL)
            )
        } else {
            result
        }
    }


    /**
     * 流式推理方法（默认实现，子类可以重写）
     */
    override suspend fun <T> streamInference(
        request: AIRequest,
        onChunkResponse: ((ChatCompletionResponse) -> Unit)?
    ): AIResult<T> {
        // 检查模态支持
        tryStopChatting = false

        // NEW: Pre-validate API key (skip if using default model or apikeyEnabled=false)
        val apiKeyError = validateApiKeyBeforeCall(request.model)
        if (apiKeyError != null) {
            return AIResult.Failure(
                AgentError.create(
                    code = AgentErrorCode.AI_AUTHENTICATION_FAILED,
                    message = apiKeyError.reason.name,
                    aiErrorDetail = apiKeyError
                )
            )
        }

        if (TextUtils.isEmpty(request.model)) {
            return AIResult.Failure(
                AgentError.create(
                    code = AgentErrorCode.AI_SERVICE_UNAVAILABLE,
                    aiErrorDetail = AIErrorDetail.ModelError(
                        modelId = "",
                        providerId = getProviderInfo().name,
                        reason = ModelErrorReason.NOT_AVAILABLE
                    )
                )
            )
        }

        val result = onStreamInference<T>(request, onChunkResponse)
        if (!tryStopChatting && result is AIResult.Success && result.data is ChatCompletionResponse) {
            AIUsageTracker.record(result.data as ChatCompletionResponse, getProviderInfo().name, request.model)
        }
        return if (tryStopChatting) {
            AIResult.Failure(
                AgentError.create(code = AgentErrorCode.AI_REQUEST_CANCEL)
            )
        } else {
            result
        }
    }

    /**
     * 推理方法（默认实现，子类可以重写）
     */
    abstract suspend fun <T> onInference(request: AIRequest): AIResult<T>

    /**
     * 推理方法（默认实现，子类可以重写）
     */
    abstract suspend fun <T> onStreamInference(
        request: AIRequest,
        onChunkResponse: ((ChatCompletionResponse) -> Unit)?
    ): AIResult<T>

    override suspend fun getModels(): List<ModelInfo> {
        val allModels = mutableListOf<ModelInfo>()
        val buildIn = getBuildInModels()
        val customs =
            serviceManager?.getProviderCustomModels(getProviderInfo().name) ?: mutableListOf()
        allModels.addAll(buildIn)
        allModels.addAll(customs)
        return allModels
    }

    abstract suspend fun getBuildInModels(): List<ModelInfo>

    override fun updateCustomModels(models: MutableList<ModelInfo>) {

    }

    /**
     * 检查Provider是否启用
     */
    override fun isProviderReady(): Boolean {
        return hasValidApiKey() && serviceManager?.isProviderEnabled(getProviderInfo().name) == true
    }

    override fun isModelReady(modelId: String): Boolean {
        return isProviderReady() && serviceManager?.isProviderModelEnabled(
            getProviderInfo().name,
            modelId
        ) == true
    }


    override fun hasValidApiKey(): Boolean {
        return serviceManager?.hasValidApiKey(
            getProviderInfo().name,
            getProviderInfo().apiKeyPrefix
        ) == true
    }

    /**
     * 获取Provider名称
     */
    fun getProviderName(): String {
        return getProviderInfo().name
    }

    /**
     * 获取Provider显示名称
     */
    fun getDisplayName(): String {
        return getProviderInfo().displayName
    }

    /**
     * 获取Provider描述
     */
    fun getDescription(): String {
        return getProviderInfo().description
    }

    /**
     * 获取连接超时时间（毫秒）
     * 默认 30 秒，子类可重写
     */
    protected open fun getConnectTimeout(): Int = 30_000

    /**
     * 获取读取超时时间（毫秒）
     * 默认 120 秒，流式推理时服务端可能长时间无数据（如思考模型），子类可重写
     */
    protected open fun getReadTimeout(): Int = 120_000

    /**
     * 发送流式HTTP请求的通用方法
     * 子类可以使用此方法来实现真正的流式推理
     */
    protected suspend fun sendStreamHttpRequest(
        url: String,
        requestBody: String,
        shouldStop: AtomicBoolean,
        customHeaders: Map<String, String> = emptyMap(),
        requestId: String? = null,
        onChunk: (String) -> Boolean
    ) {
        withContext(Dispatchers.IO) {
            val connection = URL(url).openConnection() as HttpURLConnection
            val actualRequestId = requestId ?: generateRequestId(null)

            try {
                // 注册连接
                activeConnections[actualRequestId] = connection

                connection.apply {
                    requestMethod = "POST"
                    // 设置默认请求头
                    getRequestHeaders().forEach { (key, value) ->
                        setRequestProperty(key, value)
                    }
                    // 设置自定义请求头
                    customHeaders.forEach { (key, value) ->
                        setRequestProperty(key, value)
                    }
                    // 设置流式传输相关的请求头
                    setRequestProperty("Accept", "text/event-stream")
                    setRequestProperty("Cache-Control", "no-cache")
                    doOutput = true
                    connectTimeout = getConnectTimeout()
                    readTimeout = getReadTimeout()
                }

                // 检查是否应该停止
                if (shouldStop.get()) {
                    throw InterruptedException(GlobalApp.getString(com.hive.i8n.R.string.agent_request_stopped))
                }

                // 发送请求体
                OutputStreamWriter(connection.outputStream).use { writer ->
                    writer.write(requestBody)
                    writer.flush()
                }

                // 检查是否应该停止
                if (shouldStop.get()) {
                    throw InterruptedException(GlobalApp.getString(com.hive.i8n.R.string.agent_request_stopped))
                }

                // 检查响应状态
                val responseCode = connection.responseCode
                if (responseCode !in 200..299) {
                    val errorStream = connection.errorStream
                    val errorText = errorStream?.bufferedReader()?.readText()
                        ?: GlobalApp.getString(com.hive.i8n.R.string.agent_unknown_error)

                    // NEW: Create detailed error based on HTTP code
                    val aiErrorDetail = classifyHttpError(
                        responseCode,
                        errorText,
                        getProviderInfo().name
                    )

                    throw AIHttpException(
                        responseCode,
                        errorText,
                        aiErrorDetail
                    )
                }

                // 检查是否应该停止
                if (shouldStop.get()) {
                    throw InterruptedException(GlobalApp.getString(com.hive.i8n.R.string.agent_request_stopped))
                }

                // 读取流式响应
                val reader = connection.inputStream.bufferedReader()
                var line: String?

                while (reader.readLine().also { line = it } != null) {
                    // 检查是否应该停止
                    if (shouldStop.get()) {
                        throw InterruptedException(GlobalApp.getString(com.hive.i8n.R.string.agent_request_stopped))
                    }

                    line?.let { chunk ->
                        if (chunk.isNotEmpty()) {
                            if (!onChunk(chunk)) break
                        }
                    }
                }
            } finally {
                connection.disconnect()
                activeConnections.remove(actualRequestId)
            }
        }
    }

    override fun stopInference(request: AIRequest?) {
        tryStopChatting = true
        if (request == null) {
            // 停止所有正在进行的请求
            activeRequests.values.forEach { shouldStop ->
                shouldStop.set(true)
            }
            // 断开所有活跃连接
            activeConnections.values.forEach { connection ->
                try {
                    connection.disconnect()
                } catch (e: Exception) {
                    DLog.w(
                        "BaseAIProvider",
                        GlobalApp.getString(
                            com.hive.i8n.R.string.agent_disconnect_error,
                            e.message ?: ""
                        )
                    )
                }
            }
            activeConnections.clear()
        } else {
            // 停止特定请求
            val requestId = generateRequestId(request)
            activeRequests[requestId]?.set(true)
            activeConnections[requestId]?.let { connection ->
                try {
                    connection.disconnect()
                } catch (e: Exception) {
                    DLog.w(
                        "BaseAIProvider",
                        GlobalApp.getString(
                            com.hive.i8n.R.string.agent_disconnect_error,
                            e.message ?: ""
                        )
                    )
                }
                activeConnections.remove(requestId)
            }
        }

        // 强制清理已停止的请求
        cleanupStoppedRequests()
    }

    /**
     * 清理已停止的请求
     */
    private fun cleanupStoppedRequests() {
        val stoppedRequests = activeRequests.filter { it.value.get() }
        stoppedRequests.forEach { (requestId, _) ->
            activeRequests.remove(requestId)
            activeConnections.remove(requestId)
        }
    }

    /**
     * 发送HTTP请求的通用方法
     */
    protected suspend fun sendHttpRequest(
        url: String,
        requestBody: String,
        shouldStop: AtomicBoolean,
        customHeaders: Map<String, String> = emptyMap(),
        requestId: String? = null
    ): String {
        return withContext(Dispatchers.IO) {
            val connection = URL(url).openConnection() as HttpURLConnection
            val actualRequestId = requestId ?: generateRequestId(null)

            try {
                // 注册连接
                activeConnections[actualRequestId] = connection

                connection.apply {
                    requestMethod = "POST"
                    // 设置默认请求头
                    getRequestHeaders().forEach { (key, value) ->
                        setRequestProperty(key, value)
                    }
                    // 设置自定义请求头
                    customHeaders.forEach { (key, value) ->
                        setRequestProperty(key, value)
                    }
                    doOutput = true
                    connectTimeout = getConnectTimeout()
                    readTimeout = getReadTimeout()
                }

                // 检查是否应该停止
                if (shouldStop.get()) {
                    throw InterruptedException(GlobalApp.getString(com.hive.i8n.R.string.agent_request_stopped))
                }

                // 发送请求体
                OutputStreamWriter(connection.outputStream).use { writer ->
                    writer.write(requestBody)
                    writer.flush()
                }

                // 检查是否应该停止
                if (shouldStop.get()) {
                    throw InterruptedException(GlobalApp.getString(com.hive.i8n.R.string.agent_request_stopped))
                }

                // 检查响应状态
                val responseCode = connection.responseCode
                if (responseCode !in 200..299) {
                    val errorStream = connection.errorStream
                    val errorText = errorStream?.bufferedReader()?.readText()
                        ?: GlobalApp.getString(com.hive.i8n.R.string.agent_unknown_error)

                    // NEW: Create detailed error based on HTTP code
                    val aiErrorDetail = classifyHttpError(
                        responseCode,
                        errorText,
                        getProviderInfo().name
                    )

                    throw AIHttpException(
                        responseCode,
                        errorText,
                        aiErrorDetail
                    )
                }

                // 检查是否应该停止
                if (shouldStop.get()) {
                    throw InterruptedException(GlobalApp.getString(com.hive.i8n.R.string.agent_request_stopped))
                }

                // 读取响应 - 分块读取以便及时响应停止请求
                val reader = connection.inputStream.bufferedReader()
                val responseBuilder = StringBuilder()
                val buffer = CharArray(1024)
                var bytesRead: Int

                while (reader.read(buffer).also { bytesRead = it } != -1) {
                    // 检查是否应该停止
                    if (shouldStop.get()) {
                        throw InterruptedException(GlobalApp.getString(com.hive.i8n.R.string.agent_request_stopped))
                    }
                    responseBuilder.append(buffer, 0, bytesRead)
                }

                responseBuilder.toString()
            } finally {
                connection.disconnect()
                activeConnections.remove(actualRequestId)
            }
        }
    }

    /**
     * 生成请求ID
     */
    protected open fun generateRequestId(request: AIRequest?): String {
        return request?.let {
            "${it.model}_${it.requestType}_${System.currentTimeMillis()}"
        } ?: "unknown_${System.currentTimeMillis()}"
    }

    /**
     * 获取当前活跃请求数量
     */
    open fun getActiveRequestCount(): Int = activeRequests.size

    /**
     * 清理所有资源
     */
    open fun cleanup() {
        stopInference(null)
        activeRequests.clear()
        activeConnections.clear()
    }

/**
 * Classify HTTP error based on status code and response body
 */
protected open fun classifyHttpError(
    httpCode: Int,
    responseBody: String,
    providerId: String
): AIErrorDetail {
        return when (httpCode) {
            401 -> AIErrorDetail.AuthenticationError(
                httpStatusCode = httpCode,
                errorType = AuthErrorType.INVALID_API_KEY,
                responseBody = responseBody,
                troubleshootingHint = GlobalApp.getString(
                    com.hive.i8n.R.string.ai_error_auth_invalid_key_hint
                )
            )

            403 -> {
                val errorType = if (responseBody.contains("permission", ignoreCase = true)) {
                    AuthErrorType.PERMISSION_DENIED
                } else {
                    AuthErrorType.INSUFFICIENT_SCOPE
                }
                AIErrorDetail.AuthenticationError(
                    httpStatusCode = httpCode,
                    errorType = errorType,
                    responseBody = responseBody,
                    troubleshootingHint = GlobalApp.getString(
                        com.hive.i8n.R.string.ai_error_auth_permission_hint
                    )
                )
            }

            404 -> AIErrorDetail.ModelError(
                modelId = "",  // Will be filled by caller
                providerId = providerId,
                reason = ModelErrorReason.NOT_AVAILABLE,
                responseBody = responseBody,
                troubleshootingHint = GlobalApp.getString(
                    com.hive.i8n.R.string.ai_error_model_not_found_hint
                )
            )

            429 -> AIErrorDetail.ServiceError(
                httpStatusCode = httpCode,
                serviceType = ServiceErrorType.RATE_LIMIT_EXCEEDED,
                responseBody = responseBody,
                troubleshootingHint = GlobalApp.getString(
                    com.hive.i8n.R.string.ai_error_rate_limit_hint
                )
            )

            402 -> {
                // Provider account/quota exhausted (user-configured provider billing).
                val balanceInfo = parseBalanceInfo(responseBody)
                AIErrorDetail.InsufficientBalanceError(
                    currentBalance = balanceInfo.first,
                    requiredBalance = balanceInfo.second,
                    diffBalance = balanceInfo.third,
                    httpStatusCode = httpCode,
                    responseBody = responseBody,
                    troubleshootingHint = GlobalApp.getString(
                        com.hive.i8n.R.string.ai_error_insufficient_balance_hint,
                        balanceInfo.third
                    )
                )
            }

            500 -> AIErrorDetail.ServiceError(
                httpStatusCode = httpCode,
                serviceType = ServiceErrorType.INTERNAL_ERROR,
                responseBody = responseBody,
                troubleshootingHint = GlobalApp.getString(
                    com.hive.i8n.R.string.ai_error_service_internal_hint
                )
            )

            502 -> AIErrorDetail.ServiceError(
                httpStatusCode = httpCode,
                serviceType = ServiceErrorType.BAD_GATEWAY,
                responseBody = responseBody,
                troubleshootingHint = GlobalApp.getString(
                    com.hive.i8n.R.string.ai_error_service_bad_gateway_hint
                )
            )

            503 -> AIErrorDetail.ServiceError(
                httpStatusCode = httpCode,
                serviceType = ServiceErrorType.SERVICE_UNAVAILABLE,
                responseBody = responseBody,
                troubleshootingHint = GlobalApp.getString(
                    com.hive.i8n.R.string.ai_error_service_unavailable_hint
                )
            )

            504 -> AIErrorDetail.ServiceError(
                httpStatusCode = httpCode,
                serviceType = ServiceErrorType.GATEWAY_TIMEOUT,
                responseBody = responseBody,
                troubleshootingHint = GlobalApp.getString(
                    com.hive.i8n.R.string.ai_error_service_timeout_hint
                )
            )

            else -> AIErrorDetail.ServiceError(
                httpStatusCode = httpCode,
                serviceType = ServiceErrorType.INTERNAL_ERROR,
                responseBody = responseBody
            )
        }
    }

    /**
     * Parse balance info from HTTP 402 response body
     * Returns Triple(currentBalance, requiredBalance, diffBalance)
     */
    private fun parseBalanceInfo(responseBody: String?): Triple<Int, Int, Int> {
        return try {
            if (responseBody.isNullOrEmpty()) {
                Triple(0, 0, 0)
            } else {
                val json = org.json.JSONObject(responseBody)
                val data = json.optJSONObject("data")
                if (data != null) {
                    val current = data.optInt("balance", 0)
                    val required = data.optInt("required", 0)
                    val diff = data.optInt("diff", required - current)
                    Triple(current, required, diff)
                } else {
                    Triple(0, 0, 0)
                }
            }
        } catch (e: Exception) {
            DLog.e("BaseProvider", "Failed to parse balance info: ${e.message}")
            Triple(0, 0, 0)
        }
    }

    /**
     * Validate API key before making call
     * Returns null if validation passes, otherwise returns ApiKeyError
     *
     * @param modelId The model ID being used (optional, for default model check)
     */
    private fun validateApiKeyBeforeCall(modelId: String? = null): AIErrorDetail.ApiKeyError? {
        val providerInfo = getProviderInfo()

        // Skip validation if provider doesn't require API key
        if (!providerInfo.apikeyEnabled) {
            return null
        }

        // Skip validation if using default model (some providers offer free default models)
        if (!modelId.isNullOrEmpty()) {
            val isDefaultModel = modelId == providerInfo.defaultModelId ||
                    modelId == providerInfo.defaultMultiModelId
            if (isDefaultModel) {
                return null
            }
        }

        val apiKey = getApiKey()

        // Check if API key is configured
        if (apiKey.isNullOrEmpty()) {
            return AIErrorDetail.ApiKeyError(
                providerId = providerInfo.name,
                reason = ApiKeyErrorReason.NOT_CONFIGURED,
                troubleshootingHint = GlobalApp.getString(
                    com.hive.i8n.R.string.ai_error_apikey_missing_hint,
                    providerInfo.displayName
                )
            )
        }

        // Check API key format (prefix validation)
        val prefix = providerInfo.apiKeyPrefix
        if (!prefix.isNullOrEmpty() &&
            !apiKey.startsWith(prefix)) {
            return AIErrorDetail.ApiKeyError(
                providerId = providerInfo.name,
                reason = ApiKeyErrorReason.INVALID_FORMAT,
                troubleshootingHint = providerInfo.apiKeyValidateMsg
            )
        }

        return null
    }

    /**
     * Custom exception to carry AI error details through HTTP stack
     */
    protected class AIHttpException(
        val httpCode: Int,
        val errorBody: String,
        val aiErrorDetail: AIErrorDetail
    ) : Exception("HTTP $httpCode: $errorBody")

} 