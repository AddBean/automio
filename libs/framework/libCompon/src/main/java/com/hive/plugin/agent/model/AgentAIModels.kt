// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.plugin.agent.model

import androidx.annotation.Keep
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName

/** AI请求数据模型 */
@Keep
data class AIRequest(
    @SerializedName("model") var model: String,
    @SerializedName("requestType") val requestType: AIRequestType,
    @SerializedName("input") val input: AgentInput,
    @SerializedName("inputOrigin") val inputOrigin: AgentInput,
    @SerializedName("parameters") val parameters: Map<String, String> = emptyMap(),
    @SerializedName("enableCache") val enableCache: Boolean = true,
    @SerializedName("timeout") val timeout: Long = 30_000L,
    @SerializedName("tools") val tools: List<ToolDefinition>? = null
)

/** AI请求类型枚举 */
@Keep
enum class AIRequestType {
    CHAT_COMPLETION,

    // 添加 FUNCTION_CALL 请求类型，用于 AI 进行工具调用
    FUNCTION_CALL
}

/** AI输入数据封装 */
@Keep
data class AgentInput(@SerializedName("messages") var messages: List<ChatMessage>) {
    fun copy(): AgentInput {
        return AgentInput(messages.map { it.copy() }.toList())
    }
}

/** 聊天消息 */
@Keep
data class ChatMessage(
    @SerializedName("role") var role: MessageRole,
    @SerializedName("content") var content: String?,
    @SerializedName("reasoningContent") var reasoningContent: String? = null,
    @SerializedName("toolCalls") var toolCalls: List<ToolCall>? = null,
    @SerializedName("toolCallId") var toolCallId: String? = null,
    @SerializedName("toolCallResult") var toolCallResult: String? = null,
    @SerializedName("toolCallResultSuccess") var toolCallResultSuccess: Boolean = true,
    @SerializedName("status") var status: MessageStatus = MessageStatus.FINISH,
    @SerializedName("timestamp") val timestamp: Long = System.currentTimeMillis(),
    @SerializedName("attachments") val attachments: MutableList<ChatAttachment> = mutableListOf()
)

/** 附件类型 */
@Keep
enum class AttachmentType {
    IMAGE, AUDIO, VIDEO, FILE
}

/** 消息附件（通用结构） */
@Keep
data class ChatAttachment(
    @SerializedName("type") val type: AttachmentType,
    @SerializedName("url") val url: String? = null,
    @SerializedName("base64") var base64: String? = null,
    @SerializedName("mimeType") val mimeType: String? = null
)

@Keep
enum class MessageStatus {
    WAITING,
    TOOL_RUNNING,
    FINISH
}

/** 消息角色枚举 */
@Keep
enum class MessageRole {
    SYSTEM,
    USER,
    ASSISTANT,
    TOOL
}

/** AI响应结果封装 */
@Keep
sealed class AIResult<out T> {
    @Keep data class Success<out T>(@SerializedName("data") val data: T) : AIResult<T>()
    @Keep data class Failure(@SerializedName("error") val error: AgentError) : AIResult<Nothing>()
}

/** 聊天完成响应 */
@Keep
data class ChatCompletionResponse(
    @SerializedName("content") val content: String?,
    @SerializedName("reasoningContent") val reasoningContent: String? = null,
    @SerializedName("model") val model: String? = null,
    @SerializedName("usage") val usage: Map<String, Int> = emptyMap(),
    @SerializedName("toolCalls") val toolCalls: List<ToolCall>? = null,
    /** API 返回的本次调用费用（如 OpenRouter），用于准确统计 */
    @SerializedName("cost") val cost: Double? = null
)

/** 工具定义（Schema） */
@Keep
data class ToolDefinition(
    @SerializedName("type") val type: String = "function",
    @SerializedName("function") val function: FunctionDefinition
)

/** 函数定义 */
@Keep
data class FunctionDefinition(
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String,
    @SerializedName("parameters") val parameters: JsonObject
)

/** AI 生成的工具调用 */
@Keep
data class ToolCall(
    @SerializedName("id") val id: String,
    @SerializedName("type") val type: String = "function",
    @SerializedName("function") val function: FunctionCall
)

/** 函数调用 */
@Keep
data class FunctionCall(
    @SerializedName("name") val name: String,
    @SerializedName("arguments") val arguments: JsonObject
)

/** 多模态数据结构 */
@Keep
data class ImageData(
    @SerializedName("url") val url: String? = null,
    @SerializedName("base64") val base64: String? = null,
    @SerializedName("mimeType") val mimeType: String = "image/jpeg"
)

/** 模态类型枚举 */
@Keep
enum class Modality {
    TEXT, VISION, AUDIO, MULTIMODAL
}
