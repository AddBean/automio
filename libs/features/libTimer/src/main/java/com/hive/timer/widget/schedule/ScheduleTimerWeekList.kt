// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.timer.widget.schedule

import android.content.Context
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.hive.base.BaseLayout
import com.hive.timer.AlarmEntity
import com.hive.timer.AlarmManagerWrapper
import com.hive.timer.R
import com.hive.timer.card.TimerWeekDayItemView
import com.hive.views.list_view.IListRecyclerViewFactory
import com.hive.views.list_view.ListRecyclerItemView
import com.hive.views.list_view.ListRecyclerView
import java.util.Calendar

class ScheduleTimerWeekList(context: Context?) :
    BaseLayout(context),
    IListRecyclerViewFactory,
    ListRecyclerItemView.OnItemEventListener,
    ScheduleTimePickerDialog.OnTimeSelectedListener {

    private var alarmEntity: AlarmEntity? = null

    private var listRecyclerView: ListRecyclerView? = null
    private var pendingDayIndex: Int? = null

    override fun initView(p0: View?) {
        listRecyclerView = findViewById(R.id.listRecyclerView)
        listRecyclerView?.layoutManager =
            LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        listRecyclerView?.setItemViewFactory(this)
        listRecyclerView?.setOnItemEventListener(this)
    }

    fun load(alarmEntity: AlarmEntity) {
        this.alarmEntity = alarmEntity
        val dayNames = context?.resources?.getStringArray(com.hive.i8n.R.array.week_list_short)
            ?: emptyArray()
        val list = (0..6).map { dayIndex ->
            val name = dayNames.getOrNull(dayIndex) ?: ""
            val times = alarmEntity.alarmList
                ?.filter { it.type == AlarmEntity.AlarmType.WEEKLY && it.week == dayIndex }
                ?.sortedBy { it.triggerAtTime }
                ?: emptyList()
            ScheduleTimerWeekDayItem(dayIndex = dayIndex, dayName = name, times = times)
        }
        listRecyclerView?.submitDataSets(list)
        listRecyclerView?.notifyDataSetChanged()
    }

    fun getAlarmList(): MutableList<AlarmEntity.AlarmTime>? {
        val entity = alarmEntity ?: return null
        return entity.alarmList?.filter { it.type == AlarmEntity.AlarmType.WEEKLY }?.toMutableList()
    }

    override fun onItemEvent(itemData: Any?, eventData: Any?) {
        when (eventData) {
            is WeekListEvent.Add -> {
                pendingDayIndex = eventData.dayIndex
                val ctx = context ?: return
                val dialog = ScheduleTimePickerDialog(ctx)
                dialog.onTimeSelectedListener = this
                dialog.show()
            }

            is WeekListEvent.Delete -> {
                alarmEntity?.alarmList?.remove(eventData.alarm)
                alarmEntity?.let { load(it) }
            }
        }
    }

    override fun onTimeSelected(hour: Int, minus: Int, secs: Int) {
        val dayIndex = pendingDayIndex ?: return
        val entity = alarmEntity ?: return
        val alarmTime = AlarmEntity.AlarmTime().apply {
            this.type = AlarmEntity.AlarmType.WEEKLY
            this.week = dayIndex
            this.repeatInterval = (7 * 24 * 60 * 60 * 1000).toLong()
            this.enable = true
            this.triggerAtTime =
                getWeekStartTimeStamp(dayIndex) + (secs + minus * 60 + hour * 60 * 60) * 1000
        }
        entity.alarmList = entity.alarmList ?: mutableListOf()
        entity.alarmList?.add(alarmTime)
        load(entity)
    }

    override fun onTimeDismiss() {
    }

    override fun createItemView(viewType: Int): ListRecyclerItemView = TimerWeekDayItemView(context)

    override fun getLayoutId(): Int = R.layout.schedule_timer_week_list

    private fun getWeekStartTimeStamp(week: Int): Long {
        val calendar = Calendar.getInstance()
        calendar.firstDayOfWeek = AlarmManagerWrapper.First_Day_Of_Week
        calendar.set(Calendar.DAY_OF_WEEK, AlarmManagerWrapper.First_Day_Of_Week)
        calendar.add(Calendar.DAY_OF_WEEK, week)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    sealed class WeekListEvent {
        data class Add(val dayIndex: Int) : WeekListEvent()
        data class Delete(val alarm: AlarmEntity.AlarmTime) : WeekListEvent()
    }
}
