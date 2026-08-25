// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.dialog

import android.content.Context
import android.view.LayoutInflater
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import com.hive.script.R
import com.hive.script.utils.bundle.WorkflowBundleInstaller
import com.hive.script.views.widgets.BaseScriptDialog

/**
 * bundle 导入冲突处理弹框：
 * - 勾选：覆盖（overwrite）
 * - 取消勾选：忽略（skip）
 */
class DialogBundleImportConflictConfirm(context: Context?) : BaseScriptDialog(context) {

    private var pending: WorkflowBundleInstaller.PendingInstall? = null
    private var conflicts: List<WorkflowBundleInstaller.BundleConflict> = emptyList()
    private var onConfirmListener: OnConfirmListener? = null

    private var layoutContent: LinearLayout? = null
    private var tvBtnCancel: TextView? = null
    private var tvBtnConfirm: TextView? = null
    private var tvTitle: TextView? = null
    private var tvHint: TextView? = null

    private val selectedActions = LinkedHashMap<String, WorkflowBundleInstaller.ConflictAction>()

    override fun initWindow() {
        layoutContent = findViewById(R.id.layout_content)
        tvBtnCancel = findViewById(R.id.tv_btn_cancel)
        tvBtnConfirm = findViewById(R.id.tv_btn_export)
        tvTitle = findViewById(R.id.tv_title)
        tvHint = findViewById(R.id.tv_hint)

        tvTitle?.text = context?.getString(com.hive.i8n.R.string.sc_bundle_import_conflict_title)
        tvHint?.text = context?.getString(com.hive.i8n.R.string.sc_bundle_import_conflict_hint)
        tvBtnConfirm?.text = context?.getString(com.hive.i8n.R.string.sc_bundle_import_conflict_continue)

        tvBtnCancel?.setOnClickListener {
            dismiss()
            pending?.let { onConfirmListener?.onCancel(it) }
        }
        tvBtnConfirm?.setOnClickListener {
            dismiss()
            val p = pending ?: return@setOnClickListener
            onConfirmListener?.onConfirm(p, selectedActions.toMap())
        }
    }

    fun setPending(
        pending: WorkflowBundleInstaller.PendingInstall,
        conflicts: List<WorkflowBundleInstaller.BundleConflict>
    ): DialogBundleImportConflictConfirm {
        this.pending = pending
        this.conflicts = conflicts
        selectedActions.clear()
        conflicts.forEach { c -> selectedActions[c.key] = c.defaultAction }
        refreshContent()
        return this
    }

    fun setOnConfirmListener(listener: OnConfirmListener): DialogBundleImportConflictConfirm {
        onConfirmListener = listener
        return this
    }

    private fun refreshContent() {
        val content = layoutContent ?: return
        content.removeAllViews()
        conflicts.forEach { c ->
            addConflictItem(content, c)
        }
    }

    private fun addConflictItem(parent: LinearLayout, conflict: WorkflowBundleInstaller.BundleConflict) {
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_export_dependency_item, parent, false)
        val checkBox = view.findViewById<CheckBox>(R.id.checkbox)
        val tvName = view.findViewById<TextView>(R.id.tv_name)
        val tvSubtitle = view.findViewById<TextView>(R.id.tv_subtitle)

        tvName.text = when (conflict.type) {
            "workflow" -> context?.getString(
                com.hive.i8n.R.string.sc_bundle_import_conflict_item_workflow,
                conflict.name
            ).orEmpty()
            "tool" -> context?.getString(
                com.hive.i8n.R.string.sc_bundle_import_conflict_item_tool,
                conflict.name
            ).orEmpty()
            else -> context?.getString(
                com.hive.i8n.R.string.sc_bundle_import_conflict_item_skill,
                conflict.name
            ).orEmpty()
        }

        val localVer = conflict.existingVersion ?: "-"
        val incomingVer = conflict.incomingVersion ?: "-"
        tvSubtitle.text = context?.getString(
            com.hive.i8n.R.string.sc_bundle_import_conflict_subtitle_versions,
            localVer,
            incomingVer
        ).orEmpty()

        checkBox.isChecked = (selectedActions[conflict.key] ?: conflict.defaultAction) ==
            WorkflowBundleInstaller.ConflictAction.OVERWRITE

        view.setOnClickListener {
            checkBox.isChecked = !checkBox.isChecked
            selectedActions[conflict.key] = if (checkBox.isChecked) {
                WorkflowBundleInstaller.ConflictAction.OVERWRITE
            } else {
                WorkflowBundleInstaller.ConflictAction.SKIP
            }
            view.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
        }

        parent.addView(view)
    }

    override fun getWindowLayoutId(): Int = R.layout.dialog_export_dependency_confirm

    interface OnConfirmListener {
        fun onConfirm(
            pending: WorkflowBundleInstaller.PendingInstall,
            actions: Map<String, WorkflowBundleInstaller.ConflictAction>
        )

        fun onCancel(pending: WorkflowBundleInstaller.PendingInstall)
    }
}

