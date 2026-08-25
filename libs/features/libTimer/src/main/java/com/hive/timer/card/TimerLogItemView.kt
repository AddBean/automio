// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.timer.card

import android.content.Context
import android.view.LayoutInflater
import android.widget.TextView
import com.hive.timer.R
import com.hive.timer.db.AlarmLog
import com.hive.utils.GlobalApp
import com.hive.utils.utils.StringUtils
import com.hive.views.list_view.ListRecyclerItemView
import java.util.Date

class TimerLogItemView(context: Context) : ListRecyclerItemView(context) {

    private var logData: AlarmLog? = null

    private val layout =
        LayoutInflater.from(context).inflate(R.layout.timer_log_item_view, this)

    private val tvTime = layout.findViewById<TextView>(R.id.tvTime)
    private val tvMsg = layout.findViewById<TextView>(R.id.tvMsg)
    private val tvLevel = layout.findViewById<TextView>(R.id.tvLevel)
    override fun bindData(data: Any?) {
        logData = data as AlarmLog?
        logData?.run {
            val style = when (logLevel) {
                1 -> LogStyle(
                    text = GlobalApp.getString(com.hive.i8n.R.string.timer_schedule_status_warning),
                    textColor = GlobalApp.getColor(com.hive.i8n.R.color.colorYellow),
                    backgroundRes = R.drawable.xml_timer_schedule_status_warning,
                    messageColor = GlobalApp.getColor(com.hive.i8n.R.color.colorYellow)
                )

                2 -> LogStyle(
                    text = GlobalApp.getString(com.hive.i8n.R.string.timer_schedule_status_failed),
                    textColor = GlobalApp.getColor(com.hive.i8n.R.color.colorRed),
                    backgroundRes = R.drawable.xml_timer_schedule_status_failed,
                    messageColor = GlobalApp.getColor(com.hive.i8n.R.color.colorRed)
                )

                else -> LogStyle(
                    text = GlobalApp.getString(com.hive.i8n.R.string.timer_schedule_status_success),
                    textColor = GlobalApp.getColor(com.hive.i8n.R.color.colorGreen),
                    backgroundRes = R.drawable.xml_timer_schedule_status_success,
                    messageColor = GlobalApp.getColor(com.hive.i8n.R.color.textColorPrimary)
                )
            }
            tvTime?.text = StringUtils.dateFormat(Date(logTime))
            tvMsg?.text = logTag
            tvMsg.setTextColor(style.messageColor)
            tvLevel.text = style.text
            tvLevel.setTextColor(style.textColor)
            tvLevel.setBackgroundResource(style.backgroundRes)
        }
    }

    private data class LogStyle(
        val text: String,
        val textColor: Int,
        val backgroundRes: Int,
        val messageColor: Int
    )
}
