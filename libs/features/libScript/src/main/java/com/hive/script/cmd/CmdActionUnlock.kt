// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.cmd

import com.hive.script.R
import com.hive.script.base.ScriptCommand
import com.hive.script.base.ScriptConst
import com.hive.script.base.core.ScriptRegularInterface
import com.hive.script.base.core.ScriptThreadManager
import com.hive.script.driver.ScriptEventHelper
import com.hive.script.utils.ScriptHelper
import com.hive.script.views.manager.ScriptManager
import com.hive.utils.GlobalApp
import com.hive.utils.thread.UIHandlerUtils
import com.hive.views.widgets.CommonToast

/**
 *
 * @author jiadou
 * @email 172111432@qq.com
 * @date 6/8/21
 */
@AutoCmdRegister(type = IDS.CmdUnlock, name = "actionUnlock")
class CmdActionUnlock : ScriptCommand(), ScriptRegularInterface {

    override fun onExecute() : CmdExecuteResult {
        if (ScriptEventHelper.get()
                .isScreenLocked()
            && ScriptManager.isUnlockScriptExist()
        ) {
            startUnlockScreen()
        } else {
            if (!ScriptEventHelper.get().isScreenOn()) {
                ScriptEventHelper.get().wakeScreen()
            }
        }
        if (!ScriptManager.isUnlockScriptExist()) {
            ScriptHelper.runInMain {
                CommonToast.getInstance().showToast(com.hive.i8n.R.string.sc_cmd_unlock_not_setting)
            }
        }
        ScriptThreadManager.delay(ScriptConst.Cmd_Default_Bias)
        return CmdExecuteResult.success()
    }

    override fun getCommand() = cmdPrefix()

    override fun getCommandName() = GlobalApp.getString(com.hive.i8n.R.string.cmd_name_action_unlock)

    override fun getCommandDescribe() = GlobalApp.getString(com.hive.i8n.R.string.cmd_name_action_unlock)

    override fun getCommandIcon() = R.drawable.sc_icon_unlock

    override fun parseCmd(cmd: String) {
    }

    override fun getPermissionRequest()= mutableListOf(ScriptHelper.PERMISSION_UNLOCK_SERVICE)

    companion object {
        fun createCommand() = CmdActionUnlock()
    }
}