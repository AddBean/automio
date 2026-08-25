// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.cmd

import com.hive.script.R
import com.hive.script.base.ScriptCommand
import com.hive.script.base.ScriptCommandRoot
import com.hive.script.base.ScriptConst
import com.hive.script.base.core.ScripRunningEnv
import com.hive.script.base.core.ScriptRegularInterface
import com.hive.script.base.core.ScriptThreadManager
import com.hive.script.base.params.ScriptSystemParam
import com.hive.script.exception.ScriptJumpException
import com.hive.script.base.core.ScriptLineTokenizer
import com.hive.script.scope.PackageRuntimeResolver
import com.hive.utils.GlobalApp
import com.hive.utils.extends.decode
import com.hive.utils.extends.encode
import com.hive.utils.utils.StringUtils
import com.hive.views.widgets.CommonToast
import java.io.File

/**
 *
 * @author jiadou
 * @email 172111432@qq.com
 * @date 6/8/21
 */
@AutoCmdRegister(type = IDS.CmdCallScript, name = "callScript")
class CmdCallScript : ScriptCommand(), ScriptRegularInterface {

    var params: Map<String, String>? = null

    var scriptPath: String? = null

    var scriptName: String? = null

    var scriptCommandRoot: ScriptCommandRoot? = null

    override fun onExecute(): CmdExecuteResult {
        ScriptThreadManager.delay(getCommandDuration())
        val resolvedScriptPath = resolveScriptPath()
        if (resolvedScriptPath != null && File(resolvedScriptPath).exists()) {
            scriptCommandRoot = ScriptCommandRoot().apply {
                ScriptCommandRoot.loadScriptSync(
                    resolvedScriptPath,
                    this
                )
            }
            getRootScript()?.envRunning?.scriptInterpreter?.run {
                scriptCommandRoot?.envRunning = ScripRunningEnv(this)
            }
        } else {
            CommonToast.show(com.hive.i8n.R.string.cmd_load_script_error)
        }
        scriptCommandRoot?.commandQueue?.firstOrNull()?.let {cmd->
            if (cmd is CmdScriptStart) {
                params?.forEach {
                    val paramName = cmd.findFullParamIdByName(it.key)
                    paramName?.run {
                        scriptCommandRoot?.getParamEnv()?.writeParam(paramName, it.value)
                    }
                }
            }
        }

        CmdScriptStart.ignoreExecuteOnce()
        val root = scriptCommandRoot ?: return CmdExecuteResult.failure()
        executeScriptCommand(root)
        CmdScriptStart.cleanIgnoreFlag()
        return CmdExecuteResult.success(data = getOutputInfo())
    }

    private fun resolveScriptPath(): String? {
        val raw = scriptPath ?: return null
        val resolved = PackageRuntimeResolver.resolveCallScriptPath(
            currentScriptDir = File(getScriptBasePath()),
            ref = raw
        )
        return resolved ?: raw
    }

    private fun executeScriptCommand(scriptCommand: ScriptCommand) {
        try {
            scriptCommand.doExecute()
        } catch (jumpExp: ScriptJumpException) {
            scriptCommandRoot?.envRunning?.getJumpControl()?.jumpTo(jumpExp.cmd)
            executeScriptCommand(scriptCommand)
        }
    }

    private fun getCommandDuration() = ScriptConst.Cmd_Default_Bias

    override fun getCommandName() = GlobalApp.getString(com.hive.i8n.R.string.cmd_name_load_script)

    override fun getCommandDescribe() = GlobalApp.getString(
        com.hive.i8n.R.string.cmd_name_load_script_des,
        scriptName
    )

    override fun getCommandIcon() = R.drawable.sc_cmd_import_script

    override fun getCommand() = "${cmdPrefix()} path=\"${scriptPath?.encode()}\" name=\"${scriptName?.encode()}\""

    private fun getOutputInfo(): String {
        val output1 = scriptCommandRoot?.getParamEnv()?.readParam(ScriptSystemParam.OUTPUT1.paramId)
        val output2 = scriptCommandRoot?.getParamEnv()?.readParam(ScriptSystemParam.OUTPUT2.paramId)
        val output3 = scriptCommandRoot?.getParamEnv()?.readParam(ScriptSystemParam.OUTPUT3.paramId)
        return listOfNotNull(output1, output2, output3).joinToString("\n")
    }


    override fun parseCmd(cmd: String) {
        val kv = ScriptLineTokenizer.parseKeyValueParams(cmd)
        scriptPath = kv["path"]?.decode()?.takeIf { it.isNotEmpty() }
        scriptName = kv["name"]?.decode()?.takeIf { it.isNotEmpty() }
    }

    companion object {
        fun createCommand(
            scriptPath: String,
            scriptName: String,
            params: Map<String, String>? = null
        ) =
            CmdCallScript().apply {
                this.scriptName = scriptName
                this.scriptPath = scriptPath
                this.params = params
            }
    }
}
