// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.agent.views.chat

import com.hive.agent.XAgent
import com.hive.agent.mcp.McpToolClient
import com.hive.plugin.agent.model.ToolCall
import com.hive.plugin.mcp.McpConst
import com.hive.utils.GlobalApp.getString

/**
 * 工具显示名解析（列表 chip 与详情 sheet 共用）。
 */
object AgentToolDisplayNames {

    fun resolve(toolCall: ToolCall): String {
        val fallbackName = toolCall.function.name
        val displayName = resolveSkillDisplayName(toolCall) ?: XAgent.getInstance()
            .getRegisteredTools()
            .asSequence()
            .filterIsInstance<McpToolClient>()
            .mapNotNull { it.resolveDisplayName(fallbackName) }
            .firstOrNull()
            ?: fallbackName
        return displayName.limitToTitleLength()
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

    private fun String.limitToTitleLength(maxLength: Int = 20): String {
        return if (maxLength !in 4..<length) {
            take(maxLength)
        } else {
            take(maxLength - 3) + "..."
        }
    }
}
