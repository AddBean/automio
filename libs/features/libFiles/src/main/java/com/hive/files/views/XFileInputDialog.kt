// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.files.views

import android.app.Dialog
import android.content.Context
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import com.hive.libfiles.R

class XFileInputDialog(
    context: Context,
    title: String,
    successListener: (value: String, dialog: XFileInputDialog) -> Unit
) : Dialog(context, com.hive.views.R.style.base_dialog) {
    private var tv_title: TextView? = null
    private var iv_close: ImageView? = null
    private var btn_submit: TextView? = null
    private var edit_text: EditText? = null

    init {

        setContentView(R.layout.x_file_input_dialog)
        tv_title = findViewById(R.id.tv_title)
        iv_close = findViewById(R.id.iv_close)
        btn_submit = findViewById(R.id.btn_submit)
        edit_text = findViewById(R.id.edit_text)

        tv_title?.text = title
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