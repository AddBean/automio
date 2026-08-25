// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.timer.utils

import androidx.annotation.IntDef
import com.hive.timer.AlarmEntity
import com.hive.timer.AlarmTaskEntity
import com.hive.timer.db.AlarmClock
import com.hive.timer.db.AlarmDbService
import com.hive.timer.db.AlarmLog
import com.hive.utils.utils.GsonHelper

object TimerLogger {

    fun logE(logTag: String?, alarmId: Long?) {
        alarmId?: return
        AlarmDbService.get(alarmId)?.run {
            log(LogLevelError, logTag, this)
        }

    }

    fun logW(logTag: String?, alarmId: Long?) {
        alarmId?: return
        AlarmDbService.get(alarmId)?.run {
            log(LogLevelWarn, logTag, this)
        }
    }

    fun logI(logTag: String?, alarmId: Long?) {
        alarmId?: return
        AlarmDbService.get(alarmId)?.run {
            log(LogLevelInfo, logTag, this)
        }
    }

    fun log(@TimerLogLevel logLevel: Int, logTag: String?, alarm: AlarmClock) {
        val entity = GsonHelper.getInstance().fromJson(
            alarm.alarmJson,
            AlarmEntity::class.java
        )
        val taskEntity = GsonHelper.getInstance().fromJson(
            entity.taskInfo,
            AlarmTaskEntity::class.java
        )
        val record = AlarmLog()
        record.logTime = System.currentTimeMillis()
        record.alarmId = alarm.alarmId
        record.logLevel = logLevel
        record.logTag = logTag
        record.logInfo = alarm.alarmJson
        record.taskName = taskEntity.scriptName
        record.taskJson = entity.taskInfo
        record.save()
    }

    const val LogLevelError = 2
    const val LogLevelWarn = 1
    const val LogLevelInfo = 0

    @IntDef(LogLevelError, LogLevelWarn, LogLevelInfo)
    annotation class TimerLogLevel
}