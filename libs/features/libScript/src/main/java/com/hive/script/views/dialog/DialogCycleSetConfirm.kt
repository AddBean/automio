// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.dialog

import android.content.Context
import android.view.View
import com.hive.script.R
import com.hive.script.views.widgets.BaseScriptDialog
import com.hive.script.views.widgets.ScriptNumberView
import com.hive.script.views.widgets.ScriptTabSelectorView
import com.hive.views.widgets.NumberOptView
import com.hive.views.widgets.SelectorTabView

/**
 *
 * @author jiadou
 * @date 7/7/21
 */
class DialogCycleSetConfirm(context: Context?) : BaseScriptDialog(context) {

    var confirmFun: ((dialog: DialogCycleSetConfirm, loopCount: Int) -> Unit)? = null

    var isInfiniteEnable = false

    var loopCount = 1

    var onDismissFun: (() -> Unit?)? = null

    private var btnCancel: View? = null
    private var btnSubmit: View? = null
    private var iv_close: View? = null
    private var play_count: ScriptNumberView? = null
    private var play_type: ScriptTabSelectorView? = null

    override fun initWindow() {
        iv_close = findViewById(R.id.iv_close)
        btnCancel = findViewById(R.id.btnCancel)
        btnSubmit = findViewById(R.id.btnSubmit)
        play_count = findViewById(R.id.play_count)
        play_type = findViewById(R.id.play_type)
        iv_close?.setOnClickListener {
            dismiss()
        }
        btnCancel?.setOnClickListener {
            dismiss()
        }
        play_type?.onTabSelectedChangedListener =
            object : SelectorTabView.OnTabSelectedChangedListener {
                override fun onSelectedChanged(p: Pair<String?, String?>?) {
                    isInfiniteEnable = ((p?.second ?: "0") == "1")
                    updateNumberEnable()
                }
            }
        play_count?.changedListener = object : NumberOptView.OnValueChangedListener {
            override fun onValueChanged(value: Int) {
                loopCount = value
            }
        }

        btnSubmit?.setOnClickListener {
            confirmFun?.invoke(this@DialogCycleSetConfirm, if (isInfiniteEnable) 0 else loopCount)
            dismiss()
        }

        updateNumberEnable()
    }

    private fun updateNumberEnable() {
        play_count?.isEnabled = !isInfiniteEnable
        play_count?.alpha = if (isInfiniteEnable) 0.4f else 1f
    }

    override fun getWindowLayoutId() = R.layout.dialog_cycle_setting

    override fun isTouchOutsideDismissed() = false


    override fun onDismiss() {
        super.onDismiss()
        onDismissFun?.invoke()
        onDismissFun = null
    }

//    fun loadCmd(cmd: ScriptCommandRoot, confirm: (dialog: DialogCycleSetConfirm, startStep: Int, loopCount: Int) -> Unit): DialogCycleSetConfirm {
//        onDismissFun = null
//        mCommandRoot = cmd
//        confirmFun = confirm
//        btn_submit?.setOnClickListener {
//            dismiss()
//            onDismissFun = {
//                var count = if (isInfiniteEnable) -1 else loopCount
//                var step = startStep - 1
//                if (step < 0) step = 0
//                confirm.invoke(this, step, count)
//            }
//        }
//        return this
//    }
}