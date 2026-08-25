// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.mcp.tools

import android.text.TextUtils
import com.hive.permissions.PermissionsUtils
import com.hive.plugin.mcp.model.ActionResult
import com.hive.plugin.mcp.model.McpAction
import com.hive.plugin.mcp.model.McpActionParameters
import com.hive.script.R
import com.hive.script.base.ScriptCommand
import com.hive.script.cmd.CmdRequestPermission
import com.hive.script.extensions.executeSync
import com.hive.script.mcp.CheckActionResult
import com.hive.script.mcp.McpToolBuilder
import com.hive.utils.GlobalApp

@AutoMcpToolsRegister(MCP_IDS.ToolRequestPermission)
class ScriptToolBuilder_CmdRequestPermission : McpToolBuilder() {

    private var cmd = CmdRequestPermission.createCommand("")

    override fun matchAction(actionName: String): Boolean {
        return "requestPermission" == actionName
    }

    override fun getAction(): McpAction = McpAction(
        action = "requestPermission",
        extraName = GlobalApp.getString(com.hive.i8n.R.string.tool_permission_name),
        description = GlobalApp.getString(com.hive.i8n.R.string.tool_permission_description),
        paramInfo = mutableListOf(
            McpActionParameters(
                name = "permission",
                type = "string",
                description = GlobalApp.getString(com.hive.i8n.R.string.tool_permission_desc),
                required = true,
                examples = listOf("android.permission.CAPTURE","android.permission.CAMERA"),
            )
        ),
        paramValues = emptyMap(),
    )

    override fun onCheckAction(action: McpAction): CheckActionResult {
        val permission = action.paramValues["permission"]

        if (permission.isNullOrBlank()) {
            return CheckActionResult(false, GlobalApp.getString(com.hive.i8n.R.string.tool_permission_error_empty))
        }
        if (TextUtils.isEmpty(PermissionsUtils.getPermissionsName(permission))) {
            return CheckActionResult(
                false,
                GlobalApp.getString(com.hive.i8n.R.string.tool_permission_error_invalid, PermissionsUtils.getAllPermissions().map { it.key }.joinToString { it })
            )
        }

        return CheckActionResult(true, null)
    }


    override fun getCommand(): ScriptCommand {
        return cmd
    }

    override fun onCreateCommand(params: Map<String, String>): ScriptCommand? {
        val permission = params["permission"] ?: ""
        cmd = CmdRequestPermission.createCommand(permission)
        return cmd
    }
} 