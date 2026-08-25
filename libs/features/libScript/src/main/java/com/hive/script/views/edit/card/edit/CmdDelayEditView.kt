// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.edit.card.edit

import android.content.Context
import android.view.View
import com.hive.script.R
import com.hive.script.base.ScriptCommand
import com.hive.script.cmd.CmdDelay
import com.hive.script.views.widgets.ScriptFloatView
import com.hive.script.views.widgets.ScriptNumberQuickView
import com.hive.utils.GlobalApp
import com.hive.views.widgets.FloatOptView

/**
 *
 * @author jiadou
 * @date 6/16/21
 */
class CmdDelayEditView(context: Context) : BaseCommandEditCard(context), View.OnClickListener {

    var cmd: CmdDelay? = null

    private var number_end_duration: ScriptFloatView? = null
    private var number_quick_view: ScriptNumberQuickView? = null
    private var number_start_duration: ScriptFloatView? = null
    override fun initView() {
        number_start_duration = findViewById(R.id.number_start_duration)
        number_end_duration = findViewById(R.id.number_end_duration)
        number_quick_view = findViewById(R.id.number_quick_view)

        number_start_duration?.changedListener =
            FloatOptView.OnValueChangedListener { value ->
                cmd?.startDuration = value.toLong()
            }
        number_end_duration?.changedListener =
            FloatOptView.OnValueChangedListener { value ->
                cmd?.endDuration = value.toLong()
            }
        number_quick_view?.onItemClickedListener =
            object : ScriptNumberQuickView.OnItemClickedListener {
                override fun onItemClicked(data: Pair<String, Any?>) {
                    val value = data.second as Float
                    cmd?.startDuration = value.toLong()
                    cmd?.endDuration = value.toLong()
                    number_start_duration?.setNumber(value)
                    number_end_duration?.setNumber(value)
                }
            }
        number_quick_view?.setDataSets(
            listOf(
                Pair(GlobalApp.getString(com.hive.i8n.R.string.cmd_0_1s), 100f),
                Pair(GlobalApp.getString(com.hive.i8n.R.string.cmd_0_5s), 500f),
                Pair(GlobalApp.getString(com.hive.i8n.R.string.cmd_1s), 1000f),
                Pair(GlobalApp.getString(com.hive.i8n.R.string.cmd_2s), 2000f),
                Pair(GlobalApp.getString(com.hive.i8n.R.string.cmd_3s), 3000f),
                Pair(GlobalApp.getString(com.hive.i8n.R.string.cmd_5s), 5000f),
                Pair(GlobalApp.getString(com.hive.i8n.R.string.cmd_10s), 10 * 1000f),
                Pair(GlobalApp.getString(com.hive.i8n.R.string.cmd_15s), 15 * 1000f),
                Pair(GlobalApp.getString(com.hive.i8n.R.string.cmd_20s), 20 * 1000f),
                Pair(
                    GlobalApp.getString(com.hive.i8n.R.string.cmd_25s), 25 * 1000f
                ),
                Pair(GlobalApp.getString(com.hive.i8n.R.string.cmd_30s), 30 * 1000f),
                Pair(GlobalApp.getString(com.hive.i8n.R.string.cmd_1m), 60 * 1000f),
                Pair(GlobalApp.getString(com.hive.i8n.R.string.cmd_2m), 120 * 1000f)
            )
        )
    }

    override fun checkCommandOrThrowError() {
        val minValue = number_start_duration?.getNumber() ?: 0f
        val maxValue = number_end_duration?.getNumber() ?: 0f
        if (minValue > maxValue) {
            throw RuntimeException(GlobalApp.getString(com.hive.i8n.R.string.cmd_value_has_worng_range))
        }
    }

    override fun onBindCommand(command: ScriptCommand) {
        cmd = command as CmdDelay
        number_start_duration?.setNumber((cmd?.startDuration ?: 100).toFloat())
        number_end_duration?.setNumber((cmd?.endDuration ?: 100).toFloat())
    }


    override fun getEditContentId() = R.layout.cmd_delay_edit_card

}