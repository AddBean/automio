// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.edit.views

import android.content.Context
import android.util.AttributeSet
import android.view.View
import com.hive.base.BaseLayout
import com.hive.script.R
import com.hive.script.base.ScriptCommand
import com.hive.script.cmd.CmdRepeatTap
import com.hive.script.views.widgets.ScriptFloatView
import com.hive.script.views.widgets.ScriptNumberView
import com.hive.script.views.widgets.ScriptRandomSizeView
import com.hive.script.views.widgets.ScriptSizeSeekbarView
import com.hive.views.widgets.FloatOptView
import com.hive.views.widgets.NumberOptView

/**
 *
 * @author jiadou
 * @date 6/16/21
 */
class FastClickEditView(context: Context?, attrs: AttributeSet?) : BaseLayout(context, attrs) {

    var cmd: CmdRepeatTap? = null

    private var number_gap: ScriptFloatView? = null
    private var number_times: ScriptNumberView? = null
    private var random_size: ScriptRandomSizeView? = null
    override fun initView(view: View?) {
        number_gap = findViewById(R.id.number_gap)
        number_times = findViewById(R.id.number_times)
        random_size = findViewById(R.id.random_size)
        number_gap?.changedListener = object : FloatOptView.OnValueChangedListener {
            override fun onValueChanged(value: Float) {
                cmd?.gap = value.toLong()
            }
        }
        number_times?.changedListener = object : NumberOptView.OnValueChangedListener {
            override fun onValueChanged(value: Int) {
                cmd?.count = value
            }
        }
        random_size?.mOnProgressChanged = object : ScriptSizeSeekbarView.OnSizeChangedListener {
            override fun onSizeChanged(action: Int, size: Int) {
                cmd?.random = size
            }
        }
    }

    fun bindCommand(command: ScriptCommand) {
        cmd = command as CmdRepeatTap

        number_times?.setNumber(cmd?.count ?: 0)
        number_gap?.setNumber(cmd?.gap?.toFloat() ?: 1f)
        random_size?.setValue(cmd?.random ?: 1)
    }

    fun hiddenPosition() {

    }

    override fun getLayoutId() = R.layout.spot_fast_click_edit_view

}