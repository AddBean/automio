// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.dialog

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import com.hive.script.condition.ConditionPermission
import com.hive.script.R
import com.hive.script.views.widgets.BaseScriptDialog

/**
 * 选择单个权限，用于添加到权限条件列表。
 * 使用 ScriptHelper.mPermissionMap 的 key 作为 fullKey。
 * 已选权限可传入过滤，避免重复添加。
 */
class DialogPermissionSelector(context: Context) : BaseScriptDialog(context) {

    private var onPermissionSelectedListener: OnPermissionSelectedListener? = null
    private var alreadySelectedKeys: Set<String> = emptySet()

    override fun initWindow() {
        val layoutContent = findViewById<ViewGroup>(R.id.layout_content)
        val tvBtnCancel = findViewById<TextView>(R.id.tv_btn_cancel)
        val tvTitle = findViewById<TextView>(R.id.tv_title)
        tvTitle?.setText(com.hive.i8n.R.string.sc_condition_permission_select_hint)
        tvBtnCancel?.setOnClickListener { dismiss() }
        layoutContent?.removeAllViews()
        val permissionPairs = ConditionPermission.getAllPermissionPairs()
            .filter { it.second !in alreadySelectedKeys }
        permissionPairs.forEach { (displayName, fullKey) ->
            val itemView = LayoutInflater.from(context)
                .inflate(R.layout.dialog_common_selector_item, null).apply {
                    setOnClickListener {
                        onPermissionSelectedListener?.onPermissionSelected(
                            this@DialogPermissionSelector,
                            displayName,
                            fullKey
                        )
                        dismiss()
                    }
                }
            itemView.findViewById<TextView>(R.id.btn_tv).text = displayName
            layoutContent?.addView(itemView)
        }
    }

    override fun enableFadeAnimation() = true

    override fun getWindowLayoutId() = R.layout.dialog_script_cmd_selector

    interface OnPermissionSelectedListener {
        fun onPermissionSelected(
            dialog: DialogPermissionSelector,
            displayName: String,
            fullKey: String
        )
    }
}
