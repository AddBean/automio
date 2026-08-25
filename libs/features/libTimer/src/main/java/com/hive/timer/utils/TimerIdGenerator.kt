// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.timer.utils

import java.util.concurrent.atomic.AtomicInteger

object TimerIdGenerator {
    private val counter = AtomicInteger(0)

    @JvmStatic
    fun newId(nowMillis: Long = System.currentTimeMillis()): Long {
        val suffix = counter.getAndIncrement()
        val safeSuffix = ((suffix % 1000) + 1000) % 1000
        return nowMillis * 1000L + safeSuffix
    }
}

