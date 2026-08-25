// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.mcp.tools

import com.hive.plugin.mcp.model.ActionResult
import com.hive.plugin.mcp.model.McpAction
import com.hive.plugin.mcp.model.McpActionParameters
import com.hive.script.R
import com.hive.script.base.ScriptCommand
import com.hive.script.cmd.CmdCaptureScreen
import com.hive.script.extensions.executeSync
import com.hive.script.mcp.CheckActionResult
import com.hive.script.mcp.McpToolBuilder
import com.hive.script.utils.ScriptPermissionManager
import com.hive.utils.GlobalApp

@AutoMcpToolsRegister(MCP_IDS.ToolCaptureScreen)
class ScriptToolBuilder_CmdCaptureScreen : McpToolBuilder() {

    private val cmd = CmdCaptureScreen.createCommand("main.param0")

    override fun matchAction(actionName: String): Boolean {
        return "captureScreen" == actionName
    }

    override fun getAction(): McpAction = McpAction(
        action = "captureScreen",
        extraName = GlobalApp.getString(com.hive.i8n.R.string.tool_capture_screen_name),
        description = GlobalApp.getString(com.hive.i8n.R.string.tool_capture_screen_description),
        paramInfo = mutableListOf(
            McpActionParameters(
                name = "saveToGallery",
                type = "boolean",
                description = GlobalApp.getString(com.hive.i8n.R.string.tool_capture_save_to_gallery_desc),
                required = false,
                examples = listOf("true", "false"),
                format = "boolean",
            )
        ),
        paramValues = mapOf("saveToGallery" to "false"),
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
                com.hive.utils.GlobalApp.getString(
                    com.hive.i8n.R.string.script_tool_missing_permissions,
                    missedPermissions.map { it.second }.joinToString(", ")
                ),
                null
            )
        }
        return super.onCheckPermission(action)
    }

    override fun withScreenLayout(): Boolean = false

    override fun onCheckAction(action: McpAction): CheckActionResult {
        // 参数是可选的，有默认值
        return CheckActionResult(true, null)
    }


    override fun getCommand(): ScriptCommand {
        return cmd
    }

    override fun onCreateCommand(params: Map<String, String>): ScriptCommand? {
        cmd.saveToGallery = params["saveToGallery"]?.lowercase() in listOf("true", "1", "yes")
        return cmd
    }
}