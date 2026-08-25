// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.driver

import com.hive.script.base.core.ScriptInterpreter
import com.hive.script.setting.ScriptSetting
import com.hive.timer.alarm.HiveAlarmFactory
import java.util.Timer
import java.util.TimerTask

class ScriptKeepAliveHelper : TimerTask() {

    private val Keep_Alive_Gap = 1000 * 12L

    private val timer = Timer()

    override fun run() {
        if (!ScriptEventHelper.get().isScreenOn()
            && !ScriptInterpreter.getDefault().isRunning()
        ) {
            if (HiveAlarmFactory.getDefault()
                    .getNextAlarmDate() != null && ScriptSetting.script_setting_keep_alive
            ) {
//                ScriptEventHelper.instance.performActionWakeUpScreen()
//                ScriptEventHelper.get().performActionWakeUpScreen {
//                    ScriptEventHelper.get().performActionHome()
//                }
            }
        }
    }


    fun start() {
//        timer.schedule(this, Keep_Alive_Gap, Keep_Alive_Gap)
    }

    fun stop() {
//        timer.cancel()
    }

    companion object {

        private val instance = ScriptKeepAliveHelper()

        @JvmStatic
        fun get() = instance
    }


}