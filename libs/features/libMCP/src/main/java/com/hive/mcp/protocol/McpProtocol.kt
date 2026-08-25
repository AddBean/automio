// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.mcp.protocol

import android.content.Context
import com.hive.mcp.model.McpContentItem
import com.hive.mcp.model.McpInitializeResult
import com.hive.mcp.model.McpPromptContent
import com.hive.mcp.model.McpCapabilities
import com.hive.mcp.model.McpPromptInfo
import com.hive.mcp.model.McpPromptMessage
import com.hive.mcp.model.McpPromptsGetParams
import com.hive.mcp.model.McpPromptsListResult
import com.hive.mcp.model.McpPromptsGetResult
import com.hive.mcp.model.McpResourcesReadParams
import com.hive.mcp.model.McpResourcesReadResult
import com.hive.mcp.model.McpResourceContent
import com.hive.mcp.model.McpResourceInfo
import com.hive.mcp.model.McpResourcesListResult
import com.hive.mcp.model.McpRpcError
import com.hive.mcp.model.McpRpcRequest
import com.hive.mcp.model.McpRpcResponse
import com.hive.mcp.model.McpServerInfo
import com.hive.mcp.model.McpToolsCallParams
import com.hive.mcp.model.McpToolsCallSuccessResult
import com.hive.mcp.model.McpToolsListResult
import com.hive.mcp.model.McpToolInfo
import com.hive.plugin.mcp.model.McpResultFile
import com.hive.mcp.registry.PromptManager
import com.hive.mcp.registry.ResourceManager
import com.hive.mcp.registry.ToolRegistry
import com.hive.utils.utils.GsonHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetAddress

/**
 * MCP 客户端连接上下文，用于区分内部/外部调用来源
 */
data class McpClientContext(
    val remoteAddress: String? = null,
    val headers: Map<String, String> = emptyMap()
) {
    val isInternalClient: Boolean
        get() {
            val isLoopback = try {
                val addr = remoteAddress
                    ?.removePrefix("/")
                    ?.substringBefore(":")
                addr != null && InetAddress.getByName(addr).isLoopbackAddress
            } catch (_: Exception) {
                false
            }
            if (!isLoopback) return false
            val clientHeader = headers["x-hive-mcp-client"]
            return clientHeader == "app-agent"
        }
}

/**
 * Android兼容的MCP协议处理器
 * 处理JSON-RPC 2.0和MCP协议消息
 * 全部使用预定义 model，无 JsonObject 构建
 */
class McpProtocol(private val context: Context) {

    suspend fun processMessage(
        message: String,
        toolRegistry: ToolRegistry,
        resourceManager: ResourceManager,
        promptManager: PromptManager,
        clientContext: McpClientContext = McpClientContext()
    ): String = withContext(Dispatchers.IO) {
        try {
            val request = GsonHelper.getInstance().fromJson(message, McpRpcRequest::class.java)
            when (request.method) {
                "initialize" -> handleInitialize(request)
                "tools/list" -> handleToolsList(request, toolRegistry)
                "tools/call" -> handleToolsCall(request, toolRegistry, clientContext)
                "resources/list" -> handleResourcesList(request, resourceManager)
                "resources/read" -> handleResourcesRead(request, resourceManager)
                "prompts/list" -> handlePromptsList(request, promptManager)
                "prompts/get" -> handlePromptsGet(request, promptManager)
                else -> handleUnknownMethod(request, request.method)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            toJson(createErrorResponse(null, -32700, context.getString(com.hive.i8n.R.string.mcp_parse_error, e.message)))
        }
    }

    private fun handleInitialize(request: McpRpcRequest): String {
        val result = McpInitializeResult(
            protocolVersion = "2024-11-05",
            capabilities = McpCapabilities(),
            serverInfo = McpServerInfo(
                name = context.getString(com.hive.i8n.R.string.mcp_server_name),
                version = context.getString(com.hive.i8n.R.string.mcp_server_version),
                description = context.getString(com.hive.i8n.R.string.mcp_server_description)
            )
        )
        return toJson(McpRpcResponse(id = request.id, result = result))
    }

    private fun handleToolsList(request: McpRpcRequest, toolRegistry: ToolRegistry): String {
        val tools = toolRegistry.getTools().values.map { tool ->
            McpToolInfo(
                name = tool.name,
                extraName = tool.extraName,
                extraType = tool.extraType,
                description = tool.description,
                inputSchema = tool.inputSchema
            )
        }
        val result = McpToolsListResult(tools = tools)
        return toJson(McpRpcResponse(id = request.id, result = result))
    }

    private suspend fun handleToolsCall(
        request: McpRpcRequest,
        toolRegistry: ToolRegistry,
        clientContext: McpClientContext = McpClientContext()
    ): String {
        val params = request.params ?: return toJson(createErrorResponse(request.id, -32602, context.getString(com.hive.i8n.R.string.mcp_missing_tool_name)))
        val callParams = try {
            GsonHelper.getInstance().fromJson(params.toString(), McpToolsCallParams::class.java)
        } catch (e: Exception) {
            return toJson(createErrorResponse(request.id, -32602, context.getString(com.hive.i8n.R.string.mcp_missing_tool_name)))
        }
        val toolName = callParams.name.ifBlank { null } ?: return toJson(createErrorResponse(request.id, -32602, context.getString(com.hive.i8n.R.string.mcp_missing_tool_name)))
        val arguments = callParams.arguments ?: com.google.gson.JsonObject()

        val tool = toolRegistry.getTools()[toolName]
            ?: return toJson(createErrorResponse(request.id, -32601, context.getString(com.hive.i8n.R.string.mcp_tool_not_exist, toolName)))

        val isInternal = clientContext.isInternalClient

        return try {
            val actionResult = tool.handler(arguments)
            val files = actionResult.files
            if (actionResult.success) {
                val textMain = if (actionResult.data != null) GsonHelper.getInstance().toJson(actionResult.data) else actionResult.message ?: context.getString(com.hive.i8n.R.string.mcp_tool_execution_success)
                val contentItems = mutableListOf<McpContentItem>().apply {
                    add(McpContentItem(type = "text", text = textMain))
                    actionResult.extra?.toString()?.let { extraText ->
                        if (extraText.isNotBlank()) {
                            add(McpContentItem(type = "text", text = extraText))
                        }
                    }
                    files?.forEach { file ->
                        if (file.mimeType?.startsWith("image/") == true) {
                            val imageData = extractImageDataForContent(file, isInternal)
                            if (imageData != null) {
                                add(McpContentItem(
                                    type = "image",
                                    data = imageData,
                                    mimeType = file.mimeType
                                ))
                            }
                        }
                    }
                }
                val responseFiles = if (isInternal) {
                    files?.map { it.copy(base64 = null) }
                } else {
                    files
                }
                val result = McpToolsCallSuccessResult(
                    content = contentItems,
                    files = responseFiles
                )
                toJson(McpRpcResponse(id = request.id, result = result))
            } else {
                val msgResult = actionResult.message ?: context.getString(com.hive.i8n.R.string.mcp_tool_execution_failed_hardcoded)
                val msgExtra = actionResult.extra?.toString()?.trim()
                val msgContent = if (msgExtra.isNullOrBlank()) {
                    context.getString(com.hive.i8n.R.string.mcp_tool_result_format_simple, msgResult).trimIndent()
                } else {
                    context.getString(com.hive.i8n.R.string.mcp_tool_result_format, msgResult, msgExtra).trimIndent()
                }
                val responseFiles = if (isInternal) {
                    files?.map { it.copy(base64 = null) }
                } else {
                    files
                }
                val error = McpRpcError(code = -32000, message = msgContent, files = responseFiles)
                toJson(McpRpcResponse(id = request.id, error = error))
            }
        } catch (e: Exception) {
            toJson(createErrorResponse(request.id, -32603, context.getString(com.hive.i8n.R.string.mcp_tool_execution_failed, e.message)))
        }
    }

    private fun handleResourcesList(request: McpRpcRequest, resourceManager: ResourceManager): String {
        val resources = resourceManager.getResources().values.map { r ->
            McpResourceInfo(uri = r.uri, name = r.name, description = r.description, mimeType = r.mimeType)
        }
        val result = McpResourcesListResult(resources = resources)
        return toJson(McpRpcResponse(id = request.id, result = result))
    }

    private suspend fun handleResourcesRead(request: McpRpcRequest, resourceManager: ResourceManager): String {
        val params = request.params ?: return toJson(createErrorResponse(request.id, -32602, context.getString(com.hive.i8n.R.string.mcp_missing_resource_uri)))
        val readParams = try {
            GsonHelper.getInstance().fromJson(params.toString(), McpResourcesReadParams::class.java)
        } catch (e: Exception) {
            return toJson(createErrorResponse(request.id, -32602, context.getString(com.hive.i8n.R.string.mcp_missing_resource_uri)))
        }
        val uri = readParams.uri.ifBlank { null } ?: return toJson(createErrorResponse(request.id, -32602, context.getString(com.hive.i8n.R.string.mcp_missing_resource_uri)))

        val resource = resourceManager.getResources()[uri]
            ?: return toJson(createErrorResponse(request.id, -32601, context.getString(com.hive.i8n.R.string.mcp_resource_not_exist, uri)))

        return try {
            val result = resource.handler()
            val contents = listOf(McpResourceContent(uri = uri, mimeType = "application/json", text = result.toString()))
            val resultObj = McpResourcesReadResult(contents = contents)
            toJson(McpRpcResponse(id = request.id, result = resultObj))
        } catch (e: Exception) {
            toJson(createErrorResponse(request.id, -32603, context.getString(com.hive.i8n.R.string.mcp_resource_read_failed, e.message)))
        }
    }

    private fun handlePromptsList(request: McpRpcRequest, promptManager: PromptManager): String {
        val prompts = promptManager.getPrompts().values.map { p ->
            McpPromptInfo(name = p.name, description = p.description, arguments = p.arguments)
        }
        val result = McpPromptsListResult(prompts = prompts)
        return toJson(McpRpcResponse(id = request.id, result = result))
    }

    private suspend fun handlePromptsGet(request: McpRpcRequest, promptManager: PromptManager): String {
        val params = request.params ?: return toJson(createErrorResponse(request.id, -32602, context.getString(com.hive.i8n.R.string.mcp_missing_prompt_name)))
        val getParams = try {
            GsonHelper.getInstance().fromJson(params.toString(), McpPromptsGetParams::class.java)
        } catch (e: Exception) {
            return toJson(createErrorResponse(request.id, -32602, context.getString(com.hive.i8n.R.string.mcp_missing_prompt_name)))
        }
        val name = getParams.name.ifBlank { null } ?: return toJson(createErrorResponse(request.id, -32602, context.getString(com.hive.i8n.R.string.mcp_missing_prompt_name)))
        val arguments = getParams.arguments ?: com.google.gson.JsonObject()

        val prompt = promptManager.getPrompts()[name]
            ?: return toJson(createErrorResponse(request.id, -32601, context.getString(com.hive.i8n.R.string.mcp_prompt_not_exist, name)))

        return try {
            val result = prompt.handler(arguments)
            val messages = listOf(
                McpPromptMessage(
                    role = "user",
                    content = McpPromptContent(type = "string", text = result.toString())
                )
            )
            val resultObj = McpPromptsGetResult(
                description = context.getString(com.hive.i8n.R.string.mcp_automated_workflow_prompt),
                messages = messages
            )
            toJson(McpRpcResponse(id = request.id, result = resultObj))
        } catch (e: Exception) {
            toJson(createErrorResponse(request.id, -32603, context.getString(com.hive.i8n.R.string.mcp_prompt_generation_failed, e.message)))
        }
    }

    private fun handleUnknownMethod(request: McpRpcRequest, method: String?): String {
        return toJson(createErrorResponse(request.id, -32601, context.getString(com.hive.i8n.R.string.mcp_unknown_method, method)))
    }

    private fun createErrorResponse(requestId: Any?, code: Int, message: String): McpRpcResponse {
        return McpRpcResponse(id = requestId, error = McpRpcError(code = code, message = message))
    }

    private fun toJson(response: McpRpcResponse): String {
        return GsonHelper.getInstance().toJson(response)
    }

    /**
     * 从 McpResultFile 中提取用于 content 的图片数据。
     * 内部客户端（app-agent）：优先使用 blob URL，避免在聊天记录中保存大体积 base64。
     * 外部客户端：优先使用 base64（更通用，Cursor 等客户端都支持）。
     */
    private fun extractImageDataForContent(file: McpResultFile, isInternalClient: Boolean = false): String? {
        val base64 = file.base64
        val url = file.url

        return if (isInternalClient) {
            when {
                !url.isNullOrBlank() && url.startsWith("http") -> url
                !url.isNullOrBlank() -> url
                !base64.isNullOrBlank() -> extractPureBase64FromDataUrl(base64)
                else -> null
            }
        } else {
            when {
                !base64.isNullOrBlank() -> extractPureBase64FromDataUrl(base64)
                !url.isNullOrBlank() && url.startsWith("http") -> url
                else -> null
            }
        }
    }

    /**
     * 从 data URL 中提取纯 base64 字符串
     * 
     * @param dataUrl 可能是 data URL 格式（data:image/jpeg;base64,xxx）或纯 base64 字符串
     * @return 纯 base64 字符串
     */
    private fun extractPureBase64FromDataUrl(dataUrl: String): String {
        return if (dataUrl.startsWith("data:")) {
            val commaIndex = dataUrl.indexOf(',')
            if (commaIndex > 0) {
                dataUrl.substring(commaIndex + 1)
            } else {
                dataUrl
            }
        } else {
            dataUrl
        }
    }
}
