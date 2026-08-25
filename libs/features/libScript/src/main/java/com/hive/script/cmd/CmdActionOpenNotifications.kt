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

/**
 *
 * @author jiadou
 * @email 172111432@qq.com
 * @date 6/8/21
 */
@AutoCmdRegister(type = IDS.CmdActionOpenNotifications, name = "actionOpenNotifications")
class CmdActionOpenNotifications : ScriptCommand(), ScriptRegularInterface {

    override fun onExecute() : CmdExecuteResult {
        ScriptEventHelper.get().performActionNotifications()
        ScriptThreadManager.delay(ScriptConst.Cmd_Default_Bias)
        return CmdExecuteResult.success()
    }

    override fun getCommand() = cmdPrefix()

    override fun getCommandName() = GlobalApp.getString(com.hive.i8n.R.string.cmd_name_action_open_notifications)

    override fun getCommandDescribe() = GlobalApp.getString(com.hive.i8n.R.string.cmd_des_action_open_notifications)

    override fun getCommandIcon() = R.drawable.ic_notify

    override fun parseCmd(cmd: String) {
    }

    companion object {
        fun createCommand() = CmdActionOpenNotifications()
    }


}