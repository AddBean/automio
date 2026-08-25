// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.widgets

import android.content.Context
import android.util.AttributeSet
import com.hive.script.base.params.ScriptParamEnv
import com.hive.script.utils.ScriptCommandHelper
import com.hive.script.views.dialog.DialogParamsEdit
import com.hive.utils.extends.string

class ScriptSpanParamTextView(context: Context?, attrs: AttributeSet?) :
    ScriptSpanBaseTextView(
        context, attrs
    ) {


    private var spanText: ScriptSpanHelper.ParamsSpan? = null

    private var rawText = ""

    fun setSpanText(content: String?) {
        try {
            rawText = content ?: ""
            spanText = ScriptSpanHelper.parseSpanText(content ?: "") {
                refreshSpanText()
            }
            spanText?.spans ?: return
            spanText?.run {
                setSpans(spanText?.content!!, this.spans) { span, _ ->
                    ScriptCommandHelper.parseParamsId(
                        span.rawValue ?: return@setSpans
                    ).let {
                        val param = ScriptParamEnv.getParam(it) ?: return@setSpans
                        if (param.writable) return@setSpans
                        clearFocus()
                        DialogParamsEdit(context)
                            .setTitle(com.hive.i8n.R.string.sc_param_manager_edit_title.string())
                            .setEditParams(param)
                            .show()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun refreshSpanText() {
        setSpanText(rawText)
        requestLayout()
    }

}