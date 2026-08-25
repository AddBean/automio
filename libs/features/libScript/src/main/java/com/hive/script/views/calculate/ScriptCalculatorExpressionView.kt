// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.calculate

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.View
import com.hive.script.views.widgets.ScriptSpanParamEditView

class ScriptCalculatorExpressionView(context: Context?, attrs: AttributeSet?) :
    ScriptSpanParamEditView(context, attrs) {
    init {
        this.showSoftInputOnFocus = false // 点击输入框时不显示系统键盘
        this.onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {

            }
        }
        this.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                this@ScriptCalculatorExpressionView.setSelection(s?.length ?: 0) // 光标后移
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }
}