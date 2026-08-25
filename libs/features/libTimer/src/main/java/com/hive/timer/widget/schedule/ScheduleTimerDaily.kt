// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.timer.widget.schedule

import android.content.Context
import android.view.View
import com.hive.base.BaseLayout
import com.hive.timer.AlarmEntity
import com.hive.timer.AlarmManagerWrapper
import com.hive.timer.R
import com.hive.timer.card.AlarmItemView
import com.hive.views.list_view.ListRecyclerItemView
import com.hive.views.list_view.IListRecyclerViewFactory
import java.util.Calendar

class ScheduleTimerDaily(context: Context?) : BaseLayout(context),
    IListRecyclerViewFactory, ScheduleTimerItemList.OnAddAlarmListener {

    private var alarmEntity: AlarmEntity? = null

    private var timerList: ScheduleTimerItemList? = null

    override fun initView(p0: View?) {
        timerList = findViewById(R.id.timerList)
        timerList?.onAlarmListener = this
    }

    override fun onAddAlarm(hour: Int, minus: Int, secs: Int) {
        val alarmTime = AlarmEntity.AlarmTime().apply {
            this.type = AlarmEntity.AlarmType.DAILY
            this.week = 0
            this.repeatInterval = (24 * 60 * 60 * 1000).toLong()
            this.triggerAtTime =
                getTodayStartTimeStamp() + (secs + minus * 60 + hour * 60 * 60) * 1000
        }
        alarmEntity?.alarmList = alarmEntity?.alarmList ?: mutableListOf()
        alarmEntity?.alarmList?.add(alarmTime)
        load(alarmEntity ?: return)
    }

    override fun onDeleteAlarm(alarm: AlarmEntity.AlarmTime) {
        alarmEntity?.alarmList?.remove(alarm)
    }

    fun load(alarmEntity: AlarmEntity) {
        this.alarmEntity = alarmEntity
        timerList?.loadSchedule(alarmEntity.alarmList?.filter { it.type == AlarmEntity.AlarmType.DAILY }
            ?: mutableListOf(), true)
    }

    fun getAlarmList() = timerList?.getAlarmList()

    override fun createItemView(viewType: Int): ListRecyclerItemView = AlarmItemView(context)

    override fun getLayoutId() = R.layout.schedule_timer_daily


    private fun getTodayStartTimeStamp(): Long {
        val calendar = Calendar.getInstance()
        calendar.firstDayOfWeek = AlarmManagerWrapper.First_Day_Of_Week
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

}
