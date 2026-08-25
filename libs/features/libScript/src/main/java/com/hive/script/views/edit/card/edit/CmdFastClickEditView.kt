// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.edit.card.edit

import android.content.Context
import android.view.View
import com.hive.script.R
import com.hive.script.base.ScriptCommand
import com.hive.script.cmd.CmdRepeatTap
import com.hive.script.views.edit.views.FastClickEditView

/**
 *
 * @author jiadou
 * @email 172111432@qq.com
 * @date 6/16/21
 */
class CmdFastClickEditView(context: Context) : BaseCommandEditCard(context), View.OnClickListener {

    var cmd: CmdRepeatTap? = null

    override fun initView() {
    }

    override fun onBindCommand(command: ScriptCommand) {
        cmd = command as CmdRepeatTap
        findViewById<FastClickEditView>(R.id.edit_view).bindCommand(cmd!!)
    }

    override fun getEditContentId() = R.layout.cmd_fast_click_card

}