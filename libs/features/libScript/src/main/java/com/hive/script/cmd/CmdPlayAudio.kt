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
@AutoCmdRegister(type = IDS.CmdPlayAudio, name = "playAudio")
class CmdPlayAudio : ScriptCommand() , ScriptRegularInterface {

    override fun onExecute() : CmdExecuteResult {
        ScriptEventHelper.get().performPlayAudio()
        ScriptThreadManager.delay(getCommandDuration())
        return CmdExecuteResult.success()
    }

    private fun getCommandDuration() = ScriptConst.Cmd_Default_Play_Audio

    override fun getCommand() = cmdPrefix()

    override fun getCommandName(): String = GlobalApp.getString(com.hive.i8n.R.string.cmd_name_play_audio)

    override fun getCommandDescribe(): String = GlobalApp.getString(com.hive.i8n.R.string.cmd_des_play_audio)

    override fun getCommandIcon() = R.drawable.sc_cmd_reminder

    override fun parseCmd(cmd: String) {
    }

    companion object {
        fun createCommand() = CmdPlayAudio()
    }
}