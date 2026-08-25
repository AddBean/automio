// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.edit.card.edit

import android.content.Context
import android.view.View
import com.hive.script.R
import com.hive.script.base.ScriptCommand
import com.hive.script.cmd.CmdClickColor
import com.hive.script.views.edit.views.ClickColorEditView

/**
 *
 * @author jiadou
 * @email 172111432@qq.com
 * @date 7/14/21
 */
class CmdClickColorEditView(context: Context) : BaseCommandEditCard(context), View.OnClickListener {

    var cmd: CmdClickColor? = null

    override fun initView() {

    }

    override fun onBindCommand(command: ScriptCommand) {
        cmd = command as CmdClickColor
        findViewById<ClickColorEditView>(R.id.spot_editor)?.loadCmdSpot(cmd!!)
    }

    override fun checkCommandOrThrowError() {
        findViewById<ClickColorEditView>(R.id.spot_editor)?.checkCommandOrThrowError()
    }

    override fun getEditContentId() = R.layout.cmd_spot_color

}