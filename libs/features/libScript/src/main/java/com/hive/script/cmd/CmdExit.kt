// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.cmd

import com.hive.script.R
import com.hive.script.base.ScriptCommand
import com.hive.script.base.ScriptConst
import com.hive.script.base.core.ScriptInterpreter
import com.hive.script.base.core.ScriptRegularInterface
import com.hive.script.base.core.ScriptThreadManager
import com.hive.script.views.manager.ScriptManager
import com.hive.utils.GlobalApp
import com.hive.utils.thread.UIHandlerUtils
import com.hive.script.utils.ScriptHelper
/**
 *
 * @author jiadou
 * @email 172111432@qq.com
 * @date 6/8/21
 */
@AutoCmdRegister(type = IDS.CmdExit, name = "exit")
class CmdExit : ScriptCommand() , ScriptRegularInterface {

    override fun onExecute() : CmdExecuteResult {
        if (!ScriptInterpreter.getDefault().isRecording()) {
            ScriptHelper.runInMain {
                ScriptManager.stopPlay()
            }
        }
        ScriptThreadManager.delay(ScriptConst.Cmd_Default_Spot)
        return CmdExecuteResult.success()
    }

    private fun getCommandDuration() = ScriptConst.Cmd_Default_Bias

    override fun getCommand() = cmdPrefix()

    override fun getCommandName(): String = GlobalApp.getString(com.hive.i8n.R.string.cmd_name_exit)

    override fun getCommandDescribe(): String = GlobalApp.getString(com.hive.i8n.R.string.cmd_name_exit)

    override fun getCommandIcon() = R.drawable.sc_icon_exit


    override fun parseCmd(cmd: String) {
    }

    override fun getPermissionRequest() = null

    companion object {
        fun createCommand() = CmdExit()
    }
}