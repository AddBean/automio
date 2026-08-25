// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.mcp.tools

import com.hive.plugin.mcp.model.ActionResult
import com.hive.plugin.mcp.model.McpAction
import com.hive.plugin.mcp.model.McpActionParameters
import com.hive.script.R
import com.hive.script.base.ScriptCommand
import com.hive.script.cmd.CmdPythonExecutor
import com.hive.script.extensions.executeSync
import com.hive.script.mcp.CheckActionResult
import com.hive.script.mcp.McpToolBuilder
import com.hive.utils.GlobalApp

@AutoMcpToolsRegister(MCP_IDS.ToolPythonExecutor)
class ScriptToolBuilder_PythonExecutor : McpToolBuilder() {

    private var cmd: CmdPythonExecutor = CmdPythonExecutor.createCommand()

    override fun matchAction(actionName: String): Boolean {
        return "pythonExecutor" == actionName
    }

    override fun getAction(): McpAction = McpAction(
        action = "pythonExecutor",
        extraName = GlobalApp.getString(com.hive.i8n.R.string.tool_python_executor_name),
        description = GlobalApp.getString(com.hive.i8n.R.string.tool_python_executor_description),
        paramInfo = mutableListOf(
            McpActionParameters(
                name = PARAM_ACTION,
                type = "string",
                description = GlobalApp.getString(com.hive.i8n.R.string.tool_python_executor_action_desc),
                required = true,
                examples = listOf(ACTION_HELP, ACTION_RUN),
            ),
            McpActionParameters(
                name = "code",
                type = "string",
                description = GlobalApp.getString(com.hive.i8n.R.string.tool_python_executor_code_desc),
                required = false,
                examples = listOf(
                    "print(1+1)",
                    "import json\nprint(json.dumps({\"a\":1}, ensure_ascii=False))",
                    "import urllib.request\nprint(urllib.request.urlopen(\"https://httpbin.org/get\").read().decode())",
                ),
            ),
        ),
        paramValues = emptyMap(),
    )

    override fun supportDelay(): Boolean = false

    override fun withScreenLayout(): Boolean = false

    override fun getCommand(): ScriptCommand? = cmd

    override fun onCheckAction(action: McpAction): CheckActionResult {
        val actionType = action.paramValues[PARAM_ACTION]?.trim()?.lowercase().orEmpty()
        return when (actionType) {
            ACTION_HELP -> CheckActionResult(true, null, null)
            ACTION_RUN -> {
                val code = action.paramValues["code"]?.trim()
                if (code.isNullOrEmpty()) {
                    CheckActionResult(
                        false,
                        GlobalApp.getString(com.hive.i8n.R.string.tool_python_executor_error_code_empty),
                        null,
                    )
                } else {
                    CheckActionResult(true, null, null)
                }
            }
            else -> CheckActionResult(
                false,
                GlobalApp.getString(com.hive.i8n.R.string.tool_python_executor_error_action_invalid),
                null,
            )
        }
    }

    override fun onCreateCommand(params: Map<String, String>): ScriptCommand? {
        val actionType = params[PARAM_ACTION]?.trim()?.lowercase().orEmpty()
        return when (actionType) {
            ACTION_HELP -> {
                // help 模式不创建命令，返回 null
                null
            }
            ACTION_RUN -> {
                val code = params["code"]?.trim().orEmpty()
                cmd = CmdPythonExecutor.createCodeCommand(code, outputParam = null)
                cmd
            }
            else -> null
        }
    }

    companion object {
        private const val PARAM_ACTION = "action"
        private const val ACTION_HELP = "help"
        private const val ACTION_RUN = "run"
        
        private val helpBody: String by lazy {
            runCatching {
                GlobalApp.getContext().resources.openRawResource(R.raw.python_executor_help)
                    .bufferedReader().use { it.readText() }
            }.getOrElse { "" }
        }
    }
}
