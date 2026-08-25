// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.dialog

import android.annotation.SuppressLint
import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.TextView
import com.hive.script.R
import com.hive.script.views.widgets.BaseScriptDialog
import com.hive.utils.system.CommonUtils

/**
 *
 * @author jiadou
 * @email 172111432@qq.com
 * @date 6/11/21
 */
@SuppressLint("ViewConstructor")
class DialogInputNumber(
    context: Context?,
    private var title: String,
    private var hint: String = "",
    private var txtHold: String? = null,
    private var checkInputFun: ((edit_text: EditText) -> Unit)? = null,
    private var confirmFun: ((dialog: DialogInputNumber, input: Float) -> Unit)
) : BaseScriptDialog(context) {

    private var btn_submit: View? = null
    private var edit_text: EditText? = null
    private var iv_close: View? = null
    private var tv_title: TextView? = null

    override fun initWindow() {
        btn_submit = findViewById(R.id.btn_submit)
        edit_text = findViewById(R.id.edit_text)
        iv_close = findViewById(R.id.iv_close)
        tv_title = findViewById(R.id.tv_title)
        tv_title?.text = title
        edit_text?.hint = if (hint.isNullOrEmpty()) context?.getString(com.hive.i8n.R.string.script_input_hint_format, title) ?: com.hive.utils.GlobalApp.getString(com.hive.i8n.R.string.script_input_hint_format, title) else hint
        iv_close?.setOnClickListener {
            dismiss()
        }
        btn_submit?.setOnClickListener {
            try {
                checkInputFun?.invoke(edit_text!!)
                if (edit_text!!.text.toString().isEmpty()) return@setOnClickListener
                confirmFun.invoke(this, edit_text!!.text.toString().toFloat())
                dismiss()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }


        edit_text?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable?) {
            }

        })
        edit_text?.requestFocus()
        edit_text?.post {
            CommonUtils.openKeyboard(edit_text)
            tv_title?.text = title
            edit_text?.hint = hint
            txtHold?.let { edit_text?.setText(it) }
        }

    }

    fun setEditTextType(type: Int): DialogInputNumber {
        edit_text?.inputType = type
        return this
    }


    override fun enableFadeAnimation() = true

    override fun isTouchOutsideDismissed() = true

    override fun getWindowLayoutId() = R.layout.dialog_input_number
}