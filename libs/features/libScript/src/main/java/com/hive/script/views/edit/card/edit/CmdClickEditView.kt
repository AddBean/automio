// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.edit.card.edit

import android.content.Context
import android.view.View
import com.hive.script.R
import com.hive.script.base.ScriptCommand
import com.hive.script.cmd.CmdClick
import com.hive.script.views.widgets.ScriptRandomSizeView
import com.hive.script.views.widgets.ScriptSizeSeekbarView

/**
 *
 * @author jiadou
 * @email 172111432@qq.com
 * @date 6/16/21
 */
class CmdClickEditView(context: Context) : BaseCommandEditCard(context), View.OnClickListener {

    var cmd: CmdClick? = null

    override fun initView() {
        findViewById<ScriptRandomSizeView>(R.id.random_size)?.mOnProgressChanged =
            object : ScriptSizeSeekbarView.OnSizeChangedListener {
                override fun onSizeChanged(action: Int, size: Int) {
                    cmd?.random = size
                }
            }
    }

    override fun onBindCommand(command: ScriptCommand) {
        cmd = command as CmdClick
        findViewById<ScriptRandomSizeView>(R.id.random_size)?.setValue(cmd?.random ?: 0)
    }

    override fun getEditContentId() = R.layout.cmd_click_card

}