// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.timer.alarm

import android.content.Intent
import com.hive.utils.GlobalApp
import java.util.Date

interface IHiveAlarmManger {

    fun init(lister: OnHiveAlarmListener)

    fun checkAlarm()

    fun setRepeating(triggerAtMillis: Long, intervalMillis: Long, requestId: Long, intent: Intent?)

    fun cancel(requestId: Long)

    fun getNextAlarmDate(): Date?

    fun getRequestIdList(): List<Long>

    fun getNextAlarmClock(): AlarmClock?

    companion object {

        @JvmStatic
        val Alarm_Action = GlobalApp.getPackageName() + ".Alarm.AlarmAction"

        @JvmStatic
        val Before_Alarm_Action = GlobalApp.getPackageName() + ".Alarm.BeforeAlarmAction"

        @JvmStatic
        val Before_Alarm_Action_Time = 10 * 1000L
    }
}