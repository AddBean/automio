// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.widgets

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import com.hive.script.R
import com.hive.script.base.params.ScriptParam
import com.hive.script.utils.ScriptCommandHelper
import com.hive.utils.utils.GsonHelper


open class ScriptSpanParamEditView(context: Context?, attrs: AttributeSet?) :
    ScriptSpanBaseEditView(
        context, attrs
    ) {

    private var focusIndex: Int = 0

    private var spanText: ScriptSpanHelper.ParamsSpan? = null

    private var rawText = ""

    private var disableReloadOnce = false

    init {
        setHint(com.hive.i8n.R.string.sc_edit_hint)
        addTextChangedListener(object : TextWatcher {
            private var spanLength = -1
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                //如果是中文输入法则返回
                if (count > 1) return
                if (start == 0) return
                if (count > after) {
                    val spans = editableText.getSpans(
                        start + count, start + count,
                        ScriptSpanHelper.ClickSpan::class.java
                    )
                    if (spans == null || spans.isEmpty()) return
                    for (i in spans.indices) {
                        val end = editableText.getSpanEnd(spans[i])
                        if (end != start + count) continue
                        val text = spans[i].rawValue
                        spanLength = text!!.length - 1
                        editableText.removeSpan(spans[i])
                    }
                }
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                //如果是中文输入法则返回
                if (count > 1) return
                if (spanLength > -1) {
                    val length = spanLength
                    spanLength = -1
                    editableText.replace(start - length, start, "")
                    //恢复光标
                    setSelection(start - length)
                }
            }

            override fun afterTextChanged(s: Editable?) {
                //originalText = s.toString()
                rawText = s.toString()
                if (!disableReloadOnce) {
                    disableReloadOnce = true
                    //记录下当前焦点的index位置
                    focusIndex = selectionStart
                    reloadSpanText()
                } else {
                    disableReloadOnce = false
                }
            }
        })
    }

    fun insertParams(param: ScriptParam?) {
        val content = ScriptCommandHelper.paramFormat.format(param?.getFullId())
        var selectionStart = selectionStart
        if (selectionStart == -1) {
            selectionStart = editableText.length
        }
        if (selectionStart > editableText.length) {
            selectionStart = editableText.length
        }
        val edt = editableText.insert(selectionStart, content)
        rawText = ScriptCommandHelper.parseToRawText(edt)
        focusIndex = selectionStart
        reloadSpanText()
    }

    fun setSpanText(content: String?) {
        try {
            rawText = content ?: ""
            spanText = ScriptSpanHelper.parseSpanText(content ?: "") {
                refreshSpanText()
            }
            spanText?.spans ?: return
            spanText?.run {
                this@ScriptSpanParamEditView.setSpans(spanText?.content!!, this.spans) { span, _ ->
//                    ScriptCommandHelper.parseParamsId(
//                        span.rawValue ?: return@setSpans
//                    ).let {
//                        val param = ScriptParamEnv.getParam(it) ?: return@setSpans
//                        if (param.readOnly) return@setSpans
//                        clearFocus()
//                        DialogParamsEdit(context)
//                            .setTitle(com.hive.i8n.R.string.sc_param_manager_edit_title.string())
//                            .setEditParams(param)
//                            .show()
//                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun format() {
        val edt = editableText.toString()
        val format = GsonHelper.toFormatJsonString(edt)
        rawText = format
        reloadSpanText()
    }

    private fun reloadSpanText() {
        setSpanText(rawText)
        post {
            setSelection(focusIndex)
        }
    }

    private fun refreshSpanText() {
        setSpanText(rawText)
        requestLayout()
    }

}