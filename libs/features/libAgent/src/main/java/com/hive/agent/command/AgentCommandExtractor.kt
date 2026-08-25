// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.agent.command

import com.google.gson.JsonObject
import com.hive.plugin.agent.model.ChatMessage
import com.hive.plugin.agent.model.MessageRole
import com.hive.plugin.agent.model.ToolCall
import com.hive.agent.storage.AgentSessionStorage
import com.hive.agent.storage.LoadedSession
import com.hive.plugin.mcp.model.McpAction
import com.hive.plugin.mcp.model.McpActionParameters
import com.hive.script.mcp.ScriptMcpRegister
import com.hive.utils.GlobalApp
import com.hive.utils.debug.DLog

/**
 * Agent 命令提取器：从 Session 的 Messages 中提取完整的工具调用历史。
 *
 * 与录制器方式对比：
 * - 录制器：依赖实时录制，可能因清空、中断导致丢失
 * - 提取器：从历史 Messages 提取，数据完整准确
 *
 * 使用场景：
 * - Agent 会话转工作流
 * - 命令历史回溯分析
 * - 数据恢复和备份
 */
object AgentCommandExtractor {

    private const val TAG = "AgentCommandExtractor"
    private val sessionStorage = AgentSessionStorage(GlobalApp.getContext())

    /**
     * 从 session 提取完整的命令历史（按时间顺序）。
     *
     * @param sessionKey Session 唯一标识
     * @return 命令字符串列表（已展平，不含空行）
     */
    fun extractCommandsFromSession(sessionKey: String): List<String> {
        val session = sessionStorage.loadSession(sessionKey)
        if (session == null) {
            DLog.w(TAG, "Session not found: $sessionKey")
            return emptyList()
        }

        return extractCommandsFromMessages(session.messages)
    }

    /**
     * 从消息列表提取命令（供外部调用）。
     *
     * @param messages ChatMessage 列表
     * @return 命令字符串列表
     */
    fun extractCommandsFromMessages(messages: List<ChatMessage>): List<String> {
        return messages
            .filter { it.role == MessageRole.ASSISTANT && it.toolCalls != null }
            .flatMap { message ->
                message.toolCalls?.mapNotNull { toolCall ->
                    extractCommandFromToolCall(toolCall)
                } ?: emptyList()
            }
            .filter { it.isNotBlank() }
    }

    /**
     * 从单个 ToolCall 提取命令字符串。
     *
     * @param toolCall AI 生成的工具调用
     * @return 命令字符串或 null（提取失败）
     */
    private fun extractCommandFromToolCall(toolCall: ToolCall): String? {
        try {
            val actionName = toolCall.function.name
            val arguments = toolCall.function.arguments

            DLog.d(TAG, "Extracting command: $actionName with args: $arguments")

            // 1. 查找对应的工具
            val tool = ScriptMcpRegister.findToolByActionPublic(actionName)
            if (tool == null) {
                DLog.w(TAG, "Tool not found for action: $actionName")
                // 尝试处理自定义工具
                return handleCustomTool(actionName, arguments)
            }

            // 2. 解析参数（JsonObject -> Map<String, String>)
            val params = parseArguments(arguments)

            // 3. 检查参数有效性（使用工具的检查逻辑）
            val mcpAction = buildMcpActionFromToolCall(actionName, tool, params)
            val checkResult = tool.checkAction(mcpAction)
            if (!checkResult.success) {
                DLog.w(TAG, "Action check failed for $actionName: ${checkResult.message}")
                return null
            }

            // 4. 安全构建命令对象（不执行实际操作）
            val cmd = tool.buildCommand(params)
            if (cmd == null) {
                DLog.w(TAG, "Failed to build command for $actionName")
                return null
            }

            // 5. 获取命令字符串
            return cmd.getCommandLines()

        } catch (e: Exception) {
            DLog.e(TAG, "Failed to extract command from toolCall: ${e.message}", e)
            return null
        }
    }

    /**
     * 解析 JsonObject 参数为 Map<String, String>。
     */
    private fun parseArguments(jsonObject: JsonObject): Map<String, String> {
        return jsonObject.entrySet().associate { (key, value) ->
            key to when {
                value.isJsonPrimitive -> value.asString
                value.isJsonNull -> ""
                value.isJsonObject || value.isJsonArray -> value.toString() // 复杂类型转字符串
                else -> value.toString()
            }
        }
    }

    /**
     * 构建 McpAction 对象（用于工具检查）。
     */
    private fun buildMcpActionFromToolCall(
        actionName: String,
        tool: com.hive.script.mcp.McpToolBuilder,
        params: Map<String, String>
    ): McpAction {
        val toolAction = tool.getToolAction() ?: McpAction(action = actionName)

        return McpAction(
            action = actionName,
            paramInfo = toolAction.paramInfo,
            paramValues = params,
            description = toolAction.description,
            extraName = toolAction.extraName,
            extraType = toolAction.extraType
        )
    }

    /**
     * 处理自定义工具（custom.*）。
     * 自定义工具通常调用脚本，生成 callScript 命令。
     */
    private fun handleCustomTool(actionName: String, arguments: JsonObject): String? {
        if (!actionName.startsWith("custom.")) {
            return null
        }

        try {
            // 自定义工具可能包含 scriptPath 或 toolId 参数
            val scriptPath = arguments.get("scriptPath")?.asString
            val toolId = arguments.get("toolId")?.asString

            if (scriptPath != null) {
                return "callScript(\"$scriptPath\")"
            }

            // 如果有 toolId，尝试从缓存获取脚本路径
            // （这部分需要 ScriptMcpRegister 的 customToolRuntimeCache 支持）
            DLog.w(TAG, "Custom tool $actionName has no scriptPath, cannot generate command")
            return null

        } catch (e: Exception) {
            DLog.e(TAG, "Failed to handle custom tool: ${e.message}")
            return null
        }
    }
}