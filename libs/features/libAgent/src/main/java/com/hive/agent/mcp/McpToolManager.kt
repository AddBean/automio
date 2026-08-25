// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.agent.mcp

import com.hive.utils.GlobalApp
import com.hive.agent.core.AgentContext
import com.hive.plugin.agent.model.AgentError
import com.hive.plugin.agent.model.AgentErrorCode
import com.hive.plugin.agent.model.AgentRequest
import com.hive.plugin.agent.model.AgentResult
import java.util.concurrent.ConcurrentHashMap

/**
 * MCP工具管理器
 * 管理MCP服务器连接和工具调用
 */
class McpToolManager(
    private val agentContext: AgentContext
) {

    private val mcpServers = ConcurrentHashMap<String, String>()  // serverId -> serverUrl

    private val mcpTools = ConcurrentHashMap<String, McpToolClient>()

    /**
     * 注册MCP服务器
     */
    suspend fun registerMcpServer(
        serverId: String,
        serverUrl: String
    ): Boolean {
        return try {
            val mcpTool = McpToolClient(serverId, serverUrl)
            mcpTool.initTools()
            mcpServers[serverId] = serverUrl
            mcpTools[serverId] = mcpTool
            agentContext.agentManager.registerTool(mcpTool)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 调用MCP工具
     */
    suspend fun callMcpTool(
        serverId: String,
        toolName: String,
        arguments: Map<String, Any>
    ): AgentResult<*> {
        val mcpTool = mcpTools[serverId]
            ?: return AgentResult.Failure(
                error = AgentError(
                    code = AgentErrorCode.TOOL_NOT_FOUND,
                    msg = GlobalApp.getString(com.hive.i8n.R.string.agent_tool_call_empty_result)
                )
            )

        return try {
            val result = mcpTool.execute(
                AgentRequest(
                    toolId = mcpTool.id,
                    action = "tools_call",
                    params = mapOf(
                        "name" to toolName,
                        "arguments" to arguments
                    )
                )
            )
            result
        } catch (e: Exception) {
            AgentResult.Failure(
                error = AgentError(
                    code = AgentErrorCode.UNKNOWN_ERROR,
                    msg = GlobalApp.getString(
                        com.hive.i8n.R.string.agent_tool_call_exception,
                        e.message ?: ""
                    ),
                    e = e,
                )
            )
        }
    }

    /**
     * 获取已注册的服务器
     */
    fun getRegisteredServers(): List<String> {
        return mcpServers.keys.toList()
    }

    fun getMcpServer(serverId: String): McpToolClient? {
        return mcpTools[serverId]
    }

    /**
     * 注销MCP服务器
     */
    fun unregisterMcpServer(serverId: String) {
        mcpServers.remove(serverId)
        val removedTool = mcpTools.remove(serverId)
        removedTool?.onDestroy()
        removedTool?.let { agentContext.agentManager.unregisterTool(it.id) }
    }
}
