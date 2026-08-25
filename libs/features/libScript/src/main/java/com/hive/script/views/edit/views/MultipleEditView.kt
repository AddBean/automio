// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.edit.views

import android.content.Context
import android.util.AttributeSet
import android.view.View
import com.hive.base.BaseLayout
import com.hive.script.R
import com.hive.script.base.ScriptCommand
import com.hive.script.cmd.CmdPinch
import com.hive.script.views.widgets.ScriptFloatView
import com.hive.script.views.widgets.ScriptMultipleView
import com.hive.script.views.widgets.ScriptTabSelectorView
import com.hive.views.widgets.FloatOptView
import com.hive.views.widgets.SelectorTabView

/**
 *
 * @author jiadou
 * @email 172111432@qq.com
 * @date 6/16/21
 */
class MultipleEditView(context: Context?, attrs: AttributeSet?) : BaseLayout(context, attrs) {

    var cmd: CmdPinch? = null

    private var multiple_view: ScriptMultipleView? = null
    private var number_duration: ScriptFloatView? = null
    private var type_selector: ScriptTabSelectorView? = null

    override fun initView(view: View?) {
        multiple_view = findViewById(R.id.multiple_view)
        number_duration = findViewById(R.id.number_duration)
        type_selector = findViewById(R.id.type_selector)
        type_selector?.onTabSelectedChangedListener =
            object : SelectorTabView.OnTabSelectedChangedListener {

                override fun onSelectedChanged(p: Pair<String?, String?>?) {
                    cmd?.run {
                        fingerCount = p?.second?.toInt() ?: 2
                        bindCommand(this)
                    }
                }
            }
        number_duration?.changedListener = object : FloatOptView.OnValueChangedListener {
            override fun onValueChanged(value: Float) {
                cmd?.duration = value.toLong()
            }
        }
    }

    fun bindCommand(command: ScriptCommand) {
        cmd = command as CmdPinch
        type_selector?.setValue("" + (cmd?.fingerCount ?: 2))
        number_duration?.setNumber(cmd?.duration?.toFloat() ?: 1f)
        multiple_view?.enableTouch = false
        cmd?.run {
            multiple_view?.post {
                multiple_view?.loadCmd2(this, multiple_view!!.width, multiple_view!!.height)
            }
        }
    }

    override fun getLayoutId() = R.layout.muliple_edit_view

}