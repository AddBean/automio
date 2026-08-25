// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.timer.widget.schedule

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import com.hive.base.BaseLayout
import com.hive.timer.AlarmEntity
import com.hive.timer.AlarmTaskEntity
import com.hive.timer.R
import com.hive.timer.db.AlarmDbService
import com.hive.timer.event.RefreshTimerEvent
import com.hive.timer.utils.CalendarUtils
import com.hive.timer.utils.TimerIdGenerator
import com.hive.utils.GlobalApp
import com.hive.utils.utils.GsonHelper
import org.greenrobot.eventbus.EventBus

class ScheduleTimerLayout(context: Context?, attrs: AttributeSet?) :
    BaseLayout(context, attrs), CalendarUtils.CallBack {
    var onScheduleSettingListener: OnScheduleSettingListener? = null
    var onTaskSelectClickListener: (() -> Unit)? = null

    private var iv_close: View? = null
    private var iv_ok: View? = null
    private var layoutTaskSelect: View? = null
    private var layout_type_content: ViewGroup? = null
    private var tvTaskName: TextView? = null
    private var tvTaskDesc: TextView? = null
    private var tvTypeHelper: TextView? = null
    private var typeDaily: TextView? = null
    private var typeWeek: TextView? = null
    private var typeRepeat: TextView? = null

    private var currentSelectType = 0

    private var scheduleTimerDaily = ScheduleTimerDaily(context)

    private var scheduleTimerRepeat = ScheduleTimerRepeat(context)

    private var scheduleTimerWeek = ScheduleTimerWeekList(context)

    private var alarmEntity: AlarmEntity? = null

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        CalendarUtils.unregister(this)
    }

    override fun onResult(success: Boolean) {
    }

    override fun initView(view: View?) {

        CalendarUtils.register(this)

        iv_close = findViewById(R.id.iv_close)
        iv_ok = findViewById(R.id.iv_ok)
        layoutTaskSelect = findViewById(R.id.layout_task_select)
        layout_type_content = findViewById(R.id.layout_type_content)
        tvTaskName = findViewById(R.id.tv_task_name)
        tvTaskDesc = findViewById(R.id.tv_task_desc)
        tvTypeHelper = findViewById(R.id.tv_type_helper)
        typeDaily = findViewById(R.id.tv_type_daily)
        typeWeek = findViewById(R.id.tv_type_week)
        typeRepeat = findViewById(R.id.tv_type_repeat)

        iv_close?.setOnClickListener {
            onScheduleSettingListener?.onScheduleClosed()
        }

        iv_ok?.setOnClickListener {
            save()
        }

        layoutTaskSelect?.setOnClickListener {
            onTaskSelectClickListener?.invoke()
        }

        typeDaily?.setOnClickListener {
            selectType(0)
        }

        typeWeek?.setOnClickListener {
            selectType(1)
        }

        typeRepeat?.setOnClickListener {
            selectType(2)
        }

    }

    fun loadData(alarmEntity: AlarmEntity?) {
        this.alarmEntity = alarmEntity?.deepCopy() ?: AlarmEntity()
        currentSelectType = this.alarmEntity?.enableType?.toInt() ?: 0
        post {
            updateUi()
        }
    }

    fun save() {
        // Validate workflow binding
        if (alarmEntity?.taskInfo.isNullOrBlank()) {
            Toast.makeText(context, GlobalApp.getString(com.hive.i8n.R.string.timer_schedule_editor_task_required), Toast.LENGTH_SHORT).show()
            return
        }
        fillWithAlarmEntity(alarmEntity)
        val confirmCall = {
            AlarmDbService.saveAlarmEntity(alarmEntity)
            EventBus.getDefault().post(RefreshTimerEvent())
        }
        onScheduleSettingListener?.onScheduleConfirmed(alarmEntity, confirmCall)
    }

    private fun fillWithAlarmEntity(alarmEntity: AlarmEntity?) {
        alarmEntity?.alarmId = alarmEntity?.alarmId ?: TimerIdGenerator.newId()
        alarmEntity?.enableType = when (currentSelectType) {
            0 -> AlarmEntity.AlarmType.DAILY
            1 -> AlarmEntity.AlarmType.WEEKLY
            2 -> AlarmEntity.AlarmType.REPEAT
            else -> AlarmEntity.AlarmType.DAILY
        }
        alarmEntity?.alarmList = mutableListOf()
        alarmEntity?.alarmList?.addAll(scheduleTimerDaily.getAlarmList() ?: mutableListOf())
        alarmEntity?.alarmList?.addAll(scheduleTimerWeek.getAlarmList() ?: mutableListOf())
        alarmEntity?.alarmList?.addAll(scheduleTimerRepeat.getAlarmList() ?: mutableListOf())
    }

    fun updateTaskInfo(taskInfo: String?) {
        alarmEntity = alarmEntity ?: AlarmEntity()
        alarmEntity?.taskInfo = taskInfo
        updateTaskInfoUi()
    }

    fun getTaskInfo(): String? {
        return alarmEntity?.taskInfo
    }

    private fun selectType(type: Int) {
        currentSelectType = type
        alarmEntity?.enableType = when (currentSelectType) {
            0 -> AlarmEntity.AlarmType.DAILY
            1 -> AlarmEntity.AlarmType.WEEKLY
            2 -> AlarmEntity.AlarmType.REPEAT
            else -> AlarmEntity.AlarmType.DAILY
        }
        updateUi()
    }

    private fun updateTaskInfoUi() {
        val taskInfo = GsonHelper.getInstance()
            .fromJson(alarmEntity?.taskInfo, AlarmTaskEntity::class.java)
        val taskName = taskInfo?.scriptName?.takeIf { it.isNotBlank() }
        tvTaskName?.text = taskName
            ?: GlobalApp.getString(com.hive.i8n.R.string.timer_schedule_editor_task_placeholder)
        tvTaskDesc?.text = if (taskName.isNullOrBlank()) {
            GlobalApp.getString(com.hive.i8n.R.string.timer_schedule_editor_task_desc)
        } else {
            taskInfo?.scriptPath?.takeIf { it.isNotBlank() }
                ?: GlobalApp.getString(com.hive.i8n.R.string.timer_schedule_editor_task_ready)
        }
    }

    private fun updateTypeUi() {
        val typeList = listOf(typeDaily, typeWeek, typeRepeat)
        typeList.forEachIndexed { index, textView ->
            textView?.isSelected = index == currentSelectType
            if (index == currentSelectType) {
                textView?.setTextColor(GlobalApp.getColor(com.hive.i8n.R.color.color_white))
                textView?.setBackgroundColor(GlobalApp.getColor(com.hive.i8n.R.color.design_accent_indigo))
            } else {
                textView?.setTextColor(GlobalApp.getColor(com.hive.i8n.R.color.colorTextSecondary))
                textView?.setBackgroundResource(android.R.color.transparent)
            }
            textView?.text = GlobalApp.getResources()
                .getStringArray(com.hive.i8n.R.array.schedule_menu_repeat_array)[index]
        }
        tvTypeHelper?.text = when (currentSelectType) {
            0 -> GlobalApp.getString(com.hive.i8n.R.string.timer_schedule_editor_type_daily_hint)
            1 -> GlobalApp.getString(com.hive.i8n.R.string.timer_schedule_editor_type_week_hint)
            2 -> GlobalApp.getString(com.hive.i8n.R.string.timer_schedule_editor_type_repeat_hint)
            else -> ""
        }
    }

    private fun updateUi() {
        updateTaskInfoUi()
        updateTypeUi()
        alarmEntity?.run {
            scheduleTimerDaily.load(this)
            scheduleTimerWeek.load(this)
            scheduleTimerRepeat.load(this, true)
        }
        when (currentSelectType) {
            0 -> {
                layout_type_content?.removeAllViews()
                layout_type_content?.addView(scheduleTimerDaily)
            }

            1 -> {
                layout_type_content?.removeAllViews()
                layout_type_content?.addView(scheduleTimerWeek)
            }

            2 -> {
                layout_type_content?.removeAllViews()
                layout_type_content?.addView(scheduleTimerRepeat)
            }
        }
    }

    override fun getLayoutId() = R.layout.schedule_time_setting_view

    interface OnScheduleSettingListener {

        fun onScheduleClosed()

        fun onScheduleConfirmed(alarmEntity: AlarmEntity?, confirmCall: () -> Unit)
    }


}
