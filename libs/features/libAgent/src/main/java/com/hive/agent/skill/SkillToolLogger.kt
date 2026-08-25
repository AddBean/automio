// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.agent.skill

import com.hive.plugin.agent.ExecutionContextFrame
import com.hive.utils.debug.DLog

/**
 * 统一 Skill Tool 调试日志，tag 固定为 AgentSkillTool，便于 logcat 过滤。
 * 支持在 ExecutionContext 栈变化时打印帧栈。
 */
object SkillToolLogger {

    const val TAG = "AgentSkillTool"

    @Volatile
    private var lastLoggedStackStr: String? = null

    fun logFrameStack(snapshot: List<ExecutionContextFrame>) {
        synchronized(this) {
            val str = formatFrameStack(snapshot)
            if (str != lastLoggedStackStr) {
                lastLoggedStackStr = str
                DLog.d(TAG, "frameStack: $str")
            }
        }
    }

    fun formatFrameStack(snapshot: List<ExecutionContextFrame>): String =
        snapshot.joinToString(" -> ") { "${it.type}:${it.name}:${it.id}" }

    fun d(msg: String) {
        DLog.d(TAG, msg)
    }

    fun e(msg: String, e: Throwable? = null) {
        if (e is Exception) DLog.e(TAG, msg, e) else DLog.e(TAG, msg)
    }

    fun w(msg: String) {
        DLog.w(TAG, msg)
    }
}
