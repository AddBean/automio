// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.mcp.tools

import com.hive.plugin.mcp.model.ActionResult
import com.hive.plugin.mcp.model.McpAction
import com.hive.script.base.ScriptCommand
import com.hive.script.cmd.CmdListInstalledApps
import com.hive.script.extensions.executeSync
import com.hive.script.mcp.CheckActionResult
import com.hive.script.mcp.McpToolBuilder
import com.hive.utils.GlobalApp
import com.hive.utils.extends.jsonList2Yaml

@AutoMcpToolsRegister(MCP_IDS.ToolGetInstalledAppList)
class ScriptToolBuilder_CmdGetInstalledAppList : McpToolBuilder() {

    private var cmd = CmdListInstalledApps.createCommand()

    override fun matchAction(actionName: String): Boolean {
        return "getInstalledAppList" == actionName
    }

    override fun getAction(): McpAction = McpAction(
        action = "getInstalledAppList",
        extraName = GlobalApp.getString(com.hive.i8n.R.string.tool_get_installed_app_list_name),
        description = GlobalApp.getString(com.hive.i8n.R.string.tool_get_installed_app_list_description),
        paramInfo = mutableListOf(
        ),
        paramValues = emptyMap(),
    )

    override fun onCheckAction(action: McpAction): CheckActionResult {

        return CheckActionResult(true, null)
    }

    override fun withScreenLayout() = false

    override fun getCommand(): ScriptCommand {
        return cmd
    }

    override fun onCreateCommand(params: Map<String, String>): ScriptCommand? {
        cmd = CmdListInstalledApps.createCommand()
        return cmd
    }
} 