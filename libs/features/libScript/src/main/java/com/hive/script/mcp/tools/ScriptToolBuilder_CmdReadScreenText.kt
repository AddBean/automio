// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.mcp.tools

import com.hive.plugin.mcp.model.ActionResult
import com.hive.plugin.mcp.model.McpAction
import com.hive.plugin.ocr.OcrResult
import com.hive.script.R
import com.hive.script.base.ScriptCommand
import com.hive.script.cmd.CmdReadScreenText
import com.hive.script.extensions.executeSync
import com.hive.script.mcp.CheckActionResult
import com.hive.script.mcp.McpToolBuilder
import com.hive.script.utils.ScriptPermissionManager
import com.hive.script.utils.ScreenPageInfoFormatter
import com.hive.utils.GlobalApp

@AutoMcpToolsRegister(MCP_IDS.ToolReadScreenText)
class ScriptToolBuilder_CmdReadScreenText : McpToolBuilder() {

    private val cmd = CmdReadScreenText.createCommand("main.param0")

    override fun matchAction(actionName: String): Boolean {
        return "readScreenText" == actionName
    }

    override fun getAction(): McpAction = McpAction(
        action = "readScreenText",
        extraName = GlobalApp.getString(com.hive.i8n.R.string.tool_read_screen_text_name),
        description = GlobalApp.getString(com.hive.i8n.R.string.tool_read_screen_text_description),
        paramInfo = mutableListOf(),
        paramValues = emptyMap(),
    )

    override fun onCheckPermission(action: McpAction): CheckActionResult {
        val permissions = cmd.getPermissionRequest()
        // 检查是否有权限
        val missedPermissions = ScriptPermissionManager.checkMissedPermissions(
            permissions
        )
        if (missedPermissions.isNotEmpty()) {
            return CheckActionResult(
                false,
                GlobalApp.getString(com.hive.i8n.R.string.tool_read_screen_text_error_permission, missedPermissions.map { it.second }.joinToString(", ")),
                null
            )
        }
        return super.onCheckPermission(action)
    }
    
    override fun onCheckAction(action: McpAction): CheckActionResult {
        // 参数是可选的，有默认值
        return CheckActionResult(true, null)
    }


    override fun getCommand(): ScriptCommand {
        return cmd
    }

    override fun onCreateCommand(params: Map<String, String>): ScriptCommand? {
        // 查询类工具，无需解析参数，直接返回命令
        return cmd
    }


}
