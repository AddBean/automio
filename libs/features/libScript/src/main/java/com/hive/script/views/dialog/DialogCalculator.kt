// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.dialog

import android.content.Context
import android.view.View
import android.widget.FrameLayout
import com.hive.script.R
import com.hive.script.views.calculate.ScriptCalculatorView
import com.hive.script.views.widgets.BaseScriptDialog
import com.hive.utils.extends.isLandscape

class DialogCalculator(context: Context?) : BaseScriptDialog(context) {
    private var btnCancel: View? = null
    private var btnSubmit: View? = null
    private var calculatorView: ScriptCalculatorView? = null
    private var ivClose: View? = null
    override fun initWindow() {
        btnCancel = findViewById(R.id.btnCancel)
        btnSubmit = findViewById(R.id.btnSubmit)
        calculatorView = findViewById(R.id.calculatorView)
        ivClose = findViewById(R.id.ivClose)
        ivClose?.setOnClickListener { dismiss() }
        btnCancel?.setOnClickListener { dismiss() }
    }

    override fun enableFadeAnimation() = true

    override fun getWindowLayoutId() = R.layout.dialog_calculator

    fun setExpression(expression: String): DialogCalculator {
        post {
            calculatorView?.expression = expression
        }

        return this
    }

    override fun getWidthByOrientation(): Int {
        return if (context.isLandscape())
            240 * DP
        else FrameLayout.LayoutParams.MATCH_PARENT
    }

    fun setSubmitListener(onSubmit: (expr: String) -> Unit): DialogCalculator {
        btnSubmit?.setOnClickListener {
            onSubmit(calculatorView?.expression ?: "")
            dismiss()
        }
        return this
    }

    companion object {
        fun show(context: Context, expression: String, onSubmit: (expr: String) -> Unit) {
            DialogCalculator(context)
                .setExpression(expression)
                .setSubmitListener({
                    onSubmit(it)
                }).show()
        }
    }
}