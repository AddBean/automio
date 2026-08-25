// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.views

import android.app.Dialog
import android.content.Context
import android.widget.TextView
import com.hive.app.script.R
import com.hive.i8n.R as i8nR

class ConfirmDialog(context: Context) : Dialog(context, com.hive.views.R.style.base_dialog) {

    private var btnCancel: TextView? = null
    private var btnConfirm: TextView? = null
    private var tvMessage: TextView? = null
    private var tvTitle: TextView? = null

    private var onConfirmed: ((dialog: Dialog) -> Unit)? = null

    private var onCancel: ((dialog: Dialog) -> Unit)? = null

    init {
        setContentView(R.layout.layout_confirm_dialog)
        btnCancel = findViewById(R.id.btnCancel)
        btnConfirm = findViewById(R.id.btnConfirm)
        tvMessage = findViewById(R.id.tvMessage)
        tvTitle = findViewById(R.id.tvTitle)
        btnCancel?.setOnClickListener {
            onCancel?.invoke(this)
            if (onCancel == null) dismiss()
        }
        btnConfirm?.setOnClickListener {
            onConfirmed?.invoke(this)
        }
    }

    fun setTitle(title: String): ConfirmDialog {
        tvTitle?.text = title
        return this
    }

    fun setContent(content: String): ConfirmDialog {
        tvMessage?.text = content
        return this
    }

    fun setCancel(content: String): ConfirmDialog {
        btnCancel?.text = content
        return this
    }

    fun setConfirm(content: String): ConfirmDialog {
        btnConfirm?.text = content
        return this
    }

    fun setCancelListener(onCancel: ((dialog: Dialog) -> Unit)): ConfirmDialog {
        this.onCancel = onCancel
        return this
    }

    fun show(onConfirmed: (dialog: Dialog) -> Unit) {
        this.onConfirmed = onConfirmed
        this.onCancel = onCancel
        this.show()
    }
}