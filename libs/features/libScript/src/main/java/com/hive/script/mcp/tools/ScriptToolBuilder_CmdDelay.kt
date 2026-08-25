// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.mcp.tools

import com.hive.plugin.mcp.model.ActionResult
import com.hive.plugin.mcp.model.McpAction
import com.hive.plugin.mcp.model.McpActionParameters
import com.hive.script.R
import com.hive.script.base.ScriptCommand
import com.hive.script.cmd.CmdDelay
import com.hive.script.extensions.executeSync
import com.hive.script.mcp.CheckActionResult
import com.hive.script.mcp.McpToolBuilder
import com.hive.script.mcp.toLongCompat
import com.hive.script.mcp.toLongOrNullCompat
import com.hive.utils.GlobalApp

@AutoMcpToolsRegister(MCP_IDS.ToolDelay)
class ScriptToolBuilder_CmdDelay : McpToolBuilder() {

    private var cmd = CmdDelay.createCommand(1000L)

    override fun matchAction(actionName: String): Boolean {
        return "delay" == actionName
    }

    override fun getAction(): McpAction = McpAction(
        action = "delay",
        extraName = GlobalApp.getString(com.hive.i8n.R.string.tool_delay_name),
        description = GlobalApp.getString(com.hive.i8n.R.string.tool_delay_description),
        paramInfo = mutableListOf(
            McpActionParameters(
                name = "duration",
                type = "number",
                description = GlobalApp.getString(com.hive.i8n.R.string.tool_delay_duration_desc),
                required = true,
                examples = listOf("1000", "2000"),
                format = "number",
            ),
            McpActionParameters(
                name = "endDuration",
                type = "number",
                description = GlobalApp.getString(com.hive.i8n.R.string.tool_delay_end_duration_desc),
                required = false,
                examples = listOf("1500", "3000"),
                format = "number",
            )
        ),
        paramValues = emptyMap(),
    )

    override fun onCheckAction(action: McpAction): CheckActionResult {
        val duration = action.paramValues["duration"]?.toLongOrNullCompat()
        val endDuration = action.paramValues["endDuration"]?.toLongOrNullCompat()

        if (duration == null) {
            return CheckActionResult(false, GlobalApp.getString(com.hive.i8n.R.string.tool_delay_error_duration_empty))
        }
        if (duration < 0) {
            return CheckActionResult(false, GlobalApp.getString(com.hive.i8n.R.string.tool_delay_error_duration_negative))
        }
        if (endDuration != null && endDuration < duration) {
            return CheckActionResult(false, GlobalApp.getString(com.hive.i8n.R.string.tool_delay_error_end_duration_less))
        }

        return CheckActionResult(true, null)
    }

    override fun getCommand(): ScriptCommand {
        return cmd
    }

    override fun supportDelay() = false

    override fun onCreateCommand(params: Map<String, String>): ScriptCommand? {
        val duration = params["duration"].toLongCompat(1000L)
        val endDuration = params["endDuration"]?.toLongOrNullCompat()

        cmd = if (endDuration != null) {
            CmdDelay.createCommand(duration, endDuration)
        } else {
            CmdDelay.createCommand(duration)
        }

        return cmd
    }
} 