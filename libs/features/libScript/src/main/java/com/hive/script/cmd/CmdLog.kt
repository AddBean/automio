// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.cmd

import com.hive.script.R
import com.hive.script.base.ScriptCommand
import com.hive.script.base.ScriptConst
import com.hive.script.base.core.ScriptInterpreterObserver
import com.hive.script.base.core.ScriptLineTokenizer
import com.hive.script.base.core.ScriptRegularInterface
import com.hive.script.base.core.ScriptThreadManager
import com.hive.script.views.logger.ScriptLoggerView
import com.hive.utils.GlobalApp
import com.hive.utils.extends.decode
import com.hive.utils.extends.encode

@AutoCmdRegister(type = IDS.CmdLog, name = "log")
class CmdLog : ScriptCommand(), ScriptRegularInterface {
    var content: String? = null

    override fun onExecute(): CmdExecuteResult {
        ScriptInterpreterObserver.notifyLogger(
            this,
            ScriptLoggerView.LogType.ERROR,
            parseParamText(content)
        )
        ScriptThreadManager.delay(getCommandDuration())
        return CmdExecuteResult.success()
    }

    private fun getCommandDuration() = ScriptConst.Cmd_Default_Bias

    override fun getCommandName() = GlobalApp.getString(com.hive.i8n.R.string.cmd_name_printf)

    override fun getCommandDescribe() = GlobalApp.getString(com.hive.i8n.R.string.cmd_des_printf)

    override fun getCommandIcon() = R.drawable.sc_ic_log

    override fun getCommand() = "${cmdPrefix()} content=\"${content?.encode()}\""

    override fun parseCmd(cmd: String) {
        content = ScriptLineTokenizer.parseKeyValueParams(cmd)["content"]?.decode()
    }

    override fun getPermissionRequest() = null
}