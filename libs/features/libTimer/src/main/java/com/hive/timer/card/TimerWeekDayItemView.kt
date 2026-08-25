// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.timer.card

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import com.hive.timer.AlarmEntity
import com.hive.timer.R
import com.hive.timer.widget.schedule.ScheduleTimerWeekDayItem
import com.hive.timer.widget.schedule.ScheduleTimerWeekList
import com.hive.utils.extends.visibleOrGone
import com.hive.utils.utils.StringUtils
import com.hive.views.list_view.ListRecyclerItemView
import com.hive.views.widgets.FlowLayout
import java.util.Date

class TimerWeekDayItemView(context: Context) : ListRecyclerItemView(context) {

    private val layout = LayoutInflater.from(context).inflate(R.layout.timer_week_day_item_view, this)
    private val tvDay = layout.findViewById<TextView>(R.id.tvDay)
    private val ivAdd = layout.findViewById<AppCompatImageView>(R.id.ivAdd)
    private val flowTimes = layout.findViewById<FlowLayout>(R.id.flowTimes)
    private val layoutEmpty = layout.findViewById<View>(R.id.layoutEmpty)

    init {
        val density = resources.displayMetrics.density
        flowTimes.setHorizontalSpacing((6 * density).toInt())
        flowTimes.setVerticalSpacing((6 * density).toInt())
    }

    override fun bindData(data: Any?) {
        val item = data as ScheduleTimerWeekDayItem
        tvDay.text = item.dayName

        ivAdd.setOnClickListener {
            postEvent(ScheduleTimerWeekList.WeekListEvent.Add(item.dayIndex))
        }

        flowTimes.removeAllViews()
        val hasTimes = item.times.isNotEmpty()
        layoutEmpty.visibleOrGone(!hasTimes)
        flowTimes.visibleOrGone(hasTimes)

        if (!hasTimes) return

        item.times.forEach { alarm ->
            val chip = LayoutInflater.from(context).inflate(R.layout.timer_week_time_chip, flowTimes, false)
            val tvTime = chip.findViewById<TextView>(R.id.tvTime)
            val ivRemove = chip.findViewById<View>(R.id.ivRemove)
            tvTime.text = StringUtils.dateFormat(Date(alarm.triggerAtTime), "HH:mm")
            ivRemove.setOnClickListener {
                postEvent(ScheduleTimerWeekList.WeekListEvent.Delete(alarm))
            }
            flowTimes.addView(chip)
        }
    }
}

