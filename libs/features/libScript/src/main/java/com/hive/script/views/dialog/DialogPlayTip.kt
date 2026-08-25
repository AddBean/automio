// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.dialog

import android.content.Context
import android.view.View
import com.hive.script.R
import com.hive.script.base.ScriptCommandRoot
import com.hive.script.views.widgets.BaseScriptDialog
import com.hive.script.views.widgets.ScriptNumberView
import com.hive.script.views.widgets.ScriptTabSelectorView
import com.hive.views.widgets.NumberOptView
import com.hive.views.widgets.SelectorTabView

/**
 *
 * @author jiadou
 * @email 172111432@qq.com
 * @date 7/7/21
 */
class DialogPlayTip(context: Context?) : BaseScriptDialog(context) {

    private lateinit var mCommandRoot: ScriptCommandRoot

    var confirmFun: ((dialog: DialogPlayTip, startStep: Int, loopCount: Int) -> Unit)? = null

    var isInfiniteEnable = false

    var loopCount = 1

    var startStep = 1

    var onDismissFun: (() -> Unit?)? = null

    private var btn_submit: View? = null
    private var iv_close: View? = null
    private var play_count: ScriptNumberView? = null
    private var play_step: ScriptNumberView? = null
    private var play_type: ScriptTabSelectorView? = null

    override fun initWindow() {
        btn_submit = findViewById(R.id.btn_submit)
        iv_close = findViewById(R.id.iv_close)
        play_count = findViewById(R.id.play_count)
        play_step = findViewById(R.id.play_step)
        play_type = findViewById(R.id.play_type)
        iv_close?.setOnClickListener {
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

        play_step?.changedListener = object : NumberOptView.OnValueChangedListener {
            override fun onValueChanged(value: Int) {
                startStep = value
            }
        }

        updateNumberEnable()
    }


    private fun updateNumberEnable() {
        if (!isInfiniteEnable) {
            play_count?.isEnabled = true
            play_count?.alpha = 1f
        } else {
            play_count?.isEnabled = false
            play_count?.alpha = 0.3f
        }
    }

    override fun getWindowLayoutId() = R.layout.dialog_play_tip

    override fun isTouchOutsideDismissed() = false

    override fun enableFadeAnimation() = true


    override fun onDismiss() {
        super.onDismiss()
        onDismissFun?.invoke()
        onDismissFun = null
    }

    fun loadCmd(
        cmd: ScriptCommandRoot,
        confirm: (dialog: DialogPlayTip, startStep: Int, loopCount: Int) -> Unit
    ): DialogPlayTip {
        onDismissFun = null
        mCommandRoot = cmd
        confirmFun = confirm
        play_step?.setMaxNumber(mCommandRoot.commandQueue.size)
        btn_submit?.setOnClickListener {
            dismiss()
            onDismissFun = {
                val count = if (isInfiniteEnable) -1 else loopCount
                var step = startStep - 1
                if (step < 0) step = 0
                confirm.invoke(this, step, count)
            }
        }
        return this
    }
}