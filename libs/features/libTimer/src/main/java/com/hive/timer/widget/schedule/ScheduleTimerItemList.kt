// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.timer.widget.schedule

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.hive.timer.AlarmEntity
import com.hive.timer.R
import com.hive.timer.card.AlarmAddView
import com.hive.timer.card.AlarmItemView
import com.hive.views.list_view.ListRecyclerItemView
import com.hive.views.list_view.IListRecyclerViewFactory
import com.hive.views.list_view.ListRecyclerView
import com.hive.views.view_pager.PagerLayout

class ScheduleTimerItemList(context: Context?, attrs: AttributeSet?) : PagerLayout(context, attrs),
    IListRecyclerViewFactory, ListRecyclerItemView.OnItemEventListener,
    ScheduleTimePickerDialog.OnTimeSelectedListener {

    var onAlarmListener: OnAddAlarmListener? = null

    private var alarmList: MutableList<AlarmEntity.AlarmTime>? = null

    private var isShowAdd = true

    private var listRecyclerView: ListRecyclerView? = null


    override fun initView(p0: View?) {
        listRecyclerView = findViewById(R.id.listRecyclerView)
        listRecyclerView?.layoutManager =
            LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        listRecyclerView?.setItemViewFactory(this)
        listRecyclerView?.setOnItemEventListener(this)
    }

    override fun onTimeSelected(hour: Int, minus: Int, secs: Int) {
        onAlarmListener?.onAddAlarm(hour, minus, secs)
    }

    override fun onTimeDismiss() {
    }

    override fun onItemEvent(itemData: Any?, eventData: Any?) {
        when (eventData) {
            "add" -> {
                val ctx = context ?: return
                val dialog = ScheduleTimePickerDialog(ctx)
                dialog.onTimeSelectedListener = this
                dialog.show()
            }

            "delete" -> {
                onAlarmListener?.onDeleteAlarm(itemData as AlarmEntity.AlarmTime)
                alarmList?.remove(itemData as AlarmEntity.AlarmTime)
                loadSchedule(alarmList, isShowAdd)
            }
        }
    }

    fun loadSchedule(alarmList: List<AlarmEntity.AlarmTime>?, showAdd: Boolean) {
        isShowAdd = showAdd
        this.alarmList = alarmList?.toMutableList()
        val list = alarmList?.map { android.util.Pair(1, it as Any?) }?.toMutableList()
        if (showAdd) {
            list?.add(android.util.Pair(0, null))
        }
        list ?: return
        listRecyclerView?.submitDataSetsWithType(
            list
        )
        listRecyclerView?.notifyDataSetChanged()
    }

    fun getAlarmList() = alarmList

    override fun createItemView(viewType: Int): ListRecyclerItemView =
        if (viewType == 1) AlarmItemView(context) else AlarmAddView(context)

    override fun getLayoutId() = R.layout.schedule_timer_list


    interface OnAddAlarmListener {
        fun onAddAlarm(hour: Int, minus: Int, secs: Int)

        fun onDeleteAlarm(alarm: AlarmEntity.AlarmTime)
    }
}
