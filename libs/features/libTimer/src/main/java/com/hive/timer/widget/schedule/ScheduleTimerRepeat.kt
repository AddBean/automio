// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.timer.widget.schedule

import android.graphics.Rect
import android.content.Context
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hive.base.BaseLayout
import com.hive.timer.AlarmEntity
import com.hive.timer.AlarmManagerWrapper
import com.hive.timer.R
import com.hive.timer.card.AlarmRepeatItemView
import com.hive.utils.utils.DensityUtil
import com.hive.views.list_view.ListRecyclerItemView
import com.hive.views.list_view.IListRecyclerViewFactory
import com.hive.views.list_view.ListRecyclerView
import com.hive.views.widgets.NumberOptView
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date

class ScheduleTimerRepeat(context: Context?) : BaseLayout(context),
    IListRecyclerViewFactory {

    private var alarmEntity: AlarmEntity? = null

    private var alarm: AlarmEntity.AlarmTime? = null

    private var numberValue: NumberOptView? = null

    private var listRecyclerView: ListRecyclerView? = null

    override fun initView(p0: View?) {
        numberValue = findViewById(R.id.numberValue)
        listRecyclerView = findViewById(R.id.listRecyclerView)
        val hSpacing = DensityUtil.dip2px(8f)
        val vSpacing = DensityUtil.dip2px(4f)
        listRecyclerView?.layoutManager = GridLayoutManager(context, 2)
        listRecyclerView?.addItemDecoration(object : RecyclerView.ItemDecoration() {
            override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
                val position = parent.getChildAdapterPosition(view)
                val column = position % 2
                outRect.left = hSpacing - column * hSpacing / 2
                outRect.right = (column + 1) * hSpacing / 2
                outRect.top = if (position >= 2) vSpacing else 0
                outRect.bottom = vSpacing
            }
        })
        listRecyclerView?.setItemViewFactory(this)
        numberValue?.onValueChangedListener = object : NumberOptView.OnValueChangedListener {
            override fun onValueChanged(value: Int) {
                val repeatInterval = (value).toLong()
                alarm?.repeatInterval = repeatInterval * 60 * 1000
                load(alarmEntity, false)
            }
        }
    }

    fun load(alarmEntity: AlarmEntity?, updateValueView: Boolean = true) {
        this.alarmEntity = alarmEntity
        if (alarm == null) {
            alarm = alarmEntity?.alarmList?.filter { it.type == AlarmEntity.AlarmType.REPEAT }
                ?.firstOrNull() ?: AlarmEntity.AlarmTime().apply {
                this.week = 0
                this.type = AlarmEntity.AlarmType.REPEAT
                this.enable = true
                this.repeatInterval = (30 * 60 * 1000).toLong()
                this.triggerAtTime = getTodayStartTimeStamp()
            }
        }
        val itemList = mutableListOf<Long>()
        val calendar = getTodayCalendar()
        val step = (alarm?.repeatInterval ?: (60 * 1000)) / (60 * 1000)
        if (updateValueView) {
            numberValue?.number = step.toInt()
            numberValue?.updateUiStatus()
        }
        for (time in 0..(24 * 60L) step step) {
            itemList.add(calendar.timeInMillis)
            calendar.add(Calendar.MILLISECOND, (step * 60 * 1000).toInt())
        }
        listRecyclerView?.submitDataSets(itemList)
        listRecyclerView?.notifyDataSetChanged()
    }

    fun getAlarmList(): MutableList<AlarmEntity.AlarmTime>? {
        alarm ?: return null
        return mutableListOf(alarm!!)
    }

    private fun dateFormat(date: Date?, format: String?): String {
        return SimpleDateFormat(format).format(date)
    }

    private val calendar = Calendar.getInstance()

    private fun getTodayStartTimeStamp(): Long {
        calendar.firstDayOfWeek = AlarmManagerWrapper.First_Day_Of_Week
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    private fun getTodayCalendar(): Calendar {
        val calendar = Calendar.getInstance()
        calendar.firstDayOfWeek = AlarmManagerWrapper.First_Day_Of_Week
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar
    }

    override fun createItemView(viewType: Int): ListRecyclerItemView = AlarmRepeatItemView(context)

    override fun getLayoutId() = R.layout.schedule_timer_repeat

}
