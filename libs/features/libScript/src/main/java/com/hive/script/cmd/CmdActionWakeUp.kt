// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.cmd

import com.hive.script.R
import com.hive.script.base.ScriptCommand
import com.hive.script.base.ScriptConst
import com.hive.script.base.core.ScriptRegularInterface
import com.hive.script.base.core.ScriptThreadManager
import com.hive.script.driver.ScriptEventHelper
import com.hive.utils.GlobalApp


@AutoCmdRegister(type = IDS.CmdWakeUp, name = "actionWakeUp")
class CmdActionWakeUp : ScriptCommand(), ScriptRegularInterface {

    override fun onExecute() : CmdExecuteResult {
        ScriptEventHelper.get().performActionWakeUpScreen{
            ScriptThreadManager.delay(ScriptConst.Cmd_Wakeup_Default)
        }
        return CmdExecuteResult.success()
    }

    override fun getCommand() = cmdPrefix()

    override fun getCommandName() = GlobalApp.getString(com.hive.i8n.R.string.cmd_name_action_wakeup)

    override fun getCommandDescribe() = GlobalApp.getString(com.hive.i8n.R.string.cmd_name_action_wakeup)

    override fun getCommandIcon() = R.drawable.sc_ic_wakeup

    override fun parseCmd(cmd: String) {
    }

    override fun getPermissionRequest() = null

    companion object {
        fun createCommand() = CmdActionWakeUp()
    }
}