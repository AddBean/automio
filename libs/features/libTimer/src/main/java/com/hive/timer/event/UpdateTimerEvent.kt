// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.timer.event

import com.hive.timer.AlarmEntity

data class UpdateTimerEvent(var timer: AlarmEntity? = null) {
}