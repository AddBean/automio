// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.edit.card.edit

import android.content.Context
import android.view.View
import com.hive.script.R
import com.hive.script.base.ScriptCommand
import com.hive.script.cmd.CmdToast
import com.hive.script.views.widgets.ScriptSpanParamLayout
import com.hive.utils.utils.StringUtils

/**
 *
 * @author jiadou
 * @email 172111432@qq.com
 * @date 6/16/21
 */
class CmdNoSupportEditView(context: Context) : BaseCommandEditCard(context), View.OnClickListener {

    var cmd: ScriptCommand? = null

    override fun initView() {

    }

    override fun expandCommonEdit() = false

    override fun onBindCommand(command: ScriptCommand) {
        cmd = command
    }


    override fun getEditContentId() = R.layout.cmd_no_support_edit_card

}