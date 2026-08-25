// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.mcp.tools

import com.hive.plugin.mcp.model.ActionResult
import com.hive.plugin.mcp.model.McpAction
import com.hive.plugin.mcp.model.McpActionParameters
import com.hive.script.base.ScriptCommand
import com.hive.script.cmd.CmdOpenApp
import com.hive.script.cmd.CmdOpenUrl
import com.hive.script.extensions.executeSync
import com.hive.script.mcp.CheckActionResult
import com.hive.script.mcp.McpToolBuilder
import com.hive.utils.GlobalApp

@AutoMcpToolsRegister(MCP_IDS.ToolOpenApp)
class ScriptToolBuilder_CmdOpen : McpToolBuilder() {

    private var cmd: ScriptCommand = CmdOpenUrl.createCommand("")

    override fun getAction(): McpAction = McpAction(
        action = ACTION_OPEN,
        extraName = GlobalApp.getString(com.hive.i8n.R.string.tool_open_name),
        description = GlobalApp.getString(com.hive.i8n.R.string.tool_open_description),
        paramInfo = mutableListOf(
            McpActionParameters(
                name = PARAM_ACTION,
                type = "string",
                description = GlobalApp.getString(com.hive.i8n.R.string.tool_open_action_desc),
                required = true,
                examples = listOf(ACTION_OPEN_APP, ACTION_OPEN_SCHEME),
            ),
            McpActionParameters(
                name = PARAM_URI,
                type = "string",
                description = GlobalApp.getString(com.hive.i8n.R.string.tool_open_uri_desc),
                required = true,
                examples = listOf("com.tencent.mm", "com.tencent.mm:com.tencent.mm.ui.LauncherUI", "https://www.baidu.com"),
            ),
            McpActionParameters(
                name = PARAM_REOPEN,
                type = "boolean",
                description = GlobalApp.getString(com.hive.i8n.R.string.tool_open_reopen_desc),
                required = false,
                examples = listOf("true", "false"),
            ),
        ),
        paramValues = emptyMap(),
    )

    override fun onCheckAction(action: McpAction): CheckActionResult {
        val actionType = action.paramValues[PARAM_ACTION]?.trim()?.lowercase() ?: ""
        if (actionType != ACTION_OPEN_APP && actionType != ACTION_OPEN_SCHEME) {
            return CheckActionResult(false, GlobalApp.getString(com.hive.i8n.R.string.tool_open_error_action_invalid))
        }
        val uri = action.paramValues[PARAM_URI]?.trim()
        if (uri.isNullOrBlank()) {
            return CheckActionResult(false, GlobalApp.getString(com.hive.i8n.R.string.tool_open_error_uri_empty))
        }
        return CheckActionResult(true, null)
    }

    override fun getCommand(): ScriptCommand? = cmd

    /**
     * 创建命令对象（只创建，不执行）
     */
    override fun onCreateCommand(params: Map<String, String>): ScriptCommand? {
        val actionType = params[PARAM_ACTION]?.trim()?.lowercase() ?: ""
        val uri = params[PARAM_URI]?.trim().orEmpty()
        val reopen = params[PARAM_REOPEN]?.trim()?.lowercase() != "false"

        cmd = when (actionType) {
            ACTION_OPEN_APP -> {
                val (pkg, className) = parseAppUri(uri)
                val openAction = if (reopen) "reopen" else "open"
                CmdOpenApp.createCommand(pkg, className, pkg, openAction)
            }
            ACTION_OPEN_SCHEME -> CmdOpenUrl.createCommand(uri)
            else -> CmdOpenUrl.createCommand("")
        }
        return cmd
    }

    /** Parse uri for openApp: "pkg" or "pkg:class" -> (packageName, className) */
    private fun parseAppUri(uri: String): Pair<String, String> {
        val colon = uri.indexOf(':')
        return if (colon < 0) {
            uri to "-"
        } else {
            uri.substring(0, colon).trim() to uri.substring(colon + 1).trim().ifEmpty { "-" }
        }
    }

    companion object {
        private const val ACTION_OPEN = "open"
        private const val PARAM_ACTION = "action"
        private const val PARAM_URI = "uri"
        private const val PARAM_REOPEN = "reopen"
        private const val ACTION_OPEN_APP = "openapp"
        private const val ACTION_OPEN_SCHEME = "openscheme"
    }
}
