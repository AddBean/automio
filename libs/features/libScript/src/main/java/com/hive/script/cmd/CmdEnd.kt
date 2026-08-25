// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.cmd

import com.hive.script.R
import com.hive.script.base.ScriptCommand
import com.hive.script.base.core.ScriptRegularInterface
import com.hive.utils.GlobalApp

/**
 *
 * @author jiadou
 * @date 6/8/21
 */
@AutoCmdRegister(type = IDS.CmdEnd, name = "end")
class CmdEnd : ScriptCommand(), ScriptRegularInterface {

    override fun onExecute() : CmdExecuteResult {
        // 结束脚本执行
        return CmdExecuteResult.success()
    }

    override fun onExecuteJump(cmd: ScriptCommand?) {
    }

    override fun isSupportDelay() = false

    private fun getCommandDuration() = 0L

    override fun getCommand() = cmdPrefix()

    override fun getCommandName() = GlobalApp.getString(com.hive.i8n.R.string.cmd_name_end)

    override fun getCommandDescribe() = GlobalApp.getString(com.hive.i8n.R.string.cmd_des_end)

    override fun getCommandIcon() = R.drawable.sc_ic_end

    override fun isGroupCommand() = false

    override fun parseCmd(cmd: String) {
    }

    override fun getPermissionRequest() = null
}