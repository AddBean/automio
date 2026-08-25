// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.edit.card.edit

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.hive.base.BaseLayout
import com.hive.script.R
import com.hive.script.base.ScriptCommand
import com.hive.script.views.edit.ScriptMenuEditHelper

/**
 *
 * @author jiadou
 * @date 6/16/21
 */
abstract class BaseCommandEditCard(context: Context) : BaseLayout(context), View.OnClickListener {

    constructor(context: Context, attributeSet: AttributeSet?) : this(context)

    constructor(context: Context, attributeSet: AttributeSet?, defStyleAttr: Int?) : this(context)

    private var isDelayEdit: Boolean = false

    var onCommandBinded: ((cmd: ScriptCommand?) -> Unit)? = null

    var editCmd: ScriptCommand? = null

    var onPostEvent: ((clickType: ScriptMenuEditHelper.ClickType, cmd: ScriptCommand?) -> Unit)? =
        null

    var onDismissed: (() -> Unit)? = null


    var editCommon: BaseCommonEditView? = null

    var editLayout: ViewGroup? = null

    fun postEvent(clickType: ScriptMenuEditHelper.ClickType) {
        onPostEvent?.invoke(clickType, editCmd)
    }

    override fun initView(p0: View?) {
        editLayout = findViewById(R.id.editLayout)
        editCommon = findViewById(R.id.editCommon)
        LayoutInflater.from(context).inflate(getEditContentId(), editLayout!!)
        initView()
    }

    open fun initView() {

    }

    fun setPostEventHandler(onPostEvent: (clickType: ScriptMenuEditHelper.ClickType, cmd: ScriptCommand?) -> Unit) {
        this.onPostEvent = onPostEvent
    }

    override fun getLayoutId() = R.layout.sc_base_command_edit_card

    open fun isSupportEdit() = true

    fun setDelayEdit(delayEdit: Boolean) {
        isDelayEdit = delayEdit
    }

    fun bindCommand(command: ScriptCommand) {
        editCmd = command
        editCommon?.setVisibleGone(isDelayEdit)
        onBindCommand(command)
        editCommon?.onBindCommand(command)
        onCommandBinded?.invoke(command)
        editCommon?.post {
            if (expandCommonEdit()) {
                editCommon?.expandCommon()
            }
        }
    }

    open fun expandCommonEdit() = false

    open fun checkCommandOrThrowError() {
//        throw RuntimeException("checkCommand not implemented")
    }

    abstract fun onBindCommand(command: ScriptCommand)

    abstract fun getEditContentId(): Int

    override fun onClick(v: View?) {

    }

}