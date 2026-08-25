// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.dialog

import android.content.Context
import android.view.View
import android.widget.TextView
import com.hive.script.R
import com.hive.script.base.params.ScriptParam
import com.hive.script.base.params.ScriptParamEnv
import com.hive.script.views.edit.card.edit.ParamEditView
import com.hive.script.views.widgets.BaseScriptDialog
import com.hive.views.widgets.CommonToast

class DialogParamsEdit(context: Context?) : BaseScriptDialog(context) {
    private var mCallback: ((param: ScriptParam) -> Unit)? = null
    private var param: ScriptParam? = ScriptParam("", "", "", "", "", false)
    private var btnCancel: View? = null
    private var btnSubmit: View? = null
    private var editParams: ParamEditView? = null
    private var ivClose: View? = null
    private var tv_title: TextView? = null

    override fun initWindow() {
        post {
            btnCancel = findViewById(R.id.btnCancel)
            btnSubmit = findViewById(R.id.btnSubmit)
            editParams = findViewById(R.id.editParams)
            ivClose = findViewById(R.id.ivClose)
            tv_title = findViewById(R.id.tv_title)
            editParams?.bindParam(param!!)
            ivClose?.setOnClickListener {
                dismiss()
            }
            btnCancel?.setOnClickListener {
                dismiss()
            }
            btnSubmit?.setOnClickListener {
                try {
                    editParams?.checkOrThrowError()
                    if (editParams?.isEdit == false) {
                        ScriptParamEnv.getParamEnv()?.addParam(param!!)
                    } else {
                        ScriptParamEnv.getParamEnv()
                            ?.writeParamInit(param!!.getFullId(), param!!.initValue)
                    }
                    mCallback?.invoke(param!!)
                    dismiss()
                } catch (e: IllegalArgumentException) {
                    CommonToast.show(e.message)
                }
            }
        }
    }

    fun setEditParams(param: ScriptParam): DialogParamsEdit {
        this.param = param.copy()
        editParams?.setEditMode(true)
        editParams?.bindParam(param)
        return this
    }

    fun setTitle(title: String): DialogParamsEdit {
        tv_title?.text = title
        return this
    }

    fun setCallback(callback: (param: ScriptParam) -> Unit): DialogParamsEdit {
        mCallback = callback
        return this
    }

    override fun enableFadeAnimation() = true

    override fun getWindowLayoutId() = R.layout.dialog_params_add
}