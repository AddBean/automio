// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.tips

import android.annotation.SuppressLint
import android.content.Context
import com.hive.script.R
import com.hive.script.views.widgets.BaseScriptTips
import com.hive.timer.AlarmEntity
import com.hive.timer.AlarmTaskEntity
import com.hive.timer.alarm.AlarmClock
import com.hive.utils.GlobalApp
import com.hive.utils.utils.GsonHelper
import java.util.Timer
import java.util.TimerTask

@SuppressLint("ViewConstructor")
class ScriptBeforeRunningTipView(context: Context, var showTime: Int, var alarmClock: AlarmClock?) :
    BaseScriptTips(context) {

    private var timer: Timer? = null

    override fun initWindow() {
        super.initWindow()
        setCancelText(GlobalApp.getString(com.hive.i8n.R.string.script_alarm_dialog_tip_btn_1))
        setSubmitText(GlobalApp.getString(com.hive.i8n.R.string.script_alarm_dialog_tip_btn_2))
        timer = Timer()
        startCountDown()
        updateUI()
    }

    override fun getBgColor() = 0x00000000

    private fun startCountDown() {
        timer?.schedule(object : TimerTask() {
            override fun run() {
                post {
                    if (showTime <= -1) {
                        dismiss()
                    } else {
                        updateUI()
                    }
                }
                showTime--
            }

        }, 0, 1000)
    }

    private fun updateUI() {
        val entity =
            alarmClock?.intent?.getSerializableExtra("entity") as AlarmEntity?
        val taskInfo =
            GsonHelper.getInstance()
                .fromJson(entity?.taskInfo, AlarmTaskEntity::class.java)
        taskInfo ?: return
        setTitleText(
            GlobalApp.getString(
                com.hive.i8n.R.string.script_before_running_tips_title,
                taskInfo.scriptName
            )
        )
        setMsgText(
            GlobalApp.getString(
                com.hive.i8n.R.string.script_before_running_tips_msg,
                showTime
            )
        )
    }

    override fun onDismiss() {
        super.onDismiss()
        timer?.cancel()
    }
}