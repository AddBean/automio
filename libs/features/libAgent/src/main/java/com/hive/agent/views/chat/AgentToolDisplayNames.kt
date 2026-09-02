// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.agent.views.chat

import com.hive.agent.XAgent
import com.hive.agent.mcp.McpToolClient
import com.hive.plugin.ComponentManager
import com.hive.plugin.agent.model.ToolCall
import com.hive.plugin.mcp.McpConst
import com.hive.plugin.provider.IMcpProvider
import com.hive.script.mcp.ScriptMcpRegister
import com.hive.utils.GlobalApp.getString

/**
 * 工具显示名解析（列表 chip 与详情 sheet 共用）。
 */
object AgentToolDisplayNames {

    fun resolve(toolCall: ToolCall): String {
        val functionName = toolCall.function.name
        val displayName = resolveSkillDisplayName(toolCall)
            ?: resolveFromAgentToolClients(functionName)
            ?: resolveFromMcpRegistry(functionName)
            ?: resolveFromScriptMcpCache(functionName)
            ?: humanizeToolName(functionName)
        return displayName.limitToTitleLength()
    }

    private fun resolveFromAgentToolClients(functionName: String): String? {
        return XAgent.getInstance()
            .getRegisteredTools()
            .asSequence()
            .filterIsInstance<McpToolClient>()
            .mapNotNull { it.resolveDisplayName(functionName) }
            .firstOrNull()
    }

    private fun resolveFromMcpRegistry(functionName: String): String? {
        val mcpProvider = ComponentManager.getInstance()
            .getProvider(IMcpProvider::class.java) as? IMcpProvider ?: return null
        val tools = mcpProvider.getRegisteredTools()
        for (candidate in buildNameCandidates(functionName)) {
            tools.firstOrNull { it.name == candidate }?.let { tool ->
                return tool.extraName.takeIf { it.isNotBlank() }
                    ?: tool.description.takeIf { it.isNotBlank() }
            }
        }
        return null
    }

    private fun resolveFromScriptMcpCache(functionName: String): String? {
        val action = ScriptMcpRegister.findToolByActionPublic(functionName)?.getAction() ?: return null
        return action.extraName?.takeIf { it.isNotBlank() }
            ?: action.description?.takeIf { it.isNotBlank() }
    }

    private fun buildNameCandidates(functionName: String): List<String> {
        val names = linkedSetOf(functionName)
        val lastDot = functionName.lastIndexOf('.')
        if (lastDot > 0 && lastDot < functionName.lastIndex) {
            names.add(functionName.substring(lastDot + 1))
        }
        if (!functionName.startsWith(McpConst.Tool_Name_Prefix_BuildIn) &&
            !functionName.startsWith(McpConst.Tool_Name_Prefix_Custom)
        ) {
            val suffix = functionName.substringAfterLast('.')
            if (suffix.isNotBlank()) {
                names.add(McpConst.Tool_Name_Prefix_BuildIn + suffix)
                names.add(McpConst.Tool_Name_Prefix_Custom + suffix)
            }
        }
        return names.toList()
    }

    private fun humanizeToolName(functionName: String): String {
        val raw = when {
            functionName.startsWith(McpConst.Tool_Name_Prefix_BuildIn) ->
                functionName.removePrefix(McpConst.Tool_Name_Prefix_BuildIn)
            functionName.startsWith(McpConst.Tool_Name_Prefix_Custom) ->
                functionName.removePrefix(McpConst.Tool_Name_Prefix_Custom)
            functionName.contains('.') -> functionName.substringAfterLast('.')
            else -> functionName
        }
        return raw
            .replace('_', ' ')
            .replace(Regex("([a-z0-9])([A-Z])"), "$1 $2")
            .split(' ')
            .filter { it.isNotBlank() }
            .joinToString(" ") { word ->
                word.replaceFirstChar { ch ->
                    if (ch.isLowerCase()) ch.titlecase() else ch.toString()
                }
            }
            .ifBlank { functionName }
    }

    fun formatArguments(toolCall: ToolCall): String {
        return try {
            toolCall.function.arguments.entrySet().joinToString(", ") { (k, v) ->
                val str = v?.takeIf { it.isJsonPrimitive }?.asJsonPrimitive?.asString ?: v?.toString() ?: ""
                "$k=$str"
            }
        } catch (_: Exception) {
            "${toolCall.function.arguments}"
        }
    }

    private fun resolveSkillDisplayName(toolCall: ToolCall): String? {
        return when (toolCall.function.name) {
            McpConst.Tool_Name_Prefix_BuildIn + "skill" -> resolveUnifiedSkillActionName(toolCall)
            else -> null
        }
    }

    private fun resolveUnifiedSkillActionName(toolCall: ToolCall): String {
        val action = toolCall.function.arguments
            .get("action")
            ?.takeIf { it.isJsonPrimitive }
            ?.asJsonPrimitive
            ?.asString
            ?.trim()
            ?.lowercase()
            .orEmpty()
        return when (action) {
            "help" -> getString(com.hive.i8n.R.string.agent_skill_action_help)
            "list" -> getString(com.hive.i8n.R.string.agent_skill_action_list)
            "run" -> getString(com.hive.i8n.R.string.agent_skill_action_run)
            "create" -> getString(com.hive.i8n.R.string.agent_skill_action_create)
            "update" -> getString(com.hive.i8n.R.string.agent_skill_action_update)
            "delete" -> getString(com.hive.i8n.R.string.agent_skill_action_delete)
            else -> getString(com.hive.i8n.R.string.agent_skill_action_default)
        }
    }

    private fun String.limitToTitleLength(maxLength: Int = 24): String {
        return if (length <= maxLength) this else take(maxLength - 3) + "..."
    }
}
