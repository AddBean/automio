// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.cmd

import com.hive.script.R
import com.hive.script.base.ScriptCommand
import com.hive.script.base.ScriptConst
import com.hive.script.base.core.ScriptRegularInterface
import com.hive.script.base.core.ScriptThreadManager
import com.hive.script.utils.ScriptCommonUtils
import com.hive.script.base.core.ScriptLineTokenizer
import com.hive.utils.GlobalApp

/**
 *
 * @author jiadou
 * @date 6/8/21
 */
@AutoCmdRegister(type = IDS.CmdDelay, name = "delay")
class CmdDelay : ScriptCommand(), ScriptRegularInterface {
    var startDuration: Long = ScriptConst.Cmd_Delay_Default

    var endDuration: Long = ScriptConst.Cmd_Delay_Default

    override fun onExecute() : CmdExecuteResult {
        ScriptThreadManager.delay(ScriptCommonUtils.getRandomDuration(startDuration, endDuration))
        return CmdExecuteResult.success()
    }


    override fun getCommand() = "${cmdPrefix()} start=$startDuration end=$endDuration"

    override fun getCommandName() = GlobalApp.getString(com.hive.i8n.R.string.cmd_name_delay)

    override fun getCommandDescribe() =
        GlobalApp.getString(com.hive.i8n.R.string.cmd_des_delay, startDuration / 1000f)

    override fun getCommandIcon() = R.drawable.sc_icon_delay

    

    override fun parseCmd(cmd: String) {
        val p = ScriptLineTokenizer.parseKeyValueParams(cmd)
        startDuration = p["start"]?.toLongOrNull() ?: ScriptConst.Cmd_Delay_Default
        endDuration = p["end"]?.toLongOrNull() ?: startDuration
    }

    override fun isSupportDelay() = false

    override fun getPermissionRequest() = null

    companion object {

        fun createCommand(duration: Long) = createCommand(duration, duration)

        fun createCommand(start: Long, end: Long) = CmdDelay().apply {
            this.startDuration = start
            this.endDuration = end
            this.startDelay = start
            this.endDelay = end
        }
    }
}