// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.mcp.tools

import com.hive.plugin.mcp.model.ActionResult
import com.hive.plugin.mcp.model.McpAction
import com.hive.plugin.mcp.model.McpActionParameters
import com.hive.script.base.ScriptCommand
import com.hive.script.base.ScriptConst
import com.hive.script.cmd.CmdDownload
import com.hive.script.extensions.executeSync
import com.hive.script.mcp.CheckActionResult
import com.hive.script.mcp.McpToolBuilder
import com.hive.utils.GlobalApp

@AutoMcpToolsRegister(MCP_IDS.ToolDownload)
class ScriptToolBuilder_CmdDownload : McpToolBuilder() {

    private var cmd = CmdDownload.createCommand()

    override fun matchAction(actionName: String): Boolean {
        return "download" == actionName
    }

    override fun getAction(): McpAction = McpAction(
        action = "download",
        extraName = GlobalApp.getString(com.hive.i8n.R.string.tool_download_name),
        description = GlobalApp.getString(com.hive.i8n.R.string.tool_download_description),
        paramInfo = mutableListOf(
            McpActionParameters(
                name = "url",
                type = "string",
                description = GlobalApp.getString(com.hive.i8n.R.string.tool_download_url_desc),
                required = true,
                examples = listOf("https://example.com/file.pdf"),
            ),
            McpActionParameters(
                name = "path",
                type = "string",
                description = GlobalApp.getString(com.hive.i8n.R.string.tool_download_path_desc),
                required = false,
                examples = listOf(ScriptConst.getDownloadPath()),
            ),
        ),
        paramValues = emptyMap(),
    )

    override fun onCheckAction(action: McpAction): CheckActionResult {
        val url = action.paramValues["url"]
        if (url.isNullOrBlank()) {
            return CheckActionResult(
                false,
                GlobalApp.getString(com.hive.i8n.R.string.tool_download_error_url_empty)
            )
        }
        return CheckActionResult(true, null)
    }

    override fun getCommand(): ScriptCommand = cmd

    override fun withScreenLayout() = false

    override fun supportDelay() = false

    override fun onCreateCommand(params: Map<String, String>): ScriptCommand? {
        val url = params["url"] ?: ""
        val path = params["path"]?.takeIf { it.isNotBlank() } ?: ScriptConst.getDownloadPath()

        cmd = CmdDownload.createCommand()
        cmd.url = url
        cmd.savePath = path

        return cmd
    }
}
