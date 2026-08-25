// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.utils

import com.hive.statistic.Statistics

object StatisticsHelper {
    fun reportHomeTabEvent(name: String) {
        try {
            val map: MutableMap<String, String> = HashMap()
            map["tab_name"] = name
            Statistics.getInstance().onEvent("tab_event", map)
        } catch (_: Exception) {
        }
    }
}
