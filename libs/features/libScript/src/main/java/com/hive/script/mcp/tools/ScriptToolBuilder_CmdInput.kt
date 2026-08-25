// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.mcp.tools

import com.hive.plugin.mcp.model.ActionResult
import com.hive.plugin.mcp.model.McpAction
import com.hive.plugin.mcp.model.McpActionParameters
import com.hive.script.R
import com.hive.script.base.ScriptCommand
import com.hive.script.base.ScriptConst
import com.hive.script.cmd.CmdInput
import com.hive.script.extensions.executeSync
import com.hive.script.mcp.CheckActionResult
import com.hive.script.mcp.McpToolBuilder
import com.hive.script.mcp.toIntOrNullCompat
import com.hive.utils.GlobalApp

@AutoMcpToolsRegister(MCP_IDS.ToolInput)
class ScriptToolBuilder_CmdInput : McpToolBuilder() {

    private var cmd = CmdInput.createCommand("", "", "")

    override fun matchAction(actionName: String): Boolean {
        return "input" == actionName
    }

    override fun getAction(): McpAction = McpAction(
        action = "input",
        extraName = GlobalApp.getString(com.hive.i8n.R.string.tool_input_name),
        description = GlobalApp.getString(com.hive.i8n.R.string.tool_input_description),
        paramInfo = mutableListOf(
            McpActionParameters(
                name = "content",
                type = "string",
                description = GlobalApp.getString(com.hive.i8n.R.string.tool_input_text_desc),
                required = true,
                examples = listOf("Hello World"),
            ),
//            McpActionParameters(
//                name = "targetIndex",
//                type = "number",
//                description = GlobalApp.getString(com.hive.i8n.R.string.tool_input_target_index_desc),
//                required = true,
//                examples = listOf("1", "2"),
//            ),
            McpActionParameters(
                name = "action",
                type = "string",
                description = GlobalApp.getString(com.hive.i8n.R.string.tool_input_mode_desc),
                required = true,
                examples = listOf("full", "append"),
            ),
//            McpActionParameters(
//                name = "targetId",
//                type = "string",
//                description = GlobalApp.getString(com.hive.i8n.R.string.tool_input_view_id_desc),
//                required = false,
//                examples = listOf("edit_text", "-"),
//            ),
            McpActionParameters(
                name = "x",
                type = "number",
                description = GlobalApp.getString(com.hive.i8n.R.string.tool_input_x_desc),
                required = true,
                examples = listOf("0.5", "0.3"),
            ),
            McpActionParameters(
                name = "y",
                type = "number",
                description = GlobalApp.getString(com.hive.i8n.R.string.tool_input_y_desc),
                required = true,
                examples = listOf("0.5", "0.7"),
            )
        ),
        paramValues = emptyMap(),
    )

    override fun onCheckAction(action: McpAction): CheckActionResult {
        val content = action.paramValues["content"]

        if (content.isNullOrBlank()) {
            return CheckActionResult(
                false,
                GlobalApp.getString(com.hive.i8n.R.string.tool_input_error_content_empty)
            )
        }

        val actionType = action.paramValues["action"] ?: "full"
        if (actionType !in listOf("full", "append")) {
            return CheckActionResult(
                false,
                GlobalApp.getString(com.hive.i8n.R.string.tool_input_error_action_invalid)
            )
        }

//        val targetIndex = action.paramValues["targetIndex"]?.takeIf { it.isNotBlank() }
//        if (targetIndex != null && targetIndex.toIntOrNullCompat()?.let { it > 0 } != true) {
//            return CheckActionResult(
//                false,
//                GlobalApp.getString(com.hive.i8n.R.string.tool_input_error_target_index_invalid)
//            )
//        }

        return CheckActionResult(true, null)
    }

    override fun getCommand(): ScriptCommand? {
        return cmd
    }

    /**
     * 创建命令对象（只创建，不执行）
     */
    override fun onCreateCommand(params: Map<String, String>): ScriptCommand? {
        val content = params["content"] ?: ""
        val targetId = params["targetId"] ?: ScriptConst.NONE_CHAR
        val action = params["action"] ?: "full"
        val targetIndex = params["targetIndex"]?.toIntOrNull()?.takeIf { it > 0 } ?: 1
        val x = params["x"]?.toFloatOrNull()
        val y = params["y"]?.toFloatOrNull()
        cmd = CmdInput.createCommand(content, targetId, action, targetIndex = targetIndex, targetX = x, targetY = y)
        return cmd
    }
} 
