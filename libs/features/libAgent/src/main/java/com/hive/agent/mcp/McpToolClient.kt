// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.agent.mcp

import com.google.gson.JsonObject
import com.hive.agent.config.AIAgentConfig
import com.hive.mcp.model.McpRpcResponse
import com.hive.mcp.model.McpRpcResult
import com.hive.plugin.mcp.model.McpResultFile
import com.hive.mcp.model.McpToolInfo
import com.hive.plugin.agent.AgentToolClient
import com.hive.plugin.mcp.McpConst
import com.hive.plugin.agent.model.ChatAttachment
import com.hive.plugin.agent.model.AgentError
import com.hive.plugin.agent.model.AgentErrorCode
import com.hive.plugin.agent.model.AgentRequest
import com.hive.plugin.agent.model.AgentResult
import com.hive.plugin.agent.model.AttachmentType
import com.hive.plugin.agent.model.FunctionDefinition
import com.hive.plugin.agent.model.ToolDefinition
import com.hive.utils.GlobalApp
import com.hive.utils.extends.toGsonJsonObject
import com.hive.utils.utils.GsonHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.Proxy
import java.util.concurrent.TimeUnit

/**
 * MCP工具客户端
 */
class McpToolClient(
    val serverId: String,
    val serverUrl: String
) : AgentToolClient {

    override val id = "mcp:$serverId"
    override val name = GlobalApp.getString(com.hive.i8n.R.string.agent_mcp_tool_client)
    override val description = GlobalApp.getString(com.hive.i8n.R.string.agent_mcp_tool_description)

    override val supportedMethods = mutableListOf<String>()

    private val tools: MutableList<LocalMcpToolInfo> = mutableListOf()

    private data class LocalMcpToolInfo(
        val exposedName: String,
        val rawName: String,
        val displayName: String,
        val description: String,
        val parameters: JsonObject,
        val inputSchema: JsonObject?
    )

    override suspend fun initTools() {
        refreshTools()
    }

    suspend fun refreshTools() {
        // 构建新列表：远端 tools/list + 本地内置“入口方法”
        val newTools: MutableList<LocalMcpToolInfo> = mutableListOf()

        val serverTools = handleListToolsFromServer()
        if (serverTools.success) {
            serverTools.data?.let { result ->
                @Suppress("UNCHECKED_CAST")
                (result as? List<LocalMcpToolInfo>)?.forEach { newTools.add(it) }
            }
        }

        // 这两个是“入口方法”，用于让模型能直接调用 MCP 的 tools/list 与 resources/list
        val toolsListParams = defaultParametersSchema()
        newTools.add(
            LocalMcpToolInfo(
                exposedName = localToolsListExposedName(),
                rawName = "tools_list",
                displayName = GlobalApp.getString(com.hive.i8n.R.string.agent_mcp_get_tools_list),
                description = GlobalApp.getString(com.hive.i8n.R.string.agent_mcp_get_tools_list),
                parameters = toolsListParams,
                inputSchema = toolsListParams
            )
        )
//        val resourcesListParams = JsonObject().apply {
//            addProperty("type", "object")
//            add("properties", JsonObject().apply {
//                add("uri", JsonObject().apply {
//                    addProperty("type", "string")
//                    addProperty(
//                        "description",
//                        GlobalApp.getString(com.hive.i8n.R.string.agent_mcp_resource_uri)
//                    )
//                })
//            })
//            add("required", GsonHelper.getInstance().getGson().toJsonTree(listOf("uri")))
//        }
//        newTools.add(
//            LocalMcpToolInfo(
//                exposedName = "${serverId}.resources_list",
//                rawName = "resources_list",
//                description = GlobalApp.getString(com.hive.i8n.R.string.agent_mcp_get_resources_list),
//                parameters = resourcesListParams,
//                inputSchema = resourcesListParams
//            )
//        )

        tools.clear()
        tools.addAll(newTools)

        supportedMethods.clear()
        tools.forEach { supportedMethods.add(it.exposedName) }
    }

    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .proxy(Proxy.NO_PROXY)
        .build()

    private suspend fun sendMessage(
        action: String,
        function: String,
        params: Map<String, Any>
    ): String {
        val messageId = java.util.UUID.randomUUID().toString()
        val paramsObj = JsonObject().apply {
            addProperty("name", function)
            add(
                "arguments",
                (params["arguments"] as? Map<String, Any>)?.toGsonJsonObject() ?: JsonObject()
            )
        }
        val requestBody = JsonObject().apply {
            addProperty("id", messageId)
            addProperty("method", action)
            add("params", paramsObj)
        }

        val request = Request.Builder()
            .url(serverUrl)
            .header(HEADER_MCP_CLIENT, HEADER_MCP_CLIENT_VALUE)
            .post(
                okhttp3.RequestBody.create(
                    "application/json".toMediaTypeOrNull(),
                    GsonHelper.getInstance().toJson(requestBody)
                )
            )
            .build()

        return withContext(Dispatchers.IO) {
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                throw Exception("HTTP error ${response.code}")
            }
            response.body?.string() ?: throw Exception("Empty response")
        }
    }

    override suspend fun execute(request: AgentRequest): AgentResult<*> {
        return try {
            when (request.action) {
                "tools_call" -> handleCallTool(request.params as Map<String, Any>, request.taskId)
                "tools_list" -> handleListTools()
                "resources_list" -> handleReadResource(request.params as Map<String, Any>)
                else -> handleDefaultCallTool(request)
            }
        } catch (e: Exception) {
            AgentResult.Failure(
                AgentError(
                    code = AgentErrorCode.EXECUTION_FAILED,
                    msg = e.message
                        ?: GlobalApp.getString(com.hive.i8n.R.string.agent_execution_failed),
                    e = e
                )
            )
        }
    }

    override fun stopExecute() {
        client.dispatcher.executorService.shutdownNow()
        client.connectionPool.evictAll()
        client.cache?.close()
    }

    private suspend fun handleDefaultCallTool(request: AgentRequest): AgentResult<*> {
        val params = request.params as? Map<String, Any>
            ?: return AgentResult.Failure(
                AgentError(
                    code = AgentErrorCode.INVALID_PARAMS,
                    msg = GlobalApp.getString(com.hive.i8n.R.string.agent_missing_params)
                )
            )
        return handleCallToolInner(request.action, params, request.taskId)
    }

    private suspend fun handleCallTool(params: Map<String, Any>, taskId: String?): AgentResult<*> {
        val toolName = (params["name"] as? String)?.let { resolveRawToolName(it) }
            ?: return AgentResult.Failure(
                AgentError(
                    code = AgentErrorCode.INVALID_PARAMS,
                    msg = GlobalApp.getString(com.hive.i8n.R.string.agent_missing_name_param)
                )
            )
        val arguments = params["arguments"] as? Map<String, Any> ?: emptyMap()
        return handleCallToolInner(toolName, arguments, taskId)
    }

    private suspend fun handleCallToolInner(
        toolName: String,
        args: Map<String, Any>,
        taskId: String?
    ): AgentResult<*> {
        return try {
            val arguments = mutableMapOf<String, Any>()
            arguments["name"] = toolName
            arguments["arguments"] = args
            val response = sendMessage("tools/call", toolName, arguments)
            val rpc = GsonHelper.getInstance().fromJson(response, McpRpcResponse::class.java)
            val mcpResult = rpc.result?.let {
                GsonHelper.getInstance()
                    .fromJson(GsonHelper.getInstance().toJson(it), McpRpcResult::class.java)
            }
            val content = mcpResult?.content
            val text0 = content?.getOrNull(0)?.text
            val text1 = content?.getOrNull(1)?.text
            val extraFiles = mcpResult?.files?.mapNotNull { f: McpResultFile ->
                ChatAttachment(
                    type = AttachmentType.IMAGE,
                    url = f.url,
                    base64 = f.base64,
                    mimeType = f.mimeType
                )
            }

            val err = rpc.error
            if (err == null && text0 != null) {
                AgentResult.Success(data = text0, extra = text1, files = extraFiles)
            } else {
                AgentResult.Failure(
                    AgentError(
                        code = AgentErrorCode.EXECUTION_FAILED,
                        msg = err?.message
                            ?: GlobalApp.getString(com.hive.i8n.R.string.agent_call_failed_no_error)
                    )
                )
            }
        } catch (e: Exception) {
            AgentResult.Failure(
                AgentError(
                    code = AgentErrorCode.EXECUTION_FAILED,
                    msg = e.message ?: GlobalApp.getString(com.hive.i8n.R.string.agent_call_failed),
                    e = e
                )
            )
        }
    }

    private suspend fun handleListTools(): AgentResult<*> {
        // tools_list 对应 MCP 的 tools/list
        return handleListToolsFromServer()
    }

    private suspend fun handleListToolsFromServer(): AgentResult<*> {
        return try {
            val response = sendMessage("tools/list", "", emptyMap())
            val rpc = GsonHelper.getInstance().fromJson(response, McpRpcResponse::class.java)
            val err = rpc.error
            if (err != null) {
                AgentResult.Failure(
                    AgentError(
                        code = AgentErrorCode.EXECUTION_FAILED,
                        msg = err.message
                            ?: GlobalApp.getString(com.hive.i8n.R.string.agent_get_tools_list_failed)
                    )
                )
            } else {
                val mcpResult = rpc.result?.let {
                    GsonHelper.getInstance()
                        .fromJson(GsonHelper.getInstance().toJson(it), McpRpcResult::class.java)
                }
                val toolsList = mcpResult?.tools ?: emptyList()
                val list = toolsList.mapNotNull { t: McpToolInfo ->
                    val name = t.name ?: return@mapNotNull null
                    val displayName = t.extraName?.trim().takeUnless { it.isNullOrEmpty() } ?: name
                    val description = t.description ?: ""
                    val inputSchema = t.inputSchema ?: defaultParametersSchema()
                    val exposedName = if (shouldExposeCanonicalLocalToolName(name)) name else "$serverId.$name"
                    LocalMcpToolInfo(exposedName, name, displayName, description, inputSchema, inputSchema)
                }
                AgentResult.Success(data = list)
            }
        } catch (e: Exception) {
            AgentResult.Failure(
                AgentError(
                    code = AgentErrorCode.EXECUTION_FAILED,
                    msg = e.message
                        ?: GlobalApp.getString(com.hive.i8n.R.string.agent_get_tools_list_failed),
                    e = e
                )
            )
        }
    }

    private suspend fun handleReadResource(params: Map<String, Any>): AgentResult<*> {
        val resourceUri = params["uri"] as? String
            ?: return AgentResult.Failure(
                AgentError(
                    code = AgentErrorCode.INVALID_PARAMS,
                    msg = GlobalApp.getString(com.hive.i8n.R.string.agent_missing_uri_param)
                )
            )
        return try {
            val response = sendMessage("resources/list", "", mapOf("uri" to resourceUri))
            val rpc = GsonHelper.getInstance().fromJson(response, McpRpcResponse::class.java)
            val err = rpc.error
            if (err != null) {
                AgentResult.Failure(
                    AgentError(
                        code = AgentErrorCode.EXECUTION_FAILED,
                        msg = err.message
                            ?: GlobalApp.getString(com.hive.i8n.R.string.agent_read_resource_failed)
                    )
                )
            } else {
                AgentResult.Success(data = rpc.result)
            }
        } catch (e: Exception) {
            AgentResult.Failure(
                AgentError(
                    code = AgentErrorCode.EXECUTION_FAILED,
                    msg = e.message
                        ?: GlobalApp.getString(com.hive.i8n.R.string.agent_read_resource_failed),
                    e = e
                )
            )
        }
    }

    private fun defaultParametersSchema(): JsonObject = JsonObject().apply {
        addProperty("type", "object")
        add("properties", JsonObject())
    }

    override fun toToolDefinitions(): List<ToolDefinition> {
        return tools.map {
            val params = it.inputSchema ?: it.parameters
            ToolDefinition(
                type = "function",
                function = FunctionDefinition(
                    name = it.exposedName,
                    description = it.description,
                    parameters = params
                )
            )
        }
    }

    fun resolveRawToolName(functionName: String): String {
        if (functionName == localToolsListExposedName()) return "tools_list"
        if (functionName == localResourcesListExposedName()) return "resources_list"
        if (shouldExposeCanonicalLocalToolName(functionName)) return functionName
        val prefix = "$serverId."
        if (functionName.startsWith(prefix)) {
            return functionName.removePrefix(prefix)
        }
        return functionName
    }

    fun resolveDisplayName(functionName: String): String? {
        val rawName = resolveRawToolName(functionName)
        return tools.firstOrNull {
            it.exposedName == functionName || it.rawName == rawName
        }?.displayName
    }

    private fun localToolsListExposedName(): String {
        return if (serverId == AIAgentConfig.BaseConfig.McpToolName) {
            McpConst.Tool_Name_Prefix_BuildIn + "tools_list"
        } else {
            "$serverId.tools_list"
        }
    }

    private fun localResourcesListExposedName(): String {
        return if (serverId == AIAgentConfig.BaseConfig.McpToolName) {
            McpConst.Tool_Name_Prefix_BuildIn + "resources_list"
        } else {
            "$serverId.resources_list"
        }
    }

    private fun isCanonicalLocalToolName(name: String): Boolean {
        return name.startsWith(McpConst.Tool_Name_Prefix_BuildIn) ||
            name.startsWith(McpConst.Tool_Name_Prefix_Custom)
    }

    private fun shouldExposeCanonicalLocalToolName(name: String): Boolean {
        return serverId == AIAgentConfig.BaseConfig.McpToolName && isCanonicalLocalToolName(name)
    }

    override fun onDestroy() {
        client.connectionPool.evictAll()
    }

    companion object {
        private const val TAG = "McpTool"
        const val HEADER_MCP_CLIENT = "X-Hive-Mcp-Client"
        const val HEADER_MCP_CLIENT_VALUE = "app-agent"
    }
}
