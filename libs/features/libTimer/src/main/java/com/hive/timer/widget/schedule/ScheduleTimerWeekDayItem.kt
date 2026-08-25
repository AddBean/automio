// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.timer.widget.schedule

import com.hive.timer.AlarmEntity

data class ScheduleTimerWeekDayItem(
    val dayIndex: Int,
    val dayName: String,
    val times: List<AlarmEntity.AlarmTime>,
)

