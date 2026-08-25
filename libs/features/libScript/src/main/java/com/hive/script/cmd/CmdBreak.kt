// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.cmd

import com.hive.script.R
import com.hive.script.base.ScriptCommand
import com.hive.script.base.core.ScriptRegularInterface
import com.hive.script.exception.ScriptBreakException
import com.hive.utils.GlobalApp

/**
 *
 * @author jiadou
 * @date 6/8/21
 */
@AutoCmdRegister(type = IDS.CmdBreak, name = "break")
class CmdBreak : ScriptCommand(), ScriptRegularInterface {

    override fun onExecute() : CmdExecuteResult {
        return CmdExecuteResult.success()
    }

    override fun doExecute(): CmdExecuteResult {
        super.doExecute()
        throw ScriptBreakException("break")
        return CmdExecuteResult.success()
    }

    override fun isSupportDelay() = false

    private fun getCommandDuration() = 0L

    override fun getCommand() = cmdPrefix()

    override fun getCommandName() = GlobalApp.getString(com.hive.i8n.R.string.cmd_name_break)

    override fun getCommandDescribe() = GlobalApp.getString(com.hive.i8n.R.string.cmd_des_break)

    override fun getCommandIcon() = R.drawable.sc_icon_circly_break

    override fun parseCmd(cmd: String) {
    }

    override fun getPermissionRequest() = null

    companion object {
        fun createCommand() = CmdBreak()
    }
}