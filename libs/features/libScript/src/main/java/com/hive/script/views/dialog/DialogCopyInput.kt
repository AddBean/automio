// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.dialog

import android.content.Context
import android.text.TextUtils
import android.view.View
import android.widget.ImageView
import com.hive.script.R
import com.hive.script.views.widgets.BaseScriptDialog
import com.hive.script.views.widgets.ScriptSpanParamLayout
import com.hive.utils.CommomListener
import com.hive.utils.system.CommonUtils
import com.hive.views.widgets.CommonToast

/**
 *
 * @author jiadou
 * @email 172111432@qq.com
 * @date 6/11/21
 */
class DialogCopyInput(context: Context?) : BaseScriptDialog(context) {

    private var content: String? = null
    var mCallback: CommomListener.Callback? = null

    private var btn_submit: View? = null
    private var edit_text: ScriptSpanParamLayout? = null
    private var iv_close: ImageView? = null

    override fun initWindow() {
        btn_submit= findViewById(R.id.btn_submit)
        edit_text = findViewById(R.id.edit_text)
        iv_close = findViewById(R.id.iv_close)
        iv_close?.setOnClickListener {
            dismiss()
        }
        btn_submit?.setOnClickListener {
            try {
                checkInput()
                mCallback?.onEvent(0, content ?: "")
                dismiss()
            } catch (e: java.lang.Exception) {
                CommonToast.show(e.message)
            }

        }
        edit_text?.requestFocus()
        edit_text?.post {
            CommonUtils.openKeyboard(edit_text)
        }
    }

    private fun checkInput() {
        content = edit_text?.getText()
        if (TextUtils.isEmpty(content)) {
            throw Exception(getString(com.hive.i8n.R.string.sc_check_scheme_input_check_empty))
        }

        if (content?.length ?: 0 < 2) {
            throw Exception(getString(com.hive.i8n.R.string.sc_check_scheme_input_check_empty_2))
        }

    }

    override fun enableFadeAnimation() = true

    override fun getWindowLayoutId() = R.layout.dialog_copy_input
}