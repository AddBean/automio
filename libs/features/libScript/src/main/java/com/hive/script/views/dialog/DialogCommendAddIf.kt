// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.dialog

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.view.ViewGroup
import com.hive.script.R
import com.hive.script.cmd.CmdIf
import com.hive.script.views.edit.card.edit.BaseCommonEditView
import com.hive.script.views.edit.card.edit.CmdIfEditView
import com.hive.script.views.widgets.BaseScriptDialog
import com.hive.utils.GlobalApp
import com.hive.views.widgets.CommonToast

/**
 *
 * @author jiadou
 * @date 6/11/21
 */
@SuppressLint("ViewConstructor")
class DialogCommendAddIf(context: Context?, var type: Int) : BaseScriptDialog(context) {
    var mCallback: ((cmd: CmdIf) -> Unit)? = null

    private var editIfView: CmdIfEditView? = null

    private var btnCancel: View? = null
    private var btnSubmit: View? = null
    private var iv_close: View? = null
    private var layout_menu: ViewGroup? = null

    override fun initWindow() {
        editIfView = CmdIfEditView(context!!)
        layout_menu = findViewById(R.id.layout_menu)
        btnCancel = findViewById(R.id.btnCancel)
        btnSubmit = findViewById(R.id.btnSubmit)
        iv_close = findViewById(R.id.iv_close)

        val cmd = CmdIf.createCommand(mutableListOf(), mutableListOf())
        layout_menu?.addView(editIfView)
        editIfView?.bindCommand(cmd)

        iv_close?.setOnClickListener {
            dismiss()
        }
        btnCancel?.setOnClickListener {
            dismiss()
        }
        btnSubmit?.setOnClickListener {
            try {
                editIfView?.checkCommandOrThrowError()
                mCallback?.invoke(cmd)
                dismiss()
            } catch (e: Exception) {
                CommonToast.show(GlobalApp.getString(com.hive.i8n.R.string.sc_add_cmd_if_error))
            }
        }
        post {
            editIfView?.setConditionType(type)
        }
    }

    override fun enableFadeAnimation() = true


    override fun getWindowLayoutId() = R.layout.dialog_commend_add_if
}