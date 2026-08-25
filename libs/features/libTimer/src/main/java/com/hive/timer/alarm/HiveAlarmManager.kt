// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.timer.alarm

import android.content.Intent
import com.hive.timer.utils.AlarmTimerUtils
import com.hive.utils.thread.UIHandlerUtils
import java.util.Date
import java.util.Timer
import java.util.TimerTask

class HiveAlarmManager : IHiveAlarmManger {
    private var onAlarmListener: OnHiveAlarmListener? = null

    private val alarmMap = mutableMapOf<Long, AlarmClock>()

    private var nextAlarm: Pair<Long, Long>? = null

    private val timer = Timer()

    override fun init(lister: OnHiveAlarmListener) {
        this.onAlarmListener = lister
        timer.schedule(object : TimerTask() {
            override fun run() {
                UIHandlerUtils.getInstance().executeInMainThread {
                    checkAlarm()
                }
            }
        }, AlarmTimerUtils.getNextAlignTime() - System.currentTimeMillis(), 1000)
    }

    override fun setRepeating(
        triggerAtMillis: Long,
        intervalMillis: Long,
        requestId: Long,
        intent: Intent?
    ) {
        intent?.action = IHiveAlarmManger.Alarm_Action
        alarmMap[requestId] = AlarmClock().apply {
            this.triggerAtMillis = triggerAtMillis
            this.intervalMillis = intervalMillis
            this.intent = intent
        }
        refreshNextAlarm()
    }

    override fun cancel(requestId: Long) {
        if (alarmMap.contains(requestId))
            alarmMap.remove(requestId)
        refreshNextAlarm()
    }


    override fun getNextAlarmDate(): Date? {
        return nextAlarm?.takeIf { it.first > -1 && it.second >= System.currentTimeMillis() }
            ?.run { Date(nextAlarm!!.second) } ?: null
    }

    override fun getRequestIdList(): List<Long> {
        return alarmMap.keys.toList()
    }

    override fun getNextAlarmClock(): AlarmClock? {
        return nextAlarm?.takeIf { it.first > -1 && it.second >= System.currentTimeMillis() }
            ?.run { alarmMap[nextAlarm?.first] } ?: null
    }

    override fun checkAlarm() {
        nextAlarm?.run {
            if (this.first != -1L && this.second <= System.currentTimeMillis()) {
                onAlarmListener?.onTriggerAlarm(first, alarmMap[first]!!)
            }
        }
        refreshNextAlarm()
    }

    private fun refreshNextAlarm() {
        nextAlarm = AlarmTimerUtils.findNextAlarm(alarmMap)
    }

}