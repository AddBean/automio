// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.timer.utils

import com.hive.timer.alarm.AlarmClock
import com.hive.utils.utils.StringUtils
import java.util.Date

object AlarmTimerUtils {

    fun formatHHMMSSTime(time: Long): String {
        val hour = time / (60 * 60 * 1000)
        val minute = (time - hour * 60 * 60 * 1000) / (60 * 1000)
        val second = (time - hour * 60 * 60 * 1000 - minute * 60 * 1000) / 1000
        return "${if (hour < 10) "0$hour" else hour}:${if (minute < 10) "0$minute" else minute}:${if (second < 10) "0$second" else second}"
    }

    fun dateFormat(date: Date?): String? {
        return StringUtils.dateFormat(date, "MM.dd HH:mm:ss")
    }

    fun dateFormatHHMMSS(date: Date?): String? {
        return StringUtils.dateFormat(date, "HH:mm:ss")
    }

    fun getNextAlignTime(): Long {
        val calendar = java.util.Calendar.getInstance()
        calendar.add(java.util.Calendar.SECOND, 1)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        return calendar.timeInMillis.takeIf { it > 0 } ?: 0
    }

    fun findNextAlarm(alarmMap: Map<Long, AlarmClock>): Pair<Long, Long>? {
        alarmMap.takeIf { it.isNotEmpty() } ?: return null
        val sortedList = alarmMap.map { it.key to it.value }
        val nextAlarms = mutableListOf<Pair<Long, Long>>()
        sortedList.forEach {
            var triggerAtTimeInFuture = 0L
            var paddingTime = 0L
            while (triggerAtTimeInFuture < System.currentTimeMillis()) {
                triggerAtTimeInFuture = it.second.triggerAtMillis + paddingTime
                if (triggerAtTimeInFuture > System.currentTimeMillis()) {
                    nextAlarms.add(it.first to triggerAtTimeInFuture)
                } else {
                    paddingTime += it.second.intervalMillis
                }
            }
        }
        return nextAlarms.sortedBy { it.second }.firstOrNull()
    }
}