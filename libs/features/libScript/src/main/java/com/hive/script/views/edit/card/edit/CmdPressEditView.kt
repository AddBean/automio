// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.edit.card.edit

import android.content.Context
import android.view.View
import com.hive.script.R
import com.hive.script.base.ScriptCommand
import com.hive.script.base.ScriptConst
import com.hive.script.cmd.CmdPress
import com.hive.script.views.widgets.ScriptFloatView
import com.hive.views.widgets.FloatOptView

/**
 *
 * @author jiadou
 * @email 172111432@qq.com
 * @date 6/16/21
 */
class CmdPressEditView(context: Context) : BaseCommandEditCard(context), View.OnClickListener {

    var cmd: CmdPress? = null

    private var number_duration: ScriptFloatView? = null
    override fun initView() {
        number_duration = findViewById<ScriptFloatView>(R.id.number_duration)
        number_duration?.changedListener = object : FloatOptView.OnValueChangedListener {
            override fun onValueChanged(value: Float) {
                cmd?.duration = value.toLong()
            }
        }
    }

    override fun onBindCommand(command: ScriptCommand) {
        cmd = command as CmdPress
        number_duration?.setNumber(
            cmd?.duration?.toFloat()
                ?: ScriptConst.Cmd_Long_Click_Default.toFloat()
        )

    }

    override fun getEditContentId() = R.layout.cmd_long_click_card

}