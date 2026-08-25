// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.tips

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.widget.TextView
import com.hive.script.R
import com.hive.script.views.widgets.BaseScriptTips
import java.util.Timer

@SuppressLint("ViewConstructor")
class ScriptWaitingForOperateView(
    context: Context
) :
    BaseScriptTips(context) {

    private var timer: Timer? = null
    private var countDown = -1
    private var hasCallbacked = false
    private var tvCountdown: TextView? = null

    override fun initWindow() {
        super.initWindow()
        setOptEnable(true)
        tvCountdown = findViewById(R.id.tvCountdown)
        timer = Timer()
        post {
            setCollapseEnabled(true)
        }
    }

    override fun getBgColor() = 0x00000000

    fun setCountDown(countDown: Int): ScriptWaitingForOperateView {
        this.countDown = countDown
        tvCountdown?.apply {
            text = "${countDown}S"
            visibility = if (countDown > 0) View.VISIBLE else View.GONE
        }
        post {
            if (countDown >= 0) {
                val c = countDown - 1
                if (c == 0) {
                    if (isShown) {
                        hasCallbacked = true
                        onDismissListener?.invoke()
                        dismiss()
                    }
                } else {
                    postDelayed({
                        setCountDown(c)
                    }, 1000)
                }
            }
        }
        return this
    }

    private var onDismissListener: (() -> Unit)? = null

    fun setOnDismissListener(listener: () -> Unit): ScriptWaitingForOperateView {
        onDismissListener = listener
        return this
    }

    override fun onDismiss() {
        super.onDismiss()
        timer?.cancel()
        onDismissListener?.invoke()
    }
}