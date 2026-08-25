// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.timer.widget.logger

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import com.hive.base.BaseLayout
import com.hive.timer.R
import com.hive.timer.card.TimerLogItemView
import com.hive.timer.db.AlarmDbService
import com.hive.utils.GlobalApp
import com.hive.views.list_view.ListRecyclerItemView
import com.hive.views.list_view.IListRecyclerViewFactory
import com.hive.views.list_view.ListRecyclerView
import com.hive.views.popmenu.PopMenuManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TimerLoggerView(context: Context?, attrs: AttributeSet?) : BaseLayout(context, attrs),
    ListRecyclerItemView.OnItemEventListener, IListRecyclerViewFactory {

    private var currentAlarmId: Long? = null

    private var listRecyclerView : ListRecyclerView? = null
    private var layoutFilter: View? = null
    private var layoutEmptyState: View? = null
    private var tvFilterName: TextView? = null


    override fun initView(p0: View?) {
        listRecyclerView = findViewById(R.id.listRecyclerView)
        layoutFilter = findViewById(R.id.layoutFilter)
        layoutEmptyState = findViewById(R.id.layoutEmptyState)
        tvFilterName = findViewById(R.id.tvFilterName)
        listRecyclerView?.layoutManager =
            LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        listRecyclerView?.setOnItemEventListener(this)
        listRecyclerView?.setItemViewFactory(this)
        layoutFilter?.setOnClickListener {
            val dataSets = GlobalApp.getStringArray(com.hive.i8n.R.array.timer_logger_level_name).toList()
            PopMenuManager.instance.showMenu(
                tvFilterName!!,
                dataSets,
                object : PopMenuManager.OnItemClickListener<String> {
                    override fun onItemClicked(view: View, data: String, pos: Int) {
                        loadLogs(currentAlarmId, pos)
                    }
                })
        }
    }

    fun loadLogs(alarmId: Long?, level: Int) {
        tvFilterName?.text = GlobalApp.getStringArray(com.hive.i8n.R.array.timer_logger_level_name)[level]
        if (alarmId != null) {
            currentAlarmId = alarmId
        }
        currentAlarmId ?: return
        GlobalScope.launch(Dispatchers.Main) {
            val logs = withContext(Dispatchers.IO) {
                AlarmDbService.listLog(level, alarmId)
            }
            val isEmpty = logs.isNullOrEmpty()
            layoutEmptyState?.visibility = if (isEmpty) View.VISIBLE else View.GONE
            listRecyclerView?.visibility = if (isEmpty) View.GONE else View.VISIBLE
            listRecyclerView?.submitDataSets(logs)
            listRecyclerView?.notifyDataSetChanged()
        }
    }

    override fun onItemEvent(itemData: Any?, eventData: Any?) {

    }

    override fun createItemView(viewType: Int) = TimerLogItemView(context)

    override fun getLayoutId() = R.layout.timer_logger_view

}
