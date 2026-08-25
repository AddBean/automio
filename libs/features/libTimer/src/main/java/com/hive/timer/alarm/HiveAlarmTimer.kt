// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.timer.alarm

import android.content.Intent
import com.hive.utils.GlobalApp
import com.hive.utils.thread.UIHandlerUtils
import java.util.Date
import java.util.Timer

class HiveAlarmTimer : OnHiveAlarmListener {
    private val timer = Timer()

    private var hasSendBefore = false


    fun start(onNext: ((date: Date?) -> Unit)?) {
        HiveAlarmFactory.getDefault().init(this)
        timer.schedule(object : java.util.TimerTask() {
            override fun run() {
                UIHandlerUtils.getInstance().executeInMainThread {
                    onCheckAlarm(onNext)
                }
            }
        }, 0, 1000)
    }

    override fun onTriggerAlarm(requestId: Long, alarmClock: AlarmClock) {
        GlobalApp.getApp().sendBroadcast(Intent(IHiveAlarmManger.Alarm_Action).apply {
            putExtra("entity", alarmClock.intent?.getSerializableExtra("entity"))
        })
    }

    private fun onCheckAlarm(onNext: ((date: Date?) -> Unit)?) {
        HiveAlarmFactory.getDefault().getNextAlarmDate()?.run {
            if (!hasSendBefore && this.time - System.currentTimeMillis() <= IHiveAlarmManger.Before_Alarm_Action_Time) {
                HiveAlarmFactory.getDefault().getNextAlarmDate()
                GlobalApp.getApp()
                    .sendBroadcast(Intent(IHiveAlarmManger.Before_Alarm_Action))
                hasSendBefore = true
            }
            if ((this.time - System.currentTimeMillis()) > IHiveAlarmManger.Before_Alarm_Action_Time) {
                hasSendBefore = false
            }
            onNext?.invoke(this)
        } ?: run {
            onNext?.invoke(null)
        }
    }

    fun stop() {
        timer.cancel()
    }
}