// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.cmd

import com.hive.script.R
import com.hive.script.base.ScriptCommand
import com.hive.script.base.ScriptConst
import com.hive.script.base.core.ScriptInterpreter
import com.hive.script.base.core.ScriptRegularInterface
import com.hive.script.base.core.ScriptThreadManager
import com.hive.script.extensions.findRootCommand
import com.hive.script.extensions.forEachAllCommand
import com.hive.script.base.core.ScriptLineTokenizer
import com.hive.utils.GlobalApp
import com.hive.views.widgets.CommonToast

/**
 *
 * @author jiadou
 * @date 6/8/21
 */
@AutoCmdRegister(type = IDS.CmdJump, name = "jump")
class CmdJump : ScriptCommand(), ScriptRegularInterface {
    var id = 0


    override fun onExecute() : CmdExecuteResult {
        findTargetPoint(id)?.run {
            doExecuteFromRoot(this)
        } ?: run {
            CommonToast.getInstance().showToast(com.hive.i8n.R.string.cmd_jump_point_not_found)
        }
        ScriptThreadManager.delay(getCommandDuration())
        return CmdExecuteResult.success()
    }

    private fun findTargetPoint(id: Int): ScriptCommand? {
        var targetCmd: ScriptCommand? = null
        findRootCommand().forEachAllCommand {
            if (it is CmdJumpPoint && it.id == id) {
                targetCmd = it
                return@forEachAllCommand
            }
        }
        return targetCmd
    }

    /**
     * 从根节点找到目标节点并开始执行
     */
    private fun doExecuteFromRoot(targetCmd: ScriptCommand) {
        ScriptInterpreter.getDefault().jumpToCommand(targetCmd)
    }

    private fun getCommandDuration() = ScriptConst.Cmd_Default_Bias

    override fun getCommand() = "${cmdPrefix()} target=$id"

    override fun getCommandName(): String =
        if (id != 0) getCommandDescribe() else GlobalApp.getString(com.hive.i8n.R.string.cmd_name_jump)

    override fun getCommandDescribe(): String = GlobalApp.getString(com.hive.i8n.R.string.cmd_des_jump, id)

    override fun getCommandIcon() = R.drawable.sc_cmd_jump


    override fun parseCmd(cmd: String) {
        id = ScriptLineTokenizer.parseKeyValueParams(cmd)["target"]?.toIntOrNull() ?: 0
    }

    override fun isSupportDelay() = false

    companion object {
        fun createCommand(id: Int) = CmdJump().apply {
            this.id = id
        }
    }
}