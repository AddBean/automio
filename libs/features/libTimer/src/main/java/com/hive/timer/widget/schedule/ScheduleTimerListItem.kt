// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.timer.widget.schedule

import com.hive.timer.AlarmEntity
import com.hive.timer.db.AlarmLog

data class ScheduleTimerListItem(
    val alarm: AlarmEntity,
    val latestLog: AlarmLog?
)
