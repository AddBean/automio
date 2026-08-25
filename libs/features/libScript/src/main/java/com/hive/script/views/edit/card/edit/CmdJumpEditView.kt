// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.edit.card.edit

import android.content.Context
import android.view.View
import android.view.View.OnClickListener
import com.hive.script.R
import com.hive.script.base.ScriptCommand
import com.hive.script.cmd.CmdJump
import com.hive.script.utils.ScriptHelper
import com.hive.script.views.dialog.DialogCommonSelector
import com.hive.script.views.widgets.ScriptValueView
import com.hive.utils.GlobalApp
import com.hive.views.widgets.CommonToast

/**
 *
 * @author jiadou
 * @email 172111432@qq.com
 * @date 6/16/21
 */
class CmdJumpEditView(context: Context) : BaseCommandEditCard(context), View.OnClickListener {

    var cmd: CmdJump? = null

    override fun initView() {
        findViewById<ScriptValueView>(R.id.point_name)?.onMaskClickListener =
            OnClickListener { showSelector() }

    }

    private fun showSelector() {
        val points = ScriptHelper.getJumpPoints(cmd?.getRootScript() ?: return)
        if (points.isEmpty()) {
            CommonToast.show(com.hive.i8n.R.string.sc_jump_point_empty)
            return
        }
        DialogCommonSelector(context).setTitle(GlobalApp.getString(com.hive.i8n.R.string.sc_jump_menu_title))
            .setDataSet(
                points.map {
                    it.id to it.getCommandName()
                }.toMutableList()
            ).setSelectListener(object : DialogCommonSelector.OnSelectListener {
                override fun onSelected(
                    dialog: DialogCommonSelector, pos: Int, pair: Pair<Int, String>
                ) {
                    cmd?.id = pair.first
                    onBindCommand(cmd!!)
                    dialog.dismiss()
                }

                override fun onCancel() {
                }
            }).show()
    }

    override fun onBindCommand(command: ScriptCommand) {
        cmd = command as CmdJump
        findViewById<ScriptValueView>(R.id.point_name)?.setValue(
            GlobalApp.getString(
                com.hive.i8n.R.string.cmd_des_jump_point,
                cmd?.id
            )
        )
    }


    override fun getEditContentId() = R.layout.cmd_jump_card

}