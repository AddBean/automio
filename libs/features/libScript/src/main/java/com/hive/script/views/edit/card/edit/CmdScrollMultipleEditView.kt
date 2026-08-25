// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.edit.card.edit

import android.content.Context
import android.view.View
import com.hive.script.R
import com.hive.script.base.ScriptCommand
import com.hive.script.cmd.CmdScrollMultiple
import com.hive.script.views.widgets.ScriptFloatView
import com.hive.script.views.widgets.ScriptScrollSnapView
import com.hive.views.widgets.FloatOptView

/**
 *
 * @author jiadou
 * @email 172111432@qq.com
 * @date 6/16/21
 */
class CmdScrollMultipleEditView(context: Context) : BaseCommandEditCard(context),
    View.OnClickListener {

    var cmd: CmdScrollMultiple? = null

    private var number_delay: ScriptFloatView? = null
    private var scroll_snap_view: ScriptScrollSnapView? = null

    override fun initView() {
        number_delay = findViewById(R.id.number_delay)
        scroll_snap_view = findViewById(R.id.scroll_snap_view)
        number_delay?.changedListener = object : FloatOptView.OnValueChangedListener {
            override fun onValueChanged(value: Float) {
                cmd?.duration = value.toLong()
            }
        }
    }

    override fun onBindCommand(command: ScriptCommand) {
        cmd = command as CmdScrollMultiple
        number_delay?.getNumberView()?.inputEnable = false
        number_delay?.getNumberView()?.updateUiStatus()
        number_delay?.setNumber(cmd?.duration?.toFloat() ?: 100f)
        scroll_snap_view?.loadCmdScrollMultiple(cmd!!)
    }


    override fun getEditContentId() = R.layout.cmd_scroll_card

}