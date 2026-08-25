// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.timer.alarm

interface OnHiveAlarmListener {

    fun onTriggerAlarm(requestId: Long, alarmClock: AlarmClock)

}