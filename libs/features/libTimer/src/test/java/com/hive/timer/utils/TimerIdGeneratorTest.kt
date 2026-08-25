// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.timer.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TimerIdGeneratorTest {

    @Test
    fun newId_sameMillis_produces1000UniqueSuffixes() {
        val nowMillis = 123456789L
        val ids = (0 until 1000).map { TimerIdGenerator.newId(nowMillis) }
        assertEquals(1000, ids.toSet().size)
        ids.forEach { id ->
            assertTrue(id in (nowMillis * 1000L)..(nowMillis * 1000L + 999))
        }
    }
}

