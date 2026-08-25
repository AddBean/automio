// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.timer.broadcast

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.hive.timer.AlarmEntity
import com.hive.timer.R
import com.hive.timer.alarm.HiveAlarmFactory
import com.hive.timer.alarm.IHiveAlarmManger
import com.hive.timer.event.OnTimeAlarmEvent
import com.hive.timer.utils.TimerLogger
import com.hive.utils.GlobalApp
import com.hive.utils.debug.DLog
import org.greenrobot.eventbus.EventBus


class AlarmMainReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        val action = intent?.action
        val entity = intent?.getSerializableExtra("entity") as AlarmEntity?
        if (action == IHiveAlarmManger.Alarm_Action) {
            entity ?: return
            DLog.e("AlarmMainReceiver", "onReceive entity: $entity")
            TimerLogger.logI(GlobalApp.getString(com.hive.i8n.R.string.timer_script_try_start), entity.alarmId)
            EventBus.getDefault().post(OnTimeAlarmEvent(entity, false))
            HiveAlarmFactory.getDefault().checkAlarm()
        } else if (action == IHiveAlarmManger.Before_Alarm_Action) {
            EventBus.getDefault().post(OnTimeAlarmEvent(entity, true))
        }
    }

}