// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.cmd

import android.text.TextUtils
import com.hive.script.R
import com.hive.script.base.ScriptCommand
import com.hive.script.base.ScriptCommandCondition
import com.hive.script.base.ScriptConst
import com.hive.script.base.core.ScriptRegularInterface
import com.hive.script.base.core.ScriptThreadManager
import com.hive.script.condition.ConditionNotification
import com.hive.script.exception.ScriptInterruptedException
import com.hive.utils.extends.string
import java.util.regex.Matcher
import java.util.regex.Pattern

@AutoCmdRegister(type = IDS.CmdIf, name = "if")
class CmdIf : ScriptCommand(), ScriptRegularInterface {

    private var matchPattern = """if\s+(.*):"""

    private var matchPatternNot = """if\s+not\s+(.*):"""

    private var matchPattern2 = """(.*)\s+then\s+(.*)"""//条件 then 动作

    private var matchPattern3 = """(.*)\s+delay\s+(.*)"""//条件 delay 动作

    var postAction = ScriptCommandCondition.Post_Action_Click

    var delayTime = ScriptConst.Cmd_Default_Delay

    override fun onExecute() : CmdExecuteResult {
        ScriptThreadManager.delay(ScriptConst.Cmd_Default_If_Delay)
        val iterator: MutableIterator<ScriptCommand> = commandQueue.iterator()
        try {
            while (iterator.hasNext()) {
                val it = iterator.next()
                it.doExecute()
                if (Thread.currentThread().isInterrupted) {
                    throw ScriptInterruptedException()
                }
            }
        } catch (e: ConcurrentModificationException) {
            e.printStackTrace()
        }
        return CmdExecuteResult.success()
    }

    override fun onExecuteJump(cmd: ScriptCommand?) {
        val iterator: MutableIterator<ScriptCommand> = commandQueue.iterator()
        while (iterator.hasNext()) {
            val it = iterator.next()
            it.doExecute()
            if (Thread.currentThread().isInterrupted) {
                throw ScriptInterruptedException()
            }
        }
    }

    override fun getCommandLines(): String {
        //确保条件内参数的一致性
        ensureConditionDataConsistency()
        val sb = StringBuilder()
        var commentInfo = ""
        //添加注释
        if (!TextUtils.isEmpty(comment)) {
            commentInfo = " #$comment"
        }
        val actionStr = if (TextUtils.isEmpty(postAction)) "" else " then $postAction"
        val conditionSplitChar = if (conditionMeetAll) " and " else " or "
        val condition =
            conditionList?.joinToString(
                conditionSplitChar,
                transform = ScriptCommandCondition::getCondition
            )
        val commonStr = getCommonLinesInfo()
        val ifCmdStr = if (!conditionReverse) "${cmdPrefix()} " else "${cmdPrefix()} not "
        val delayStr = if (delayTime > 0) " delay $delayTime" else ""
        sb.append(getCommandIndentation() + "$ifCmdStr$condition$delayStr$actionStr$commonStr:$commentInfo")
        sb.append("\n")
        commandQueue.forEach {
            sb.append(it.getCommandLines())
            sb.append("\n")
        }
        sb.append(getCommandIndentation() + CmdEnd().getCommandLines())
        return sb.toString()
    }

    private fun ensureConditionDataConsistency() {
        //确保条件内参数的一致性
        var targetParamId: String? = null
        conditionList?.filterIsInstance<ConditionNotification>()?.forEach {
            if (targetParamId == null) {
                targetParamId = it.targetParamId
            }
            it.targetParamId = targetParamId
        }
    }

    override fun getCommand(): String {
        val sb = StringBuilder()
        val actionStr = if (TextUtils.isEmpty(postAction)) "" else " then $postAction"
        val conditionSplitChar = if (conditionMeetAll) " and " else " or "
        val condition =
            conditionList?.joinToString(
                conditionSplitChar,
                transform = ScriptCommandCondition::getCondition
            )
        val ifCmdStr = if (!conditionReverse) "${cmdPrefix()} " else "${cmdPrefix()} not "
        sb.append(getCommandIndentation() + "$ifCmdStr$condition$actionStr:")
        sb.append("\n")
        commandQueue.forEach {
            sb.append(it.getCommand())
            sb.append("\n")
        }
        sb.append(CmdEnd().getCommand())
        return sb.toString()
    }

    override fun getCommandName() =
        if (!conditionReverse) com.hive.i8n.R.string.cmd_name_if_name.string() else com.hive.i8n.R.string.cmd_name_if_name_not.string()

    override fun getCommandDescribe() =
        com.hive.i8n.R.string.cmd_des_if_des.string(conditionList?.firstOrNull()?.getConditionDesc() ?: "")

    override fun getCommandIcon() = R.drawable.sc_icon_if

    override fun isGroupCommand(): Boolean = true

    override fun isSupportDelay() = false

    override fun isSupportRect(): Boolean = true

    override fun isSupportOffset() = true

    override fun parseCmd(cmd: String) {
        conditionReverse = Regex(matchPatternNot).matches(cmd)
        val mp = if (!conditionReverse) {
            matchPattern
        } else {
            matchPatternNot
        }
        val r: Pattern = Pattern.compile(mp)
        val m: Matcher = r.matcher(cmd)
        if (m.find()) {
            val cmdParams = m.group(1) ?: ""
            val r: Pattern = Pattern.compile(matchPattern2)
            val m: Matcher = r.matcher(cmdParams)
            var conditions = ""
            if (m.find()) {
                val g1 = (m.group(1) ?: "").trim()
                val g2 = (m.group(2) ?: "").trim()
                val r2: Pattern = Pattern.compile(matchPattern3)
                val m2: Matcher = r2.matcher(g1)
                if (m2.find()) {
                    val g11 = (m2.group(1) ?: "").trim()
                    val g12 = (m2.group(2) ?: "").trim()
                    conditions = g11
                    delayTime = g12.toLongOrNull() ?: 0L
                } else {
                    conditions = g1
                }
                postAction = g2
            } else {
                val r2: Pattern = Pattern.compile(matchPattern3)
                val m2: Matcher = r2.matcher(cmdParams)
                if (m2.find()) {
                    val g11 = (m2.group(1) ?: "").trim()
                    val g12 = (m2.group(2) ?: "").trim()
                    conditions = g11
                    delayTime = g12.toLongOrNull() ?: 0L
                } else {
                    conditions = cmdParams
                }
            }
            conditionList =
                conditions
                    .split(" or ", " and ")
                    .map { it.trim() }
                    .filter { !TextUtils.isEmpty(it.trim()) }.mapNotNull {
                        ScriptCommandCondition.getConditionEntity(it, this)
                    } as MutableList<ScriptCommandCondition>

            conditionMeetAll = conditions.contains(" and ")

        }
    }

    override fun onConditionMeet(condition: ScriptCommandCondition?) {
        ScriptThreadManager.delay(delayTime)
        condition?.doPostAction(postAction)
    }

    override fun matchCmd(cmd: String) =
        Regex(matchPattern).matches(cmd) || Regex(matchPatternNot).matches(cmd)

    companion object {
        fun createCommand(
            queue: MutableList<ScriptCommand>, conditions: MutableList<ScriptCommandCondition>?
        ) = CmdIf().apply {
            this.commandQueue = queue
            this.conditionList = conditions
            queue.forEach {
                it.parentCommand = this
            }
        }
    }
}