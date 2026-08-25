// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.edit.card

import android.content.Context
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import com.hive.anim.AnimUtils
import com.hive.script.R
import com.hive.script.base.ScriptCommand
import com.hive.script.base.core.ScriptParser
import com.hive.script.extensions.getIndexInParent
import com.hive.script.views.edit.AbsListItemView
import com.hive.script.views.edit.ScriptMenuEditHelper
import com.hive.utils.GlobalApp
import com.hive.views.popmenu.PopMenuManager

/**
 *
 * @author jiadou
 * @email 172111432@qq.com
 * @date 6/16/21
 */
class BaseCommandCard(context: Context) : AbsListItemView(context), View.OnClickListener {

    private lateinit var command: ScriptCommand

    private var btn_more: View? = null
    private var layout_add: View? = null
    private var layout_main: View? = null
    private var llDelayPart: View? = null
    private var tvDelay: TextView? = null
    private var tv_desc: TextView? = null
    private var tv_index: TextView? = null
    private var tv_name: TextView? = null


    var view: View = LayoutInflater.from(context).inflate(R.layout.common_cmd_card, this)

    init {
        btn_more = findViewById(R.id.btn_more)
        layout_add = findViewById(R.id.layout_add)
        layout_main = findViewById(R.id.layout_main)
        llDelayPart = findViewById(R.id.llDelayPart)
        tvDelay = findViewById(R.id.tvDelay)
        tv_desc = findViewById(R.id.tv_desc)
        tv_index = findViewById(R.id.tv_index)
        tv_name = findViewById(R.id.tv_name)
        layout_main?.setOnClickListener(this)
        btn_more?.setOnClickListener(this)
        layout_add?.setOnClickListener(this)
        tv_name?.setOnClickListener(this)
        llDelayPart?.setOnClickListener(this)
    }

    override fun bindData(data: Any?) {
        command = data as ScriptCommand
        tv_index?.setBackgroundColor(ScriptParser.getColor(command))
        tv_index?.text = "${command.getIndexInParent()}"
        tv_name?.text =
            if (TextUtils.isEmpty(command.comment)) command.getCommandName() else command.comment
        tv_desc?.text = "${command.getCommandDescribe()}"
        tvDelay?.text = "${command.startDelay}"
    }

    override fun onClick(v: View?) {
        AnimUtils.scaleAnim(v)
        when (v?.id) {
            R.id.llDelayPart -> {
                ScriptMenuEditHelper.showEditDialog(context, command, true, { event, cmd ->
                    postEvent(event)
                }) { _ ->
                    postEvent(ScriptMenuEditHelper.ClickType.REFRESH)
                }
            }

            R.id.btn_more -> {
                showSubMenu(v)
            }

            R.id.layout_add -> {
                postEvent(ScriptMenuEditHelper.ClickType.INSERT_RECORD)
            }

            R.id.layout_main -> {
                ScriptMenuEditHelper.showEditDialog(context, command, false, { event, cmd ->
                    postEvent(event)
                }) {
                    bindData(it)
                }
            }

            else -> {
                ScriptMenuEditHelper.showEditDialog(context, command, false, { event, cmd ->
                    postEvent(event)
                }) {
                    bindData(it)
                }
            }
        }
    }

    private fun showSubMenu(v: View) {
        PopMenuManager.instance.showMenu(
            v,
            GlobalApp.getResources().getStringArray(com.hive.i8n.R.array.sc_sub_cmd_menu_array).toList(),
            object : PopMenuManager.OnItemClickListener<String> {
                override fun onItemClicked(view: View, data: String, pos: Int) {
                    when (pos) {
                        0 -> {
                            postEvent(ScriptMenuEditHelper.ClickType.COMMENT)
                        }

                        1 -> {
                            postEvent(ScriptMenuEditHelper.ClickType.COPY)
                        }

                        2 -> {
                            postEvent(ScriptMenuEditHelper.ClickType.DELETE)
                        }

                        3 -> {
                            postEvent(ScriptMenuEditHelper.ClickType.RUN_NEXT_ALL)
                        }
                    }
                }
            })
    }


}