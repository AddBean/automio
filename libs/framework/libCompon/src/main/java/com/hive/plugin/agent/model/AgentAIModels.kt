// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.plugin.agent.model

import androidx.annotation.Keep
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName

/** 可由模型接受的思考强度。 */
@Keep
enum class ReasoningEffort {
    LOW, MEDIUM, HIGH
}

/** 单次请求可发送给 Provider 的思考选项。 */
@Keep
data class ReasoningOptions(
    @SerializedName("enabled") val enabled: Boolean = false,
    @SerializedName("effort") val effort: ReasoningEffort = ReasoningEffort.MEDIUM
)

/** 思考过程在后续请求中应如何回放。 */
@Keep
enum class ReasoningReplayFormat {
    NONE,
    REASONING_CONTENT,
    REASONING_DETAILS,
    CONTENT_THINK_TAG
}

/** Provider 返回的结构化思考片段，保留未知字段以便兼容不同协议。 */
@Keep
data class ReasoningDetail(
    @SerializedName("type") val type: String? = null,
    @SerializedName("text") val text: String? = null,
    @SerializedName("id") val id: String? = null,
    @SerializedName("data") val data: Map<String, String> = emptyMap()
)

/** 可用于展示和下一轮消息回放的原始思考轨迹。 */
@Keep
data class ReasoningTrace(
    @SerializedName("rawText") val rawText: String? = null,
    @SerializedName("details") val details: List<ReasoningDetail> = emptyList(),
    @SerializedName("sourceProviderId") val sourceProviderId: String? = null,
    @SerializedName("sourceModelId") val sourceModelId: String? = null,
    @SerializedName("replayFormat") val replayFormat: ReasoningReplayFormat = ReasoningReplayFormat.NONE
)

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
    @SerializedName("tools") val tools: List<ToolDefinition>? = null,
    @SerializedName("reasoning") val reasoning: ReasoningOptions? = null
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
    /** 工具实际执行完成时刻，格式 HH:mm:ss.SSS，供模型规划与详情展示 */
    @SerializedName("execAt") var execAt: String? = null,
    @SerializedName("status") var status: MessageStatus = MessageStatus.FINISH,
    @SerializedName("timestamp") val timestamp: Long = System.currentTimeMillis(),
    @SerializedName("attachments") val attachments: MutableList<ChatAttachment> = mutableListOf(),
    @SerializedName("reasoningTrace") var reasoningTrace: ReasoningTrace? = null
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
    @SerializedName("cost") val cost: Double? = null,
    @SerializedName("reasoningTrace") val reasoningTrace: ReasoningTrace? = null
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
