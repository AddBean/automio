// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.dialog

import android.content.Context
import android.view.View
import android.widget.TextView
import com.hive.script.R
import com.hive.script.views.widgets.BaseScriptDialog

/**
 *
 * @author jiadou
 * @date 6/11/21
 */
class DialogScriptLoading(context: Context?) : BaseScriptDialog(context) {

    var onDismissCallback: (() -> Unit)? = null

    private var iv_close: View? = null

    private var tvLoading:TextView? = null

    override fun initWindow() {
        iv_close = findViewById(R.id.iv_close)
        tvLoading = findViewById(R.id.tvLoading)
        iv_close?.setOnClickListener { dismiss() }
    }

    fun setCloseEnable(enable: Boolean): DialogScriptLoading {
        iv_close?.visibility = if (enable) View.VISIBLE else View.GONE
        return this
    }

    fun setMessage(message: String): DialogScriptLoading {
        tvLoading?.text = message
        return this
    }

    override fun enableFadeAnimation() = true

    override fun isTouchOutsideDismissed() = false

    override fun onDismiss() {
        super.onDismiss()
        onDismissCallback?.invoke()
    }

    override fun getWindowLayoutId() = R.layout.sc_dialog_loading

}