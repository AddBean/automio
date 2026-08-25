// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.views.dialog

import android.app.Dialog
import android.content.Context
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.TextView
import com.hive.views.R

class SimpleInputDialog(
    context: Context, title: String,
    inputType: Int = EditorInfo.TYPE_TEXT_FLAG_MULTI_LINE,
    successListener: (value: String, dialog: SimpleInputDialog) -> Unit
) : Dialog(context, R.style.base_dialog) {
    private var tv_title: TextView? = null
    private var edit_text: EditText? = null
    private var iv_close: View? = null
    private var btn_submit: TextView? = null

    init {
        setContentView(R.layout.simple_input_dialog)
        tv_title = findViewById(R.id.tv_title)
        edit_text = findViewById(R.id.edit_text)
        iv_close = findViewById(R.id.iv_close)
        btn_submit = findViewById(R.id.btn_submit)
        tv_title?.text = title
        edit_text?.inputType = inputType
        iv_close?.setOnClickListener {
            dismiss()
        }
        btn_submit?.setOnClickListener {
            var txt = edit_text?.text.toString()
            successListener.invoke(txt, this)
        }
        edit_text?.requestFocus()
    }

    fun setEditText(txt: String) {
        edit_text?.setText(txt)
    }
}