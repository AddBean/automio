// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script

import com.hive.script.base.ScriptConst
import com.hive.script.base.core.ScriptInterpreter
import com.hive.script.driver.ScriptEventHelper
import com.hive.script.setting.ScriptSetting
import com.hive.script.utils.ScriptHelper
import com.hive.script.views.manager.ScriptManager
import com.hive.script.views.manager.ScriptMenuManager
import com.hive.script.views.menu.ScriptControlView
import com.hive.script.views.tips.BaseScriptTipsHelper
import com.hive.script.views.tips.ScriptBeforeRunningTipView
import com.hive.timer.AlarmTaskEntity
import com.hive.timer.alarm.HiveAlarmFactory
import com.hive.timer.utils.TimerLogger
import com.hive.utils.GlobalApp
import com.hive.views.widgets.CommonToast
import java.io.File

object ScriptAlarmTaskHelper {
    private var disableAlarmTaskOnce = false

    /**
     * 取消一次闹钟工作流
     */
    fun disableAlarmTaskOnce() {
        disableAlarmTaskOnce = true
    }

    /**
     * 展示运行提示框
     */
    fun showAlarmTipsDialog() {
        val nextTime = HiveAlarmFactory.getDefault().getNextAlarmDate()?.time
        val leftTime = (((nextTime ?: 0L) - System.currentTimeMillis()) / 1000).toInt()
        if (leftTime > 1) {
            ScriptBeforeRunningTipView(
                GlobalApp.getContext(), leftTime, HiveAlarmFactory.getDefault().getNextAlarmClock()
            ).setCancelClickListener { dialog ->
                dialog.dismiss()
            }.setSubmitClickListener { dialog ->
                disableAlarmTaskOnce()
                dialog.dismiss()
            }.show()
        }
    }

    /**
     * 开始闹钟工作流
     */
    fun startAlarmTask(taskInfo: AlarmTaskEntity, alarmId: Long?) {
        try {
            checkEnvironmentAndThrowException(taskInfo, alarmId)
            if (ScriptSetting.script_setting_time_task_force_to_running) {
                if (ScriptInterpreter.getDefault().isRunning()) {
                    TimerLogger.logI(
                        GlobalApp.getString(com.hive.i8n.R.string.script_log_stop_success), alarmId
                    )
                    ScriptManager.stopPlay()
                }
            }
            ScriptMenuManager.disableStopDialogOnce()
            ScriptMenuManager.getMenuView()?.saveMode()
            ScriptMenuManager.switchMenuMode(ScriptControlView.MenuMode.PLAYING_MENU)
            ScriptManager.startPlay(
                taskInfo.scriptPath,
                showPlayDialog = false,
                enableAutoUnlock = true
            )
            TimerLogger.logI(
                GlobalApp.getString(com.hive.i8n.R.string.script_log_running_success), alarmId
            )
        } catch (e: Exception) {
            TimerLogger.logE(e.message, alarmId)
            CommonToast.show(e.message)
        }
    }

    private fun checkEnvironmentAndThrowException(taskInfo: AlarmTaskEntity, alarmId: Long?) {
        if (ScriptEventHelper.get()
                .isScreenLocked() && !File(ScriptConst.Task_Screen_Lock_Script_Main_Path).exists()
        ) {
            ScriptHelper.runInMain {
                BaseScriptTipsHelper.showUnlockTips()
            }
            throw Exception(GlobalApp.getString(com.hive.i8n.R.string.script_log_accessibility_service_lock))
        }
        if (disableAlarmTaskOnce) {
            disableAlarmTaskOnce = false
            throw Exception(GlobalApp.getString(com.hive.i8n.R.string.script_log_accessibility_service_has_been_canceled))
        }
        if (!File(taskInfo.scriptPath).exists()) {
            throw Exception(GlobalApp.getString(com.hive.i8n.R.string.script_log_accessibility_service_not_exist))
        }

        if (ScriptManager.isEditPanelShowing()) {
            throw Exception(GlobalApp.getString(com.hive.i8n.R.string.script_log_accessibility_service_edit_tip))
        }

        if (!ScriptInterpreter.getDefault().isRunning()) {
            if (!ScriptManager.checkServerEnable()) {
                throw Exception(GlobalApp.getString(com.hive.i8n.R.string.script_log_accessibility_service_not_open))
            }
        } else {
            if (!ScriptSetting.script_setting_time_task_force_to_running) {
                throw Exception(GlobalApp.getString(com.hive.i8n.R.string.script_log_already_has_script_running))
            }
        }
    }
}