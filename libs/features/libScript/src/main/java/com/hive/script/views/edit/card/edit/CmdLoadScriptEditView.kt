// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.edit.card.edit

import android.content.Context
import android.view.View
import com.hive.script.R
import com.hive.script.base.ScriptCommand
import com.hive.script.cmd.CmdCallScript
import com.hive.script.views.beans.ScriptInfoModel
import com.hive.script.views.dialog.DialogScriptListSelector
import com.hive.script.views.widgets.ScriptValueView
import com.hive.utils.GlobalApp
import com.hive.utils.utils.StringUtils

/**
 *
 * @author jiadou
 * @email 172111432@qq.com
 * @date 6/16/21
 */
class CmdLoadScriptEditView(context: Context) : BaseCommandEditCard(context), View.OnClickListener {

    var cmd: CmdCallScript? = null

    override fun initView() {
        findViewById<ScriptValueView>(R.id.script_name)?.onMaskClickListener =
            OnClickListener { showSelector() }

    }

    private fun showSelector() {
        DialogScriptListSelector(context, true)
            .setTitle(GlobalApp.getString(com.hive.i8n.R.string.sc_load_script_menu_title))
            .setOnScriptSelectListener(object :
                DialogScriptListSelector.OnScriptSelectListener {
                override fun onSelected(
                    dialog: DialogScriptListSelector, model: ScriptInfoModel
                ) {
                    cmd?.scriptName = model.scriptName
                    cmd?.scriptPath = model.scriptPath
                    onBindCommand(cmd!!)
                    dialog.dismiss()
                }

                override fun onDismissed() {
                }
            }).show()

    }

    override fun onBindCommand(command: ScriptCommand) {
        cmd = command as CmdCallScript
        findViewById<ScriptValueView>(R.id.script_name)?.setValue(
            GlobalApp.getString(
                com.hive.i8n.R.string.cmd_des_load_script,
                StringUtils.decoding(cmd?.scriptName)
            )
        )
    }


    override fun getEditContentId() = R.layout.cmd_load_script_card

}