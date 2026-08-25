// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.richeditor.views

import android.app.Dialog
import android.content.Context
import android.text.TextUtils
import android.view.View
import android.widget.EditText
import com.hive.editor.R
import com.hive.views.widgets.CommonToast

class EditInputDialog(context: Context) : Dialog(context, com.hive.views.R.style.base_dialog) {
    var onConfirmListener: OnConfirmListener? = null

    private var edit_text: EditText? = null
    private var iv_close: View? = null
    private var btn_submit: View? = null

    init {
        setContentView(R.layout.edit_input_dialog)
        edit_text = findViewById(R.id.edit_text)
        iv_close = findViewById(R.id.iv_close)
        btn_submit = findViewById(R.id.btn_submit)
        iv_close?.setOnClickListener {
            dismiss()
        }
        btn_submit?.setOnClickListener {
            var txt = edit_text?.text.toString()
            if(TextUtils.isEmpty(txt)){
                CommonToast.getInstance().showToast(com.hive.i8n.R.string.empty_input_error_toast)
            }else{
                onConfirmListener?.onConfirmed(txt)
            }
        }
        edit_text?.requestFocus()
    }

    fun setEditText(txt: String) :EditInputDialog{
        edit_text?.setText(txt)
        return this
    }

    interface OnConfirmListener {
        fun onConfirmed(text: String?)
    }
}