// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.cmd

import com.hive.script.R
import com.hive.script.base.ScriptCommand
import com.hive.script.base.ScriptConst
import com.hive.script.base.core.ScriptRegularInterface
import com.hive.script.base.core.ScriptThreadManager
import com.hive.script.extensions.forEachAllCommand
import com.hive.script.base.core.ScriptLineTokenizer
import com.hive.utils.GlobalApp

/**
 *
 * @author jiadou
 * @email 172111432@qq.com
 * @date 6/8/21
 */
@AutoCmdRegister(type = IDS.CmdJumpPoint, name = "jumpPoint")
class CmdJumpPoint : ScriptCommand(), ScriptRegularInterface {
    var id = 0

    override fun onExecute(): CmdExecuteResult {
        ScriptThreadManager.delay(getCommandDuration())
        return CmdExecuteResult.success()
    }


    private fun getCommandDuration() = ScriptConst.Cmd_Default_Bias

    override fun getCommand() = "${cmdPrefix()} id=$id"

    override fun getCommandName(): String = GlobalApp.getString(com.hive.i8n.R.string.cmd_name_jump_point) + id

    override fun getCommandDescribe(): String = GlobalApp.getString(com.hive.i8n.R.string.cmd_des_jump_point, id)

    override fun getCommandIcon() = R.drawable.sc_cmd_jump_point

    override fun parseCmd(cmd: String) {
        id = ScriptLineTokenizer.parseKeyValueParams(cmd)["id"]?.toIntOrNull() ?: 0
    }

    override fun isSupportDelay() = false

    companion object {
        fun createCommand(rootCmd: ScriptCommand) = CmdJumpPoint().apply {
            val pointIds = mutableListOf<Int>()
            rootCmd.forEachAllCommand {
                if (it is CmdJumpPoint) {
                    pointIds.add(it.id)
                } else if (it is CmdJump) {
                    pointIds.add(it.id)
                }
            }
            this.id = (pointIds.maxOrNull() ?: 0) + 1
        }
    }
}