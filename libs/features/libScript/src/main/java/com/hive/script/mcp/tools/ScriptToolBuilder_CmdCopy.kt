// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.mcp.tools

import com.hive.plugin.mcp.model.ActionResult
import com.hive.plugin.mcp.model.McpAction
import com.hive.plugin.mcp.model.McpActionParameters
import com.hive.script.base.ScriptCommand
import com.hive.script.cmd.CmdCopyToClipboard
import com.hive.script.extensions.executeSync
import com.hive.script.mcp.CheckActionResult
import com.hive.script.mcp.McpToolBuilder
import com.hive.utils.GlobalApp

@AutoMcpToolsRegister(MCP_IDS.ToolCopy)
class ScriptToolBuilder_CmdCopy : McpToolBuilder() {

    private var cmd = CmdCopyToClipboard.createCommand("")

    override fun matchAction(actionName: String): Boolean {
        return "copy" == actionName
    }

    override fun getAction(): McpAction = McpAction(
        action = "copy",
        extraName = GlobalApp.getString(com.hive.i8n.R.string.tool_copy_name),
        description = GlobalApp.getString(com.hive.i8n.R.string.tool_copy_description),
        paramInfo = mutableListOf(
            McpActionParameters(
                name = "content",
                type = "string",
                description = GlobalApp.getString(com.hive.i8n.R.string.tool_copy_content_desc),
                required = true,
                examples = listOf("Hello World", GlobalApp.getString(com.hive.i8n.R.string.tool_copy_example_text)),
            )
        ),
        paramValues = emptyMap(),
    )

    override fun onCheckAction(action: McpAction): CheckActionResult {
        val content = action.paramValues["content"]

        if (content.isNullOrBlank()) {
            return CheckActionResult(false, GlobalApp.getString(com.hive.i8n.R.string.tool_copy_error_content_empty))
        }

        return CheckActionResult(true, null)
    }

    override fun getCommand(): ScriptCommand {
        return cmd
    }

    override fun withScreenLayout() = false

    override fun onCreateCommand(params: Map<String, String>): ScriptCommand? {
        val content = params["content"] ?: ""
        cmd = CmdCopyToClipboard.createCommand(content)
        return cmd
    }
} 