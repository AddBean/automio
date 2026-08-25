// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.dialog

import android.content.Context
import android.graphics.Rect
import android.text.TextUtils
import android.view.View
import android.view.accessibility.AccessibilityNodeInfo
import com.hive.script.R
import com.hive.script.cmd.CmdInput
import com.hive.script.driver.ScriptEventHelper
import com.hive.script.views.manager.ScriptInsertManager
import com.hive.script.views.widgets.BaseScriptDialog
import com.hive.script.views.widgets.ScriptSpanParamLayout
import com.hive.script.views.widgets.ScriptTabSelectorView
import com.hive.script.views.widgets.ScriptValueView
import com.hive.utils.GlobalApp
import com.hive.utils.system.CommonUtils
import com.hive.views.widgets.CommonToast
import com.hive.views.widgets.SelectorTabView

/**
 *
 * @author jiadou
 * @email 172111432@qq.com
 * @date 6/11/21
 */
class DialogCmdInputEdit(context: Context?) : BaseScriptDialog(context) {

    private var nodeInfo: AccessibilityNodeInfo? = null

    private var content: String? = null

    var confirmListener: OnConfirmListener? = null

    private var btn_submit: View? = null
    private var edit_text: ScriptSpanParamLayout? = null
    private var iv_close: View? = null
    private var type_selector: ScriptTabSelectorView? = null
    private var switch_anim_input: ScriptTabSelectorView? = null
    private var edit_target_index: ScriptValueView? = null
    private var targetIndex = 1

    override fun initWindow() {
        iv_close = findViewById(R.id.iv_close)
        btn_submit = findViewById(R.id.btn_submit)
        edit_text = findViewById(R.id.edit_text)
        type_selector = findViewById(R.id.type_selector)
        switch_anim_input = findViewById(R.id.switch_anim_input)
        edit_target_index = findViewById(R.id.edit_target_index)
        iv_close?.setOnClickListener {
            dismiss()
            ScriptInsertManager.notifyInsertDismiss()
        }
        edit_target_index?.onMaskClickListener = View.OnClickListener {
            DialogCommonTextInput(context)
                .setSingleLine(true)
                .setTitle(getString(com.hive.i8n.R.string.sc_input_target_index_title))
                .setHint(getString(com.hive.i8n.R.string.sc_input_target_index_hint))
                .setText(targetIndex.toString())
                .setCheckFunction { content ->
                    val value = content?.toIntOrNull()
                    require(value != null && value > 0) {
                        GlobalApp.getString(com.hive.i8n.R.string.tool_input_error_target_index_invalid)
                    }
                }
                .setOnCommonListener(object : DialogCommonTextInput.OnCommonListener {
                    override fun onSubmitted(content: String) {
                        targetIndex = content.toIntOrNull()?.takeIf { it > 0 } ?: 1
                        edit_target_index?.setValue(targetIndex.toString())
                    }

                    override fun onCanceled() {
                    }
                }).show()
        }
        btn_submit?.setOnClickListener {
            try {
                checkInput()
                confirmListener?.onConfirmClicked(
                    this,
                    CmdInput.createCommand(
                        content ?: "",
                        nodeInfo?.viewIdResourceName,
                        type_selector?.curValue,
                        switch_anim_input?.curValue == "1",
                        targetIndex,
                    )
                )
                dismiss()
            } catch (e: java.lang.Exception) {
                CommonToast.show(e.message)
            }

        }
        type_selector?.onTabSelectedChangedListener =
            object : SelectorTabView.OnTabSelectedChangedListener {
                override fun onSelectedChanged(p: Pair<String?, String?>?) {
                    type_selector?.curValue = p!!.second!!
                }
        }
        type_selector?.setValue("full")
        switch_anim_input?.setValue("0")
        targetIndex = resolveTargetIndex(nodeInfo)
        edit_target_index?.setValue(targetIndex.toString())
        switch_anim_input?.onTabSelectedChangedListener =
            object : SelectorTabView.OnTabSelectedChangedListener {
                override fun onSelectedChanged(p: Pair<String?, String?>?) {
                    switch_anim_input?.curValue = p?.second ?: "0"
                }
            }
        edit_text?.requestFocus()
        edit_text?.post {
            CommonUtils.openKeyboard(edit_text)
        }
    }

    fun setOnConfirmListener(listener: OnConfirmListener): DialogCmdInputEdit {
        confirmListener = listener
        return this
    }

    fun bindNodeInfo(nodeInfo: AccessibilityNodeInfo?): DialogCmdInputEdit {
        this.nodeInfo = nodeInfo
        this.targetIndex = resolveTargetIndex(nodeInfo)
        return this
    }

    private fun resolveTargetIndex(nodeInfo: AccessibilityNodeInfo?): Int {
        if (nodeInfo == null) {
            return 1
        }
        val nodes = ScriptEventHelper.get().performFindEditText(nodeInfo.viewIdResourceName, null)
            ?: return 1
        val targetRect = Rect().also { nodeInfo.getBoundsInScreen(it) }
        val index = nodes.indexOfFirst { target ->
            val rect = Rect()
            target.getBoundsInScreen(rect)
            rect == targetRect
        }
        return if (index >= 0) index + 1 else 1
    }

    private fun checkInput() {
        content = edit_text?.getText()
        if (TextUtils.isEmpty(content)) {
            throw Exception(getString(com.hive.i8n.R.string.sc_check_scheme_input_check_empty))
        }
    }

    override fun onTouchDismiss() {
        ScriptInsertManager.notifyInsertDismiss()
    }

    interface OnConfirmListener {
        fun onConfirmClicked(dialog: DialogCmdInputEdit, cmd: CmdInput)

        fun onDismissed()
    }

    override fun getWindowLayoutId() = R.layout.dialog_input_edit
}
