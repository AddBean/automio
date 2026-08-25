// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.mcp.model

import androidx.annotation.Keep
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName
import com.hive.plugin.mcp.model.McpResultFile

/** JSON-RPC 2.0 响应（result 与 error 二选一） */
@Keep
data class McpRpcResponse(
    @SerializedName("jsonrpc") val jsonrpc: String = "2.0",
    @SerializedName("id") val id: Any? = null,
    @SerializedName("result") val result: Any? = null,
    @SerializedName("error") val error: McpRpcError? = null
)

/** MCP JSON-RPC 结果（用于客户端解析，result 可能是 tools、content 等） */
@Keep
data class McpRpcResult(
    @SerializedName("tools") val tools: List<McpToolInfo>? = null,
    @SerializedName("content") val content: List<McpContentItem>? = null,
    @SerializedName("files") val files: List<McpResultFile>? = null,
    @SerializedName("resources") val resources: List<McpResourceInfo>? = null,
    @SerializedName("contents") val contents: List<McpResourceContent>? = null
)

// ========== 服务端响应模型（用于构建响应，避免 JsonObject） ==========

/** initialize 响应 result */
@Keep
data class McpInitializeResult(
    @SerializedName("protocolVersion") val protocolVersion: String,
    @SerializedName("capabilities") val capabilities: McpCapabilities,
    @SerializedName("serverInfo") val serverInfo: McpServerInfo
)

@Keep
data class McpCapabilities(
    @SerializedName("tools") val tools: McpCapabilityItem = McpCapabilityItem(listChanged = true),
    @SerializedName("resources") val resources: McpResourcesCapability = McpResourcesCapability(subscribe = false, listChanged = true),
    @SerializedName("prompts") val prompts: McpCapabilityItem = McpCapabilityItem(listChanged = true),
    @SerializedName("logging") val logging: Map<String, Any> = emptyMap()
)

@Keep
data class McpCapabilityItem(@SerializedName("listChanged") val listChanged: Boolean = true)

@Keep
data class McpResourcesCapability(
    @SerializedName("subscribe") val subscribe: Boolean = false,
    @SerializedName("listChanged") val listChanged: Boolean = true
)

@Keep
data class McpServerInfo(
    @SerializedName("name") val name: String,
    @SerializedName("version") val version: String,
    @SerializedName("description") val description: String? = null
)

/** tools/list 响应 result */
@Keep
data class McpToolsListResult(@SerializedName("tools") val tools: List<McpToolInfo>)

/** resources/list 响应 result */
@Keep
data class McpResourcesListResult(@SerializedName("resources") val resources: List<McpResourceInfo>)

/** resources/read 响应 result */
@Keep
data class McpResourcesReadResult(@SerializedName("contents") val contents: List<McpResourceContent>)

/** prompts/list 响应 result */
@Keep
data class McpPromptsListResult(@SerializedName("prompts") val prompts: List<McpPromptInfo>)

@Keep
data class McpPromptInfo(
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String,
    @SerializedName("arguments") val arguments: JsonObject
)

/** prompts/get 响应 result */
@Keep
data class McpPromptsGetResult(
    @SerializedName("description") val description: String,
    @SerializedName("messages") val messages: List<McpPromptMessage>
)

@Keep
data class McpPromptMessage(
    @SerializedName("role") val role: String,
    @SerializedName("content") val content: McpPromptContent
)

@Keep
data class McpPromptContent(
    @SerializedName("type") val type: String,
    @SerializedName("text") val text: String
)

/** tools/call 成功时 result */
@Keep
data class McpToolsCallSuccessResult(
    @SerializedName("content") val content: List<McpContentItem>,
    @SerializedName("files") val files: List<McpResultFile>? = null
)

/** resources/list 返回的 resource 项 */
@Keep
data class McpResourceInfo(
    @SerializedName("uri") val uri: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("mimeType") val mimeType: String? = null
)

/** resources/read 返回的 content 项 */
@Keep
data class McpResourceContent(
    @SerializedName("uri") val uri: String? = null,
    @SerializedName("mimeType") val mimeType: String? = null,
    @SerializedName("text") val text: String? = null
)

/** MCP JSON-RPC 请求 */
@Keep
data class McpRpcRequest(
    @SerializedName("id") val id: Any?,
    @SerializedName("method") val method: String?,
    @SerializedName("params") val params: JsonObject?
)

/** MCP JSON-RPC 错误（含可选 files） */
@Keep
data class McpRpcError(
    @SerializedName("code") val code: Int,
    @SerializedName("message") val message: String?,
    @SerializedName("files") val files: List<McpResultFile>? = null
)

/** tools/call 参数 */
@Keep
data class McpToolsCallParams(
    @SerializedName("name") val name: String,
    @SerializedName("arguments") val arguments: JsonObject?
)

/** 工具信息（tools/list 返回） */
@Keep
data class McpToolInfo(
    @SerializedName("name") val name: String? = null,
    @SerializedName("extraName") val extraName: String? = null,
    @SerializedName("extraType") val extraType: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("inputSchema") val inputSchema: JsonObject? = null
)

/** content 项 */
@Keep
data class McpContentItem(
    @SerializedName("type") val type: String,
    @SerializedName("text") val text: String? = null,
    @SerializedName("data") val data: String? = null,
    @SerializedName("mimeType") val mimeType: String? = null
)

/** resources/read 参数 */
@Keep
data class McpResourcesReadParams(
    @SerializedName("uri") val uri: String
)

/** prompts/get 参数 */
@Keep
data class McpPromptsGetParams(
    @SerializedName("name") val name: String,
    @SerializedName("arguments") val arguments: JsonObject?
)
