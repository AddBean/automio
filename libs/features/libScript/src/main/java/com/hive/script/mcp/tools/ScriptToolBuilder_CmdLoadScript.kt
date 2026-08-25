// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.mcp.tools

import com.hive.plugin.mcp.McpConst
import com.hive.plugin.mcp.model.ActionResult
import com.hive.plugin.mcp.model.McpAction
import com.hive.plugin.mcp.model.McpActionParameters
import com.hive.script.base.ScriptCommand
import com.hive.script.base.ScriptCommandRoot
import com.hive.script.base.params.ScriptParamEnv
import com.hive.script.cmd.CmdCallScript
import com.hive.script.cmd.CmdScriptStart
import com.hive.script.extensions.executeSync
import com.hive.script.mcp.CheckActionResult
import com.hive.script.mcp.McpToolBuilder
import com.hive.script.views.dialog.DialogCmdDialogInput.InputItem
import com.hive.utils.GlobalApp
import java.io.File

class ScriptToolBuilder_CmdLoadScript(
    val scriptId: String,
    val scriptName: String,
    val scriptDesc: String,
    val scriptPath: String
) :
    McpToolBuilder() {

    var cmd: CmdCallScript? = null

    override fun getAction(): McpAction = McpAction(
        action = scriptId,
        extraName = scriptName,
        extraType = McpConst.Tool_Type_Custom,
        description = scriptDesc,
        paramInfo = findAllStartParams()?.map {
            McpActionParameters(
                name = it.label,
                type = "string",
                description = it.hint,
                required = it.required,
                examples = listOf(it.value),
                localParam = ScriptParamEnv.parseParamsId(it.id ?: "default_id"),
            )
        }?.toMutableList() ?: mutableListOf(),
        paramValues = emptyMap(),
    )


    override fun onCheckAction(action: McpAction): CheckActionResult {
        if (!File(scriptPath).exists()) {
            return CheckActionResult(
                false,
                GlobalApp.getString(com.hive.i8n.R.string.tool_load_script_error_not_exist)
            )
        }
        return CheckActionResult(true, null)
    }

    override fun supportDelay() = false

    override fun getCommand(): ScriptCommand? {
        return cmd
    }

    override fun onCreateCommand(params: Map<String, String>): ScriptCommand? {
        cmd = CmdCallScript.createCommand(scriptPath, scriptName, params)
        return cmd
    }

    private fun findAllStartParams(): List<InputItem>? {
        val sPath = scriptPath
        val commandRoot = ScriptCommandRoot().apply {
            ScriptCommandRoot.loadScriptSync(
                sPath,
                this
            )
        }
        val start = commandRoot.commandQueue.firstOrNull { it is CmdScriptStart } as? CmdScriptStart
            ?: return null
        return CmdScriptStart.parseInputItems(start)
    }


}