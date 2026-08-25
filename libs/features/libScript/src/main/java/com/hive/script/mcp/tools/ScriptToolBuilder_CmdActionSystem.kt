// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.mcp.tools

import com.hive.plugin.mcp.model.ActionResult
import com.hive.plugin.mcp.model.McpAction
import com.hive.plugin.mcp.model.McpActionParameters
import com.hive.script.base.ScriptCommand
import com.hive.script.cmd.CmdActionBack
import com.hive.script.cmd.CmdActionHome
import com.hive.script.cmd.CmdActionOpenNotifications
import com.hive.script.cmd.CmdActionRecent
import com.hive.script.cmd.CmdActionScreenLock
import com.hive.script.cmd.CmdActionScreenShot
import com.hive.script.extensions.executeSync
import com.hive.script.mcp.CheckActionResult
import com.hive.script.mcp.McpToolBuilder
import com.hive.utils.GlobalApp

@AutoMcpToolsRegister(MCP_IDS.ToolActionSystem)
class ScriptToolBuilder_CmdActionSystem : McpToolBuilder() {

    private var cmd: ScriptCommand = CmdActionBack.createCommand()

    override fun getAction(): McpAction =
        McpAction(
            action = ACTION_NAME,
            extraName = GlobalApp.getString(com.hive.i8n.R.string.tool_action_system_name),
            description = GlobalApp.getString(com.hive.i8n.R.string.tool_action_system_description),
            paramInfo = mutableListOf(
                McpActionParameters(
                    name = PARAM_ACTION_TYPE,
                    type = "string",
                    description = GlobalApp.getString(com.hive.i8n.R.string.tool_action_system_action_type_desc),
                    required = true,
                    examples = listOf(TYPE_BACK, TYPE_HOME, TYPE_RECENT, TYPE_NOTIFICATIONS, TYPE_SCREEN_LOCK, TYPE_SCREENSHOT),
                )
            ),
            paramValues = emptyMap(),
        )

    override fun onCheckAction(action: McpAction): CheckActionResult {
        val actionType = action.paramValues[PARAM_ACTION_TYPE]?.trim().orEmpty()
        if (actionType.isBlank()) {
            return CheckActionResult(false, GlobalApp.getString(com.hive.i8n.R.string.tool_action_system_error_action_type_empty))
        }
        if (actionType !in VALID_TYPES) {
            return CheckActionResult(
                false,
                GlobalApp.getString(com.hive.i8n.R.string.tool_action_system_error_action_type_invalid, VALID_TYPES.joinToString(",")),
            )
        }
        return CheckActionResult(true, null)
    }

    override fun getCommand(): ScriptCommand? = cmd

    override fun withScreenLayout(): Boolean {
        // 截图操作不需要屏幕布局
        return cmd !is CmdActionScreenShot
    }

    /**
     * 创建命令对象（只创建，不执行）
     */
    override fun onCreateCommand(params: Map<String, String>): ScriptCommand? {
        val actionType = params[PARAM_ACTION_TYPE]?.trim().orEmpty()
        cmd = when (actionType) {
            TYPE_BACK -> CmdActionBack.createCommand()
            TYPE_HOME -> CmdActionHome.createCommand()
            TYPE_RECENT -> CmdActionRecent.createCommand()
            TYPE_NOTIFICATIONS -> CmdActionOpenNotifications.createCommand()
            TYPE_SCREEN_LOCK -> CmdActionScreenLock.createCommand()
            TYPE_SCREENSHOT -> CmdActionScreenShot.createCommand()
            else -> CmdActionBack.createCommand()
        }
        return cmd
    }

    companion object {
        private const val ACTION_NAME = "actionSystem"
        private const val PARAM_ACTION_TYPE = "actionType"

        private const val TYPE_BACK = "back"
        private const val TYPE_HOME = "home"
        private const val TYPE_RECENT = "recent"
        private const val TYPE_NOTIFICATIONS = "notifications"
        private const val TYPE_SCREEN_LOCK = "screenLock"
        private const val TYPE_SCREENSHOT = "screenshot"

        private val VALID_TYPES = setOf(TYPE_BACK, TYPE_HOME, TYPE_RECENT, TYPE_NOTIFICATIONS, TYPE_SCREEN_LOCK, TYPE_SCREENSHOT)
    }
}
