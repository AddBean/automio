// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.cmd

import android.text.TextUtils
import com.hive.script.R
import com.hive.script.base.ScriptCommand
import com.hive.script.base.core.ScriptRegularInterface
import com.hive.script.base.core.ScriptThreadManager
import com.hive.script.exception.ScriptBreakException
import com.hive.script.exception.ScriptInterruptedException
import com.hive.script.base.core.ScriptLineTokenizer
import com.hive.utils.GlobalApp

/**
 *
 * @author jiadou
 * @date 6/8/21
 */
@AutoCmdRegister(type = IDS.CmdFor, name = "for")
class CmdFor : ScriptCommand(), ScriptRegularInterface {
    var id = 1

    var loopCount: Int = -1

    override fun onExecute(): CmdExecuteResult {
        var count = loopCount
        //-1 0 为无限循环
        while (count > 0 || loopCount == -1 || loopCount == 0) {
            val iterator: MutableIterator<ScriptCommand> = commandQueue.iterator()
            try {
                while (iterator.hasNext()) {
                    val it = iterator.next()
                    try {
                        it.doExecute()
                        ScriptThreadManager.checkInterrupted()
                    } catch (e: ScriptBreakException) {     //如果是break命令，直接跳出循环
                        return CmdExecuteResult.success()
                    }
                    if (Thread.currentThread().isInterrupted) {
                        throw ScriptInterruptedException()
                    }
                }
            } catch (e: ConcurrentModificationException) {
                e.printStackTrace()
            }
            count--
        }
        return CmdExecuteResult.success()
    }

    override fun onExecuteJump(cmd: ScriptCommand?) {
        val iterator: MutableIterator<ScriptCommand> = commandQueue.iterator()
        try {
            while (iterator.hasNext()) {
                try {
                    val it = iterator.next()
                    it.doExecute()
                    ScriptThreadManager.checkInterrupted()
                } catch (e: ScriptBreakException) {     //如果是break命令，直接跳出循环
                    return
                }
                if (Thread.currentThread().isInterrupted) {
                    throw ScriptInterruptedException()
                }
            }
        } catch (e: ConcurrentModificationException) {
            e.printStackTrace()
        }
    }

    override fun isSupportDelay() = false

    override fun getCommandName() = GlobalApp.getString(com.hive.i8n.R.string.cmd_name_for)

    override fun getCommandDescribe() =
        if (loopCount <= 0) GlobalApp.getString(com.hive.i8n.R.string.cmd_des_for_infinate) else GlobalApp.getString(
            com.hive.i8n.R.string.cmd_des_for,
            loopCount
        )


    override fun getCommandLines(): String {
        val sb = StringBuilder()
        var commentInfo = ""
        //添加注释
        if (!TextUtils.isEmpty(comment)) {
            commentInfo = " #$comment"
        }
        sb.append(getCommandIndentation() + "for count=$loopCount:$commentInfo")
        sb.append("\n")
        commandQueue.forEach {
            sb.append(it.getCommandLines())
            sb.append("\n")
        }
        sb.append(getCommandIndentation() + CmdEnd().getCommandLines())
        return sb.toString()
    }

    override fun getCommand(): String {
        val sb = StringBuilder()
        sb.append("${cmdPrefix()} count=$loopCount:")
        sb.append("\n")
        commandQueue.forEach {
            sb.append(it.getCommand())
            sb.append("\n")
        }
        sb.append(CmdEnd().getCommand())
        return sb.toString()
    }

    override fun isGroupCommand(): Boolean = true

    override fun parseCmd(cmd: String) {
        val p = ScriptLineTokenizer.parseKeyValueParams(cmd)
        // count 值可能带尾随 ":" 如 "5:"
        loopCount = p["count"]?.replace(":", "")?.toIntOrNull() ?: -1
    }

    override fun getPermissionRequest() = null


    override fun getCommandIcon() = R.drawable.sc_icon_circly

    companion object {
        fun createCommand(loop: Int, queue: MutableList<ScriptCommand>) = CmdFor().apply {
            this.loopCount = loop
            this.commandQueue = queue
            queue.forEach {
                it.parentCommand = this
            }
        }
    }
}