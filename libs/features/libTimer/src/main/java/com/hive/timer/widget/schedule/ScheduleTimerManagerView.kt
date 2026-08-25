// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.timer.widget.schedule

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import com.hive.base.BaseLayout
import com.hive.timer.ActivityScheduleRecord
import com.hive.timer.AlarmEntity
import com.hive.timer.R
import com.hive.timer.card.TimerItemView
import com.hive.timer.db.AlarmDbService
import com.hive.timer.event.EditTimerEvent
import com.hive.timer.event.RefreshTimerEvent
import com.hive.timer.event.UpdateTimerEvent
import com.hive.timer.utils.TimerLogger
import com.hive.utils.GlobalApp
import com.hive.utils.utils.GsonHelper
import com.hive.views.SampleDialog
import com.hive.views.list_view.ListRecyclerItemView
import com.hive.views.list_view.IListRecyclerViewFactory
import com.hive.views.list_view.ListRecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe

class ScheduleTimerManagerView(context: Context?, attrs: AttributeSet?) :
    BaseLayout(context, attrs),
    IListRecyclerViewFactory, ListRecyclerItemView.OnItemEventListener {

    private var listRecyclerView: ListRecyclerView? = null
    private var layoutEmptyState: View? = null
    private var tvEnabledCount: TextView? = null

    override fun initView(p0: View?) {
        listRecyclerView = findViewById(R.id.listRecyclerView)
        layoutEmptyState = findViewById(R.id.layoutEmptyState)
        tvEnabledCount = findViewById(R.id.tvEnabledCount)
        listRecyclerView?.layoutManager =
            LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        listRecyclerView?.setItemViewFactory(this)
        listRecyclerView?.setOnItemEventListener(this)
        refreshList()
    }

    fun refreshList() {
        GlobalScope.launch(Dispatchers.Main) {
            val list = withContext(Dispatchers.IO) {
                val records = AlarmDbService.list() ?: mutableListOf()
                records.map { record ->
                    val alarm = GsonHelper.getInstance().fromJson(record.alarmJson, AlarmEntity::class.java)
                    ScheduleTimerListItem(
                        alarm = alarm,
                        latestLog = AlarmDbService.listLog(
                            TimerLogger.LogLevelInfo,
                            record.alarmId
                        )?.firstOrNull()
                    )
                }
            }
            val enabledCount = list.count { it.alarm.enable }
            tvEnabledCount?.text = "$enabledCount / ${list.size}"
            layoutEmptyState?.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
            listRecyclerView?.visibility = if (list.isEmpty()) View.GONE else View.VISIBLE
            listRecyclerView?.submitDataSets(list)
            listRecyclerView?.notifyDataSetChanged()
        }
    }

    override fun onItemEvent(itemData: Any?, eventData: Any?) {
        val data = itemData as AlarmEntity?
        when (eventData) {
            "update" -> {
                AlarmDbService.saveAlarmEntity(data)
                refreshList()
                EventBus.getDefault().post(UpdateTimerEvent(data))
            }


            "click" -> {
                EventBus.getDefault().post(EditTimerEvent(data))
            }

            "delete", "longClick" -> {
                data?.run {
                    val dialog = SampleDialog(context);
                    dialog.setDialogTitle(GlobalApp.getString(com.hive.i8n.R.string.timer_schedule_timer_delete_title))
                        .setDialogContent(GlobalApp.getString(com.hive.i8n.R.string.timer_schedule_timer_delete))
                        .setOnDialogListener { isRight ->
                            dialog.dismiss()
                            if (isRight) {
                                AlarmDbService.delete(data.alarmId!!)
                                refreshList()
                            }
                        }
                        .show()
                }
            }

            "log" -> {
                ActivityScheduleRecord.start(context, data?.alarmId ?: -1)
            }
        }

    }

    override fun createItemView(viewType: Int): ListRecyclerItemView = TimerItemView(context)

    override fun getLayoutId() = R.layout.schedule_timer_manager


    @Subscribe
    fun onRefreshTimerEvent(e: RefreshTimerEvent) {
        refreshList()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        EventBus.getDefault().register(this)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        EventBus.getDefault().unregister(this)
    }

}
