// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.edit.card.edit

import android.content.Context
import android.view.View
import com.hive.script.R
import com.hive.script.base.ScriptCommand
import com.hive.script.cmd.CmdClickText
import com.hive.script.views.edit.views.ClickTextEditView
import com.hive.utils.extends.string

/**
 *
 * @author jiadou
 * @email 172111432@qq.com
 * @date 7/14/21
 */
class CmdClickTextEditView(context: Context) : BaseCommandEditCard(context), View.OnClickListener {

    var cmd: CmdClickText? = null

    override fun initView() {

    }

    override fun onBindCommand(command: ScriptCommand) {
        cmd = command as CmdClickText
        findViewById<ClickTextEditView>(R.id.spot_editor)?.onBindCommand(cmd!!)
    }

    override fun checkCommandOrThrowError() {
        if (cmd?.targetText.isNullOrEmpty()) {
            throw IllegalArgumentException(com.hive.i8n.R.string.error_cmd_click_text_empty.string())
        }
        findViewById<ClickTextEditView>(R.id.spot_editor)?.checkCommandOrThrowError()
    }

    override fun getEditContentId() = R.layout.cmd_spot_text

}