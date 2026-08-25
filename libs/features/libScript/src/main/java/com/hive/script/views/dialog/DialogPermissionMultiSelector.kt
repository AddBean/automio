// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.dialog

import android.content.Context
import android.view.LayoutInflater
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import com.hive.script.utils.ScriptHelper
import com.hive.script.R
import com.hive.script.views.widgets.BaseScriptDialog

/**
 * 权限多选弹框，用于 ConditionPermission 中勾选多个权限。
 * 使用 ScriptHelper.mPermissionMap 的 key 作为 fullKey。
 */
class DialogPermissionMultiSelector(context: Context) : BaseScriptDialog(context) {

    private var onConfirmListener: OnConfirmListener? = null
    private var initialSelected: Set<String> = emptySet()
    override fun initWindow() {
        post {
            initView()
        }
    }

    private fun initView() {
        val layoutContent = findViewById<LinearLayout>(R.id.layout_content)
        val tvConfirm = findViewById<TextView>(R.id.tv_btn_cancel)
        val tvTitle = findViewById<TextView>(R.id.tv_title)
        tvTitle?.setText(com.hive.i8n.R.string.sc_condition_permission_select_hint)
        tvConfirm?.setText(com.hive.i8n.R.string.sc_selector_submit)

        layoutContent?.removeAllViews()

        ScriptHelper.mPermissionMap.forEach { (fullKey, displayName) ->
            val view = LayoutInflater.from(context)
                .inflate(R.layout.dialog_permission_multi_item, layoutContent, false)
            val checkBox = view.findViewById<CheckBox>(R.id.checkbox)
            val tvName = view.findViewById<TextView>(R.id.tv_name)
            tvName.text = displayName
            checkBox.isChecked = fullKey in initialSelected
            view.tag = fullKey

            view.setOnClickListener {
                checkBox.isChecked = !checkBox.isChecked
            }
            layoutContent?.addView(view)
        }

        tvConfirm?.setOnClickListener {
            val selected = mutableListOf<String>()
            for (i in 0 until (layoutContent?.childCount ?: 0)) {
                val child = layoutContent?.getChildAt(i)
                val cb = child?.findViewById<CheckBox>(R.id.checkbox)
                val key = child?.tag as? String
                if (cb?.isChecked == true && key != null) {
                    selected.add(key)
                }
            }
            onConfirmListener?.onConfirm(selected)
            dismiss()
        }
    }

    fun setInitialSelected(selected: List<String>): DialogPermissionMultiSelector {
        initialSelected = selected.toSet()
        return this
    }

    fun setOnConfirmListener(listener: OnConfirmListener): DialogPermissionMultiSelector {
        onConfirmListener = listener
        return this
    }

    override fun enableFadeAnimation() = true

    override fun getWindowLayoutId() = R.layout.dialog_script_cmd_selector

    interface OnConfirmListener {
        fun onConfirm(selected: List<String>)
    }
}
