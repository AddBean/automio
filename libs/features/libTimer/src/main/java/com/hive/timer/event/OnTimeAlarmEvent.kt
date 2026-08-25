// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.timer.event

import com.hive.timer.AlarmEntity

data class OnTimeAlarmEvent(var alarmEntity: AlarmEntity?,var isBeforeRunning: Boolean = false)