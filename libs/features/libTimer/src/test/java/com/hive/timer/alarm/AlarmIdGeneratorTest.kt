// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.timer.alarm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AlarmIdGeneratorTest {

    /**
     * Replicates the generateAlarmId logic from AlarmManagerWrapper to test for collisions.
     * Uses bit-shifting: high 32 bits = alarmId, low 32 bits = itemId.
     */
    private fun generateAlarmId(alarmId: Long, itemId: Long): Long {
        return (alarmId shl 32) or (itemId and 0xFFFFFFFFL)
    }

    @Test
    fun generateAlarmId_noCollisions_differentAlarms() {
        // Simulate multiple alarm schedules, each with multiple alarm times
        val alarmIds = listOf(1000L, 2000L, 3000L, 999999L, Long.MAX_VALUE shr 1)
        val itemIds = listOf(1L, 500L, 1000L, 2001L, 4294967295L) // including max uint

        val requestIds = mutableSetOf<Long>()
        for (alarmId in alarmIds) {
            for (itemId in itemIds) {
                val requestId = generateAlarmId(alarmId, itemId)
                val added = requestIds.add(requestId)
                assertTrue(
                    "Collision detected: alarmId=$alarmId, itemId=$itemId produced requestId=$requestId",
                    added
                )
            }
        }
        assertEquals(alarmIds.size * itemIds.size, requestIds.size)
    }

    @Test
    fun generateAlarmId_oldApproachHadCollisions() {
        // Demonstrate that the old addition-based approach had collisions
        val oldIds = mutableSetOf<Long>()
        val collisions = mutableListOf<String>()
        val testPairs = listOf(
            1000L to 2001L,
            2000L to 1001L,
            3000L to 1L,
            1500L to 1501L,
        )

        for (pair in testPairs) {
            val oldId = pair.first + pair.second
            val existed = oldIds.contains(oldId)
            if (existed) collisions.add("${pair.first}+${pair.second}=$oldId")
            oldIds.add(oldId)
        }

        assertTrue(
            "Expected collisions with addition approach but found none: $collisions",
            collisions.isNotEmpty()
        )
    }

    @Test
    fun generateAlarmId_reversible() {
        // Verify that alarmId and itemId can be extracted back from the requestId
        // alarmId must fit in 32 bits (realistic values from TimerIdGenerator)
        val testPairs = listOf(
            1000L to 100L,
            0L to 0L,
            999999L to 4294967295L,
            1L to 1L,
            123456789L to 999L,
        )

        for ((alarmId, itemId) in testPairs) {
            val requestId = generateAlarmId(alarmId, itemId)
            val extractedAlarmId = requestId ushr 32
            val extractedItemId = requestId and 0xFFFFFFFFL

            assertEquals("alarmId mismatch for pair ($alarmId, $itemId)", alarmId, extractedAlarmId)
            assertEquals("itemId mismatch for pair ($alarmId, $itemId)", itemId, extractedItemId)
        }
    }
}
