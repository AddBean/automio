// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.timer.alarm

import android.content.Intent
class AlarmClock {
    var triggerAtMillis: Long = 0
    var intervalMillis: Long = 0
    var intent: Intent? = null
}