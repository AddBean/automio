// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.timer.alarm

object HiveAlarmFactory {

    private val hiveAlarmManager = HiveAlarmManager()

    private val sysAlarmManager = SystemAlarmManager()

    fun getDefault(): IHiveAlarmManger = hiveAlarmManager
}