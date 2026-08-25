// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.edit.card.edit

import android.content.Context
import android.view.View
import com.hive.script.R
import com.hive.script.base.ScriptCommand
import com.hive.script.cmd.CmdAlignToSecond
import com.hive.script.views.widgets.ScriptValueView
import com.hive.utils.GlobalApp
import com.hive.views.popmenu.PopMenuManager

/**
 *
 * @author jiadou
 * @email 172111432@qq.com
 * @date 6/16/21
 */
class CmdTimeCalibratorEditView(context: Context) : BaseCommandEditCard(context),
    View.OnClickListener {

    var cmd: CmdAlignToSecond? = null

    private var time_type_name: ScriptValueView? = null

    override fun initView() {
        time_type_name = findViewById(R.id.time_type_name)
        time_type_name?.onMaskClickListener = OnClickListener {
            showSelector()
        }
        time_type_name?.onMaskClickListener = OnClickListener { showSelector() }
    }

    private fun showSelector() {
        val ls = CmdAlignToSecond.valueMap.map { it.value }.toList()
        PopMenuManager.instance.showMenu(
            time_type_name?.findViewById(R.id.tv_value)!!,
            0,
            2 * GlobalApp.DP,
            ls,
            object : PopMenuManager.OnItemClickListener<String> {
                override fun onItemClicked(view: View, data: String, pos: Int) {
                    CmdAlignToSecond.valueMap.filter { it.value == data }.keys.firstOrNull()?.let {
                        cmd?.value = it
                    }
                    onBindCommand(cmd!!)
                }
            })
    }

    override fun onBindCommand(command: ScriptCommand) {
        cmd = command as CmdAlignToSecond
        val value = cmd?.value
        time_type_name?.setValue(CmdAlignToSecond.valueMap[value] ?: "")
    }


    override fun getEditContentId() = R.layout.cmd_open_time_calibrator

}