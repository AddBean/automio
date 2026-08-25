// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.timer

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.hive.base.BaseFragmentActivity
import com.hive.timer.db.AlarmDbService
import com.hive.timer.utils.TimerLogger
import com.hive.timer.widget.logger.TimerLoggerView
import com.hive.utils.GlobalApp
import com.hive.utils.utils.IntentUtils
import com.hive.views.SampleDialog
import com.carlos.ui.header.CommonHeader

class ActivityScheduleRecord : BaseFragmentActivity() {

    private var alarmId: Long = -1
    private var loggerView: TimerLoggerView? = null
    override fun doOnCreate(p0: Bundle?) {
        findViewById<CommonHeader>(R.id.header)?.apply {
            setLeftClickListener { finish() }
            setRightClickListener { showClearDialog() }
        }
        loggerView = findViewById(R.id.loggerView)
        alarmId = intent.getLongExtra("alarmId", -1)
        loggerView?.loadLogs(alarmId, TimerLogger.LogLevelInfo)
    }

    private fun showClearDialog() {
        val dialog = SampleDialog(this)
        dialog.setDialogTitle(GlobalApp.getString(com.hive.i8n.R.string.timer_clear_log_title))
        dialog.setDialogContent(GlobalApp.getString(com.hive.i8n.R.string.timer_clear_log_msg))
        dialog.setLeftText(GlobalApp.getString(com.hive.i8n.R.string.cancel))
        dialog.setRightText(GlobalApp.getString(com.hive.i8n.R.string.ok))
        dialog.setOnDialogListener { isRight ->
            dialog.dismiss()
            if (isRight) {
                AlarmDbService.clearLog(alarmId)
                loggerView?.loadLogs(alarmId, TimerLogger.LogLevelInfo)
            }
        }
        dialog.show()
    }


    override fun getLayoutId() = R.layout.activity_schedule_record

    override fun isSupportStatusBarCompat(): Boolean {
        return false
    }

    companion object {
        fun start(context: Context, alarmId: Long) {
            IntentUtils.safeStartActivity(
                context,
                Intent(context, ActivityScheduleRecord::class.java).apply {
                    putExtra("alarmId", alarmId)
                }
            )
        }
    }
}
