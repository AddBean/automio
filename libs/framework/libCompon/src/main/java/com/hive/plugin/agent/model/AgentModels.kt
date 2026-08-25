// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.plugin.agent.model

import android.text.TextUtils
import com.hive.compon.R
import com.hive.utils.GlobalApp
import java.io.Serializable

/**
 * Agent请求数据模型
 */
data class AgentRequest(
    val toolId: String,
    val action: String,
    val params: Any = emptyMap<String, Any>(),
    val taskId: String? = null,
    val timeout: Long = 30_000_000L,
    val preferredTools: List<String>? = null  // 优先使用的工具列表
) : Serializable

/**
 * Agent响应结果封装
 */
data class AgentResult<T>(
    var success: Boolean,
    var data: T?,
    var extra: String?,
    var message: String? = null,
    val files: List<ChatAttachment>? = null,
    var error: AgentError? = null,
) : Serializable {

    /**
     * 获取错误，成功时返回null
     */
    fun getErrorOrNull(): AgentError? = when {
        this.success -> null
        else -> error
    }

    companion object {
        /**
         * 创建一个成功的结果
         * @param data 返回的数据
         * @param message 可选的成功消息
         */
        fun Success(
            data: Any? = null,
            message: String? = null,
            files: List<ChatAttachment>? = null,
            extra: String? = null,
        ): AgentResult<Any> {
            return AgentResult(
                success = true,
                data = data,
                message = message,
                files = files,
                extra = extra
            )
        }

        /**
         * 创建一个失败的结果
         * @param error 错误信息
         */
        fun Failure(error: AgentError): AgentResult<Nothing> {
            return AgentResult(success = false, data = null, error = error, extra = null)
        }
    }

}

/**
 * Agent错误信息
 */
open class AgentError(
    val code: AgentErrorCode,
    var msg: String? = null,
    var e: Throwable? = null,
    var aiErrorDetail: AIErrorDetail? = null  // NEW: detailed AI error info
) : RuntimeException(msg, e), Serializable {

    fun getInfo(): String {
        return if (TextUtils.isEmpty(msg)) {
            code.getMessage()
        } else {
            msg ?: ""
        }
    }

    // NEW: Get detailed error info for UI
    fun getDetailedInfo(): String {
        val base = getInfo()
        val detail = aiErrorDetail?.let { buildDetailString(it) }
        return if (detail != null) "$base\n$detail" else base
    }

    private fun buildDetailString(detail: AIErrorDetail): String {
        return when (detail) {
            is AIErrorDetail.ApiKeyError -> "Provider: ${detail.providerId}, Reason: ${detail.reason}"
            is AIErrorDetail.AuthenticationError -> "HTTP ${detail.httpStatusCode}, Type: ${detail.errorType}"
            is AIErrorDetail.NetworkError -> "Network: ${detail.networkType}"
            is AIErrorDetail.ServiceError -> "HTTP ${detail.httpStatusCode}, Service: ${detail.serviceType}"
            is AIErrorDetail.ParseError -> "Parse: ${detail.parseType}"
            is AIErrorDetail.ModelError -> "Model: ${detail.modelId}, Reason: ${detail.reason}"
            is AIErrorDetail.InsufficientBalanceError -> "Balance: ${detail.currentBalance}/${detail.requiredBalance}, Need: ${detail.diffBalance}"
        }
    }

    companion object {
        fun create(
            code: AgentErrorCode,
            message: String? = null,
            cause: Throwable? = null,
            aiErrorDetail: AIErrorDetail? = null  // NEW parameter
        ): AgentError {
            return AgentError(
                code = code,
                msg = message ?: "",
                e = cause,
                aiErrorDetail = aiErrorDetail
            )
        }
    }
}

/**
 * Agent错误代码枚举
 */
enum class AgentErrorCode(val code: Int, val msgResId: Int) {
    UNKNOWN_ERROR(1000, com.hive.i8n.R.string.error_unknown),
    TOOL_NOT_FOUND(1001, com.hive.i8n.R.string.error_tool_not_found),
    INVALID_PARAMS(1003, com.hive.i8n.R.string.error_invalid_params),
    PERMISSION_DENIED(1004, com.hive.i8n.R.string.error_permission_denied),
    NETWORK_ERROR(1005, com.hive.i8n.R.string.error_network),
    TIMEOUT(1006, com.hive.i8n.R.string.error_timeout),
    EXECUTION_FAILED(1007, com.hive.i8n.R.string.error_execution_failed),
    RESOURCE_UNAVAILABLE(1008, com.hive.i8n.R.string.error_resource_unavailable),
    CONTEXT_INVALID(1009, com.hive.i8n.R.string.error_context_invalid),

    TASK_STATE_STOP(2001, com.hive.i8n.R.string.error_task_stop),
    TASK_STATE_PAUSE(2002, com.hive.i8n.R.string.error_task_pause),
    TASK_STATE_ERROR(2003, com.hive.i8n.R.string.error_task_error),

    AI_NO_AVAILABLE_PROVIDER(3001, com.hive.i8n.R.string.error_ai_no_provider),
    AI_SERVICE_UNAVAILABLE(3002, com.hive.i8n.R.string.error_ai_service_unavailable),
    AI_INVALID_REQUEST(3003, com.hive.i8n.R.string.error_ai_invalid_request),
    AI_NETWORK_ERROR(3004, com.hive.i8n.R.string.error_ai_network),
    AI_AUTHENTICATION_FAILED(3005, com.hive.i8n.R.string.error_ai_auth_failed),
    AI_REQUEST_ERROR(3006, com.hive.i8n.R.string.error_ai_request_error),
    AI_REQUEST_CANCEL(3007, com.hive.i8n.R.string.error_ai_request_cancel);

    fun getMessage(): String {
        return GlobalApp.getString(msgResId)
    }
}

/**
 * 执行状态
 */
enum class ExecutionStatus {
    UNKNOWN,      // 未知状态
    RUNNING,      // 正在执行
    PAUSED,       // 已暂停
    STOPPED,       // 已取消
    SUCCESS,      // 执行成功
    FAILED,       // 执行失败
    TIMEOUT       // 超时
}


