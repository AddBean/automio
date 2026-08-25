// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.base

import android.text.TextUtils


/**
 * 脚本操作时间管理
 * 计算原理：计算开始时间和结束时间的差值，通过比对前后命令名称来确定是否是同一次操作。
 * 缺点：如果A命令先执行编辑，其起点时间会被记录，此时取消A命令，再执行B命令，其结束时间会被置空，此次操作会视为0时间操作；
 */
class ScriptOperateTimeManager {

    private var targetCmdName: String? = null

    private var operateDuration = 0L

    private var operateStartTime = 0L

    fun startOperate(startCmd: String) {
        this.targetCmdName = startCmd
        operateStartTime = System.currentTimeMillis()
    }

    fun endOperate(endCmd: String) {
        if (operateStartTime == 0L) {
            return
        }
        if (targetCmdName != null && TextUtils.equals(targetCmdName, endCmd)) {
            operateDuration = System.currentTimeMillis() - operateStartTime
        } else {
            resetOperate()
        }
    }

    fun getOperateDuration(): Long {
        if (operateDuration < 0) {
            operateDuration = 0
        }
        return operateDuration
    }

    fun resetOperate() {
        operateDuration = 0
        operateStartTime = 0
        targetCmdName = null
    }

    companion object {
        private val instance: ScriptOperateTimeManager by lazy {
            ScriptOperateTimeManager()
        }

        fun get() = instance
    }
}