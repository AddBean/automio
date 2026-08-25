// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.edit.card.edit.condition

import android.content.Context
import android.view.View
import com.hive.base.BaseLayout
import com.hive.script.base.ScriptCommand
import com.hive.script.base.ScriptCommandCondition
import com.hive.script.views.edit.ScriptMenuEditHelper
import com.hive.utils.GlobalApp

/**
 *
 * @author jiadou
 * @email 172111432@qq.com
 * @date 6/16/21
 */
abstract class BaseConditionEditCard(context: Context) : BaseLayout(context),
    View.OnClickListener {

    protected val dp = GlobalApp.DP

    constructor(context: Context, attributeSet: android.util.AttributeSet) : this(context)

    protected var onPostEvent: ((clickType: ScriptMenuEditHelper.ClickType, cnd: ScriptCommandCondition?) -> Unit)? =
        null

    var onDismissed: (() -> Unit)? = null

    override fun initView(p0: View?) {
        initView()
    }

    open fun initView() {

    }

    fun setPostEventHandler(onPostEvent: (clickType: ScriptMenuEditHelper.ClickType, cnd: ScriptCommandCondition?) -> Unit) {
        this.onPostEvent = onPostEvent
    }

    override fun getLayoutId() = getEditContentId()

    abstract fun onBindCommand(command:ScriptCommand)

    abstract fun getEditContentId(): Int


    override fun onClick(v: View?) {

    }

}