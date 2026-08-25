// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.timer

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.hive.base.BaseFragmentActivity
import com.hive.plugin.ComponentManager
import com.hive.plugin.provider.IScriptProvider
import com.hive.timer.widget.schedule.ScheduleTimerLayout
import com.hive.utils.GlobalApp
import com.hive.utils.utils.GsonHelper
import com.carlos.ui.header.CommonHeader

/**
 * 定时任务编辑页面（全屏）
 * 对齐 script-design 的 AddScheduleOverlay
 */
class ActivityScheduleTimerEditor : BaseFragmentActivity() {

    private var scheduleView: ScheduleTimerLayout? = null
    private var headerView: CommonHeader? = null

    private var alarmEntity: AlarmEntity? = null

    override fun doOnCreate(savedState: Bundle?) {
        val alarmJson = intent.getStringExtra(EXTRA_ALARM_ENTITY)
        alarmEntity = if (alarmJson.isNullOrBlank()) {
            AlarmEntity()
        } else {
            GsonHelper.getInstance().fromJson(alarmJson, AlarmEntity::class.java)
        }

        scheduleView = findViewById(R.id.scheduleView)
        headerView = findViewById(R.id.header_view)

        scheduleView?.loadData(alarmEntity)

        headerView?.setRightClickListener {
            scheduleView?.save()
        }

        scheduleView?.onTaskSelectClickListener = {
            showWorkflowSelector { taskInfo ->
                scheduleView?.updateTaskInfo(taskInfo)
            }
        }

        scheduleView?.onScheduleSettingListener = object : ScheduleTimerLayout.OnScheduleSettingListener {
            override fun onScheduleClosed() {
                finish()
            }

            override fun onScheduleConfirmed(entity: AlarmEntity?, confirmCall: () -> Unit) {
                confirmCall.invoke()
                finish()
            }
        }
    }

    private fun showWorkflowSelector(onSelected: (taskInfoJson: String) -> Unit) {
        val scriptProvider = ComponentManager.getInstance()
            .getProvider(IScriptProvider::class.java) as? IScriptProvider ?: return

        scriptProvider.showWorkflowSelector(
            this,
            GlobalApp.getString(com.hive.i8n.R.string.timer_schedule_editor_task_title),
            object : IScriptProvider.OnWorkflowSelectedCallback {
                override fun onWorkflowSelected(scriptPath: String, scriptName: String) {
                    onSelected.invoke(
                        GsonHelper.getInstance().toJson(AlarmTaskEntity().apply {
                            this.scriptPath = scriptPath
                            this.scriptName = scriptName
                        })
                    )
                }

                override fun onDismissed() {
                }
            }
        )
    }

    override fun getLayoutId() = R.layout.activity_schedule_timer_editor

    companion object {
        const val EXTRA_ALARM_ENTITY = "extra_alarm_entity"

        fun start(context: Context, alarmEntity: AlarmEntity? = null) {
            val intent = Intent(context, ActivityScheduleTimerEditor::class.java)
            alarmEntity?.let {
                intent.putExtra(EXTRA_ALARM_ENTITY, GsonHelper.getInstance().toJson(it))
            }
            context.startActivity(intent)
        }
    }
}
