// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.timer.card

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import com.hive.timer.AlarmEntity
import com.hive.timer.AlarmTaskEntity
import com.hive.timer.R
import com.hive.timer.db.AlarmLog
import com.hive.timer.widget.TimeIconView
import com.hive.timer.widget.schedule.ScheduleTimerListItem
import com.hive.utils.GlobalApp
import com.hive.utils.extends.visibleOrGone
import com.hive.utils.utils.GsonHelper
import com.hive.utils.utils.StringUtils
import com.hive.utils.utils.TimeUtils
import com.hive.views.list_view.ListRecyclerItemView
import com.hive.views.widgets.UIRoundCornerLinearLayout
import java.util.Date

class TimerItemView(context: Context) : ListRecyclerItemView(context), View.OnClickListener {

    private val layout =
        LayoutInflater.from(context).inflate(R.layout.timer_item_view, this).apply {
            setOnClickListener {
                postEvent("click", dataItem)
            }
            setOnLongClickListener {
                postEvent("longClick", dataItem)
                true
            }
        }

    private val cardRoot = layout.findViewById<UIRoundCornerLinearLayout>(R.id.cardRoot)
    private val dividerFooter = layout.findViewById<View>(R.id.dividerFooter)
    private val ivStatus = layout.findViewById<Switch>(R.id.ivStatus)
    private val ivDelete = layout.findViewById<View>(R.id.ivDelete)
    private val ivLog = layout.findViewById<View>(R.id.ivLog)
    private val tvName = layout.findViewById<TextView>(R.id.tvName)
    private val tvSchedule = layout.findViewById<TextView>(R.id.tvSchedule)
    private val tvNextRunLabel = layout.findViewById<TextView>(R.id.tvNextRunLabel)
    private val tvNextRun = layout.findViewById<TextView>(R.id.tvNextRun)
    private val layoutFooter = layout.findViewById<LinearLayout>(R.id.layoutFooter)
    private val tvLastRun = layout.findViewById<TextView>(R.id.tvLastRun)
    private val tvLastStatus = layout.findViewById<TextView>(R.id.tvLastStatus)
    private val tvTime = layout.findViewById<TimeIconView>(R.id.timeIcon)

    private var dataItem: AlarmEntity? = null
    private var latestLog: AlarmLog? = null

    override fun bindData(data: Any?) {
        val item = data as ScheduleTimerListItem
        dataItem = item.alarm
        latestLog = item.latestLog
        val enabled = dataItem?.enable == true
        ivStatus.setOnCheckedChangeListener(null)
        ivStatus.isChecked = enabled
        ivStatus.setOnCheckedChangeListener { _, isChecked ->
            dataItem?.enable = isChecked
            updateUi()
            postEvent("update", dataItem)
        }
        ivDelete.setOnClickListener(this)
        ivLog.setOnClickListener(this)
        val taskInfo =
            GsonHelper.getInstance().fromJson(dataItem?.taskInfo, AlarmTaskEntity::class.java)
        val nextAlarm = dataItem?.findNextLastAlarm()
        val nextDate = nextAlarm?.let { Date(it) }
        tvName.text = taskInfo?.scriptName
        tvSchedule.text = dataItem?.enableType?.toString()
        tvNextRun.text = nextDate?.let { StringUtils.dateFormat(it, "MM.dd HH:mm") }
            ?: GlobalApp.getString(com.hive.i8n.R.string.timer_schedule_next_run_none)
        tvTime.setTime(Date(nextAlarm ?: 0L), false)
        updateUi()
    }

    override fun onClick(view: View?) {
        when (view?.id) {
            R.id.ivDelete -> {
                postEvent("delete", dataItem)
            }

            R.id.ivLog -> {
                postEvent("log", dataItem)
            }
        }
    }

    private fun updateUi() {
        val enabled = dataItem?.enable == true
        val primaryTextColor = GlobalApp.getColor(com.hive.i8n.R.color.textColorPrimary)
        val tertiaryTextColor = GlobalApp.getColor(com.hive.i8n.R.color.colorTextSecondary)
        val inactiveTitleColor = tertiaryTextColor

        cardRoot.alpha = if (enabled) 1.0f else 0.6f
        tvTime.visibleOrGone(enabled)
        tvNextRunLabel.visibleOrGone(enabled)
        tvNextRun.visibleOrGone(enabled)

        tvName.setTextColor(
            if (enabled) primaryTextColor else inactiveTitleColor
        )
        ivLog.background = null
        ivDelete.background = null

        val log = latestLog
        val hasLog = log?.logTime != null
        dividerFooter.visibleOrGone(hasLog)
        layoutFooter.visibleOrGone(hasLog)
        if (!hasLog) return

        tvLastRun.text = GlobalApp.getString(
            com.hive.i8n.R.string.timer_schedule_last_run_format,
            TimeUtils.getFriendlyTimeSpanByNow(log.logTime ?: 0L)
        )
        val status = when (log.logLevel) {
            2 -> TimerLogStatus(
                com.hive.i8n.R.string.timer_schedule_status_failed,
                com.hive.i8n.R.color.colorRed,
                0x1AFF0000 // Dark red tint bg
            )

            1 -> TimerLogStatus(
                com.hive.i8n.R.string.timer_schedule_status_warning,
                com.hive.i8n.R.color.colorYellow,
                0x1AFFA500 // Dark yellow tint bg
            )

            else -> TimerLogStatus(
                com.hive.i8n.R.string.timer_schedule_status_success,
                com.hive.i8n.R.color.colorGreen,
                0x1A00FF00 // Dark green tint bg
            )
        }
        tvLastStatus.text = GlobalApp.getString(status.textRes)
        tvLastStatus.setTextColor(GlobalApp.getColor(status.textColorRes))
        tvLastStatus.setBackgroundColor(status.backgroundArgb)
    }

    private data class TimerLogStatus(
        val textRes: Int,
        val textColorRes: Int,
        val backgroundArgb: Int
    )
}
