// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.mcp.tools

import com.hive.plugin.mcp.model.ActionResult
import com.hive.plugin.mcp.model.McpAction
import com.hive.script.R
import com.hive.script.base.ScriptCommand
import com.hive.script.cmd.CmdReadScreenLayout
import com.hive.script.driver.ScriptEventHelper
import com.hive.script.extensions.executeSync
import com.hive.script.mcp.CheckActionResult
import com.hive.script.mcp.McpToolBuilder
import com.hive.script.ScriptScreenShotService
import com.hive.script.utils.ScreenPageInfoFormatter
import com.hive.script.utils.ScriptLayoutReader
import com.hive.utils.GlobalApp

@AutoMcpToolsRegister(MCP_IDS.ToolGetCurrentLayout)
class ScriptToolBuilder_CmdReadScreenLayout : McpToolBuilder() {

    private val cmd = CmdReadScreenLayout.createCommand("main.param0")

    override fun getAction(): McpAction =
        McpAction(
            action = "readScreenLayout",
            extraName = GlobalApp.getString(com.hive.i8n.R.string.tool_layout_name),
            description = GlobalApp.getString(com.hive.i8n.R.string.tool_layout_description),
            paramInfo = mutableListOf(),
            paramValues = emptyMap(),
        )

    override fun onCheckAction(action: McpAction): CheckActionResult {
        val scriptEventHelper = ScriptEventHelper.get()
        val serviceEntity = scriptEventHelper.serviceEntity ?: return CheckActionResult(false, GlobalApp.getString(com.hive.i8n.R.string.tool_layout_error_service))

        //        if (serviceEntity.rootInActiveWindow == null) {
//            return CheckActionResult(false, "Unable to get current page root node")
//        }

        return CheckActionResult(true, null)
    }

    override fun withScreenLayout(): Boolean = false

    override fun getCommand(): ScriptCommand {
        return cmd
    }

    override fun onCreateCommand(params: Map<String, String>): ScriptCommand? {
        // 查询类工具，无需解析参数，直接返回命令
        return cmd
    }

}
