// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.edit.card.edit

import android.content.Context
import android.view.View
import com.hive.script.R
import com.hive.script.base.ScriptCommand
import com.hive.script.cmd.CmdClickView
import com.hive.script.views.edit.views.ClickViewEditView

/**
 *
 * @author jiadou
 * @email 172111432@qq.com
 * @date 6/16/21
 */
class CmdClickViewEditView(context: Context) : BaseCommandEditCard(context), View.OnClickListener {

    var cmd: CmdClickView? = null

    override fun initView() {

    }

    override fun onBindCommand(command: ScriptCommand) {
        cmd = command as CmdClickView
        findViewById<ClickViewEditView>(R.id.eidt_view)?.bindData(cmd!!)
        findViewById<ClickViewEditView>(R.id.eidt_view)?.checkCommandOrThrowError()
    }


    override fun getEditContentId() = R.layout.cmd_click_layout_card

}