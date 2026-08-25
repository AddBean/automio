// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.edit.card.edit

import android.content.Context
import android.view.View
import com.hive.script.R
import com.hive.script.base.ScriptCommand
import com.hive.script.cmd.CmdPinchZoom
import com.hive.script.views.edit.views.ScaleInOutEditView

/**
 *
 * @author jiadou
 * @email 172111432@qq.com
 * @date 7/14/21
 */
class CmdScaleEditView(context: Context) : BaseCommandEditCard(context), View.OnClickListener {

    var cmd: CmdPinchZoom? = null

    override fun initView() {

    }

    override fun onBindCommand(command: ScriptCommand) {
        cmd = command as CmdPinchZoom
        findViewById<ScaleInOutEditView>(R.id.scale_editor)?.bindCommand(cmd!!)
    }

    override fun getEditContentId() = R.layout.cmd_scale_card

}