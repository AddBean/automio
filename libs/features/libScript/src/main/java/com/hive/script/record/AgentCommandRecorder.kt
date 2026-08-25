// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.record

import com.hive.script.base.ScriptCommand
import com.hive.script.cmd.CmdFor
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Agent 工具命令层级录制器。
 *
 * 当 Skill 执行时，通过 pushSkillContext/popSkillContext 建立层级上下文，
 * 工具命令会通过 CmdFor.commandQueue.add 加入，最终录制出带层级的脚本。
 *
 * 使用方式：
 * - Agent 开始：startRecord()
 * - Skill 开始：pushSkillContext(skillId)
 * - 工具执行：addCommand(cmd)（由 ScriptMcpRegister 调用）
 * - Skill 结束：popSkillContext()
 * - Agent 结束：endRecord()
 */
object AgentCommandRecorder {

    private val lock = ReentrantLock()

    /** 顶层录制结果（平铺命令字符串 + Skill 块字符串） */
    private val topLevelRecords = mutableListOf<String>()

    /** Skill 上下文栈，栈顶为当前 Skill 的 CmdFor */
    private val skillContextStack = mutableListOf<CmdFor>()

    /**
     * 添加命令到录制。
     * - 若在 Skill 上下文中：加入当前 CmdFor.commandQueue
     * - 否则：加入顶层平铺列表
     */
    fun addCommand(cmd: ScriptCommand?) {
        cmd ?: return
        lock.withLock {
            if (skillContextStack.isNotEmpty()) {
                val cmdFor = skillContextStack.last()
                cmdFor.addCommandQueue(cmd)
            } else {
                topLevelRecords.add(cmd.getCommandLines())
            }
        }
    }

    /**
     * 进入 Skill 上下文。Skill 内的工具命令将加入该 CmdFor.commandQueue。
     */
    fun pushSkillContext(skillId: String) {
        lock.withLock {
            val cmdFor = CmdFor.createCommand(1, mutableListOf())
            cmdFor.comment = skillId
            skillContextStack.add(cmdFor)
        }
    }

    /**
     * 退出 Skill 上下文。将当前 CmdFor 转为字符串加入顶层录制。
     */
    fun popSkillContext() {
        lock.withLock {
            if (skillContextStack.isEmpty()) return
            val cmdFor = skillContextStack.removeAt(skillContextStack.lastIndex)
            if (skillContextStack.isNotEmpty()) {
                // 存在父 Skill：将当前 Skill 块作为子命令插入父块，保留 skill->skill 层级
                skillContextStack.last().addCommandQueue(cmdFor)
            } else {
                topLevelRecords.add(cmdFor.getCommandLines())
            }
        }
    }

    /** 开始录制，清空状态 */
    fun startRecord() {
        lock.withLock {
            topLevelRecords.clear()
            skillContextStack.clear()
        }
    }

    /**
     * 结束录制，返回命令字符串列表。
     * 会先弹出所有未关闭的 Skill 上下文，确保数据完整。
     */
    fun endRecord(): List<String> {
        lock.withLock {
            while (skillContextStack.isNotEmpty()) {
                val cmdFor = skillContextStack.removeAt(skillContextStack.lastIndex)
                if (skillContextStack.isNotEmpty()) {
                    skillContextStack.last().addCommandQueue(cmdFor)
                } else {
                    topLevelRecords.add(cmdFor.getCommandLines())
                }
            }
            return topLevelRecords.toList()
        }
    }

    /** 是否处于 Skill 上下文中 */
    fun isInSkillContext(): Boolean = lock.withLock { skillContextStack.isNotEmpty() }
}
