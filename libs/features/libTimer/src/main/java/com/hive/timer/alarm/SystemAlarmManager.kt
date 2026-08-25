// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.timer.alarm

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.hive.timer.broadcast.AlarmMainReceiver
import com.hive.timer.utils.AlarmTimerUtils
import com.hive.utils.GlobalApp
import com.hive.utils.debug.DLog
import java.util.Date

class SystemAlarmManager : IHiveAlarmManger {

    private val alarmManager =
        GlobalApp.getContext().getSystemService(Service.ALARM_SERVICE) as AlarmManager

    private val alarmMap = mutableMapOf<Long, AlarmClock>()

    private var nextAlarm: Pair<Long, Long>? = null
    private var scheduledRequestId: Long? = null

    private val flag =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE else PendingIntent.FLAG_UPDATE_CURRENT


    override fun init(lister: OnHiveAlarmListener) {

    }

    private fun buildAlarmIntent(requestId: Long, source: Intent? = null): Intent {
        val intent = source ?: Intent()
        intent.setClass(GlobalApp.getContext(), AlarmMainReceiver::class.java)
        intent.action = IHiveAlarmManger.Alarm_Action
        intent.data = Uri.parse("hivealarm://timer/alarm/$requestId")
        return intent
    }

    @SuppressLint("NewApi")
    override fun checkAlarm() {
        nextAlarm = AlarmTimerUtils.findNextAlarm(alarmMap)
        nextAlarm?.takeIf { it.first > 0 }?.run {
            val requestId = first
            val triggerAtMillis = second
            val alarmClock = alarmMap[requestId]
            alarmClock ?: return
            if (scheduledRequestId != null && scheduledRequestId != requestId) {
                cancelScheduled(scheduledRequestId!!)
            }
            val pi = PendingIntent.getBroadcast(
                GlobalApp.getContext(),
                requestId.toInt(),
                buildAlarmIntent(requestId, alarmClock.intent),
                flag
            )
            DLog.e(
                "AlarmManagerWrapper",
                "addAlarm requestId:$requestId entity: ${alarmClock.intent?.getSerializableExtra("entity")}"
            )
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP, triggerAtMillis, pi
                    )
                } else {
                    alarmManager.set(
                        AlarmManager.RTC_WAKEUP, triggerAtMillis, pi
                    )
                }
            } else {
                // Fallback to inexact alarms if exact alarms are not allowed
                DLog.e("SystemAlarmManager", "Exact alarms not allowed, using inexact alarm")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP, triggerAtMillis, pi
                    )
                } else {
                    alarmManager.set(
                        AlarmManager.RTC_WAKEUP, triggerAtMillis, pi
                    )
                }
            }
            scheduledRequestId = requestId
        }
    }

    override fun setRepeating(
        triggerAtMillis: Long, intervalMillis: Long, requestId: Long, intent: Intent?
    ) {
        intent ?: return
        val alarmIntent = buildAlarmIntent(requestId, intent)
        alarmMap[requestId] = AlarmClock().apply {
            this.triggerAtMillis = triggerAtMillis
            this.intervalMillis = intervalMillis
            this.intent = alarmIntent
        }
        checkAlarm()
    }

    override fun cancel(requestId: Long) {
        val pi = PendingIntent.getBroadcast(
            GlobalApp.getContext(),
            requestId.toInt(),
            buildAlarmIntent(requestId),
            flag
        )
        if (alarmMap.contains(requestId)) alarmMap.remove(requestId)
        alarmManager.cancel(pi)
        if (scheduledRequestId == requestId) scheduledRequestId = null
        nextAlarm = AlarmTimerUtils.findNextAlarm(alarmMap)
        checkAlarm()
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

    private fun cancelScheduled(requestId: Long) {
        val pi = PendingIntent.getBroadcast(
            GlobalApp.getContext(),
            requestId.toInt(),
            buildAlarmIntent(requestId),
            flag
        )
        alarmManager.cancel(pi)
    }

}
