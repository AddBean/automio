// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.schedule

import android.content.Context
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import com.hive.script.R
import com.hive.script.views.beans.ScriptInfoModel
import com.hive.script.views.dialog.DialogScriptListSelector
import com.hive.script.views.widgets.BaseScriptDialog
import com.hive.timer.AlarmEntity
import com.hive.timer.AlarmTaskEntity
import com.hive.timer.widget.schedule.ScheduleTimerLayout
import com.hive.utils.GlobalApp
import com.hive.utils.utils.GsonHelper
import com.hive.views.widgets.CommonToast
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

/**
 *
 * @author jiadou
 * @email 172111432@qq.com
 * @date 7/19/21
 */
class DialogScriptScheduleTimer(context: Context?) : BaseScriptDialog(context) {

    private var scheduleView: ScheduleTimerLayout? = null
    private var tvTitle: TextView? = null
    private var ivClose: ImageView? = null

    override fun initWindow() {
        tvTitle = findViewById(R.id.tv_time_title)
        ivClose = findViewById(R.id.iv_close)
        scheduleView = findViewById(R.id.schedule_view)

        ivClose?.setOnClickListener {
            dismiss()
        }

        scheduleView?.onTaskSelectClickListener = {
            selectWorkflow { taskInfo ->
                scheduleView?.updateTaskInfo(taskInfo)
            }
        }
        scheduleView?.onScheduleSettingListener =
            object : ScheduleTimerLayout.OnScheduleSettingListener {
                override fun onScheduleClosed() {
                    dismiss()
                }

                override fun onScheduleConfirmed(
                    alarmEntity: AlarmEntity?,
                    confirmCall: () -> Unit
                ) {
                    if (isBeenEdited(alarmEntity)) {
                        val taskInfo = scheduleView?.getTaskInfo()
                        if (taskInfo.isNullOrBlank()) {
                            selectWorkflow { selectedTaskInfo ->
                                scheduleView?.updateTaskInfo(selectedTaskInfo)
                            }
                        } else {
                            alarmEntity?.taskInfo = taskInfo
                            dismiss()
                            confirmCall.invoke()
                        }
                    } else {
                        CommonToast.show(com.hive.i8n.R.string.sc_save_wrong_not_changed)
                    }
                }
            }
    }

    override fun getWindowContext() = context

    @OptIn(DelicateCoroutinesApi::class)
    private fun selectWorkflow(onSelected: (String) -> Unit) {
        DialogScriptListSelector(context, false)
            .setTitle(GlobalApp.getString(com.hive.i8n.R.string.sc_script_list_selector_title2))
            .setOnScriptSelectListener(object :
                DialogScriptListSelector.OnScriptSelectListener {
                override fun onSelected(dialog: DialogScriptListSelector, model: ScriptInfoModel) {
                    GlobalScope.launch(Dispatchers.Main) {
                        dialog.dismiss()
                        onSelected.invoke(
                            GsonHelper.getInstance().toJson(AlarmTaskEntity().apply {
                                scriptPath = model.scriptPath
                                scriptName = model.scriptName
                            })
                        )
                    }
                }

                override fun onDismissed() {
                }
            }).show()
    }

    private fun isBeenEdited(entity: AlarmEntity?): Boolean {
        val count = entity?.alarmList?.size ?: 0
        return if (count == 0) {
            false
        } else {
            (entity?.alarmList?.filter { it.type == entity.enableType }?.size ?: 0) > 0
        }

    }

    fun loadAlarmEntity(entity: AlarmEntity?) {
        scheduleView = scheduleView ?: findViewById(R.id.schedule_view)
        scheduleView?.loadData(entity)
    }

    override fun isTouchOutsideDismissed() = false

    override fun getWidthByOrientation(): Int {
        return FrameLayout.LayoutParams.MATCH_PARENT
    }

    override fun getHeightByOrientation(): Int {
        return FrameLayout.LayoutParams.MATCH_PARENT
    }

    override fun getMarginParams(): Array<Int> {
        return arrayOf(0, 100 * DP, 0, 0)
    }

    override fun getWindowLayoutId() = R.layout.schedule_view_alarm_timer

}
