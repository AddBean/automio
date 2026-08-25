// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.widgets

import android.content.Context
import android.text.TextUtils
import android.util.AttributeSet
import android.view.View
import android.view.View.OnFocusChangeListener
import com.hive.script.R
import com.hive.script.ScriptProvider
import com.hive.script.base.ScriptConst
import com.hive.script.base.params.ScriptParam
import com.hive.script.base.params.ScriptParamEnv
import com.hive.script.net.data.ScriptImageBean
import com.hive.script.utils.ScriptColorHelper
import com.hive.script.utils.ScriptHelper
import com.hive.script.condition.ConditionPermission
import com.hive.script.views.dialog.DialogColorPicker
import com.hive.script.views.dialog.DialogCommonTextInput
import com.hive.script.views.dialog.DialogImageManager
import com.hive.script.views.dialog.DialogParamsManager
import com.hive.script.views.dialog.DialogPermissionMultiSelector
import com.hive.utils.GlobalApp
import com.hive.utils.utils.BitmapUtils
import com.hive.utils.utils.ColorUtils
import com.hive.views.popmenu.PopMenuManager

class ScriptSpanMenuView(context: Context?, attrs: AttributeSet?) :
    ScriptSpanBaseEditView(context, attrs) {

    private var spanList: List<ScriptSpanHelper.ClickSpan>? = null

    private var spanText: String? = null

    init {
        //不允许编辑和获取焦点
        this.isFocusable = false
        this.isFocusableInTouchMode = false
        this.isClickable = true
        this.isLongClickable = true
        this.showSoftInputOnFocus = false
        this.onFocusChangeListener = OnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                //隐藏光标
                this.isCursorVisible = false
            }
        }
        //不要背景
        this.setBackgroundResource(R.drawable.xml_transparent)
    }

    fun setSpanText(text: String?, spans: List<ScriptSpanHelper.ClickSpan>?) {
        this.spanList = spans
        this.spanText = text
        if (text.isNullOrEmpty() || spans.isNullOrEmpty()) {
            setText("")
            return
        }
        val content = String.format(text, *spans.map { it.spanText }.toTypedArray())
        spans.forEach { span ->
            var startIndex = 0
            while (startIndex < content.length) {
                val spanStart = content.indexOf(span.spanText ?: "", startIndex)
                if (spanStart == -1) break
                span.spanStart = spanStart
                span.spanEnd = spanStart + (span.spanText?.length ?: 0)
                startIndex = span.spanEnd
            }
        }
        this.setSpans(content, spans) { span, view ->
            handleEvent(span, view)
        }
    }

    fun refreshSpanText() {
        spanList ?: return
        spanText ?: return
        setSpanText(spanText, spanList)
        requestLayout()
    }

    private fun handleEvent(span: ScriptSpanHelper.ClickSpan, view: View) {
        when (span.spanType) {
            ScriptSpanHelper.ClickSpanType.ParamSelector -> {
                DialogParamsManager(context)
                    .setReadable(true)
                    .setParamListener(object :
                    DialogParamsManager.OnParamListener {
                    override fun onParamSelected(param: ScriptParam?) {
                        span.spanText = ScriptParamEnv.getParam(param?.getFullId())?.name
                        span.spanExtra = param?.getFullId()
                        refreshSpanText()
                        span.onHandleCallback?.invoke(span)
                    }
                }).show()
            }

            ScriptSpanHelper.ClickSpanType.Input -> {
                DialogCommonTextInput(context)
                    .setSingleLine(true)
                    .setTitle(
                        GlobalApp.getString(com.hive.i8n.R.string.sc_condition_edit_text_input_title)
                    ).setHint(GlobalApp.getString(com.hive.i8n.R.string.sc_condition_edit_text_input_hint))
                    .setEnableInputEmpty(true)
                    .setText(span.spanText ?: "")
                    .setOnCommonListener(object : DialogCommonTextInput.OnCommonListener {
                        override fun onSubmitted(content: String) {
                            val text=if(TextUtils.isEmpty(content)) ScriptConst.NONE_CHAR else content
                            span.spanText = text
                            refreshSpanText()
                            span.onHandleCallback?.invoke(span)
                        }

                        override fun onCanceled() {
                        }
                    }).show()
            }

            ScriptSpanHelper.ClickSpanType.Selector -> {
                val map = span.spanExtra as Map<String, String>
                PopMenuManager.instance.showMenu(
                    this,
                    this.touchPoint.x - 60 * GlobalApp.DP,
                    -this.measuredHeight + this.touchPoint.y,
                    map.map { it.value },
                    object : PopMenuManager.OnItemClickListener<String> {
                        override fun onItemClicked(
                            view: View,
                            data: String,
                            pos: Int
                        ) {
                            span.spanText = map.toList()[pos].second
                            refreshSpanText()
                            span.onHandleCallback?.invoke(span)
                        }
                    })
            }

            ScriptSpanHelper.ClickSpanType.ImageSelector -> {

                DialogImageManager(ScriptProvider.getViewContext())
                    .setSelectorMode(
                        true,
                        ScriptConst.Save_Script_Temp_Path + ScriptConst.Save_Image_Relative_Path,
                        object : DialogImageManager.OnImageSelectedListener {
                            override fun onSelected(
                                dialog: DialogImageManager,
                                paths: List<ScriptImageBean>?
                            ) {
                                dialog.dismiss()
                                span.spanExtra = paths?.map { ScriptHelper.copyToTempDir(it.path) }
                                span.spanIcon =
                                    BitmapUtils.getLocalBitmap(paths?.firstOrNull()?.path)
                                refreshSpanText()
                                span.onHandleCallback?.invoke(span)
                            }
                        })
                    .show()
            }

            ScriptSpanHelper.ClickSpanType.ColorSelector -> {
                DialogColorPicker(context).loadColor(span.spanExtra as Int)
                    .setOnColorPickListener(object :
                        DialogColorPicker.OnColorPickListener {
                        override fun onColorPicked(
                            dialog: DialogColorPicker,
                            color: Int
                        ) {
                            ScriptColorHelper.addColorToFirst(color)
                            dialog.dismiss()
                            //hex color
                            span.spanExtra = color
                            span.spanIcon =
                                ColorUtils.createColorBitmap(ColorUtils.toHexColor(span.spanExtra as Int))
                            refreshSpanText()
                            span.onHandleCallback?.invoke(span)
                        }
                    }).show()
            }

            ScriptSpanHelper.ClickSpanType.PermissionMultiSelector -> {
                val condition = span.spanExtra as? ConditionPermission ?: return@handleEvent
                DialogPermissionMultiSelector(context)
                    .setInitialSelected(condition.permissionList)
                    .setOnConfirmListener(object : DialogPermissionMultiSelector.OnConfirmListener {
                        override fun onConfirm(selected: List<String>) {
                            condition.permissionList.clear()
                            condition.permissionList.addAll(selected)
                            span.spanText = condition.getPermissionDisplayText()
                            refreshSpanText()
                            span.onHandleCallback?.invoke(span)
                        }
                    }).show()
            }
        }
    }
}