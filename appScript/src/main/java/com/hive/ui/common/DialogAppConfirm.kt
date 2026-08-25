// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.ui.common

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import com.hive.app.script.R

class DialogAppConfirm : DialogFragment() {

    private var title: String = ""
    private var content: String = ""
    private var cancelText: String? = null
    private var confirmText: String? = null
    private var onConfirm: (() -> Unit)? = null
    private var onCancel: (() -> Unit)? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = Dialog(requireContext())
        val view = LayoutInflater.from(requireContext()).inflate(R.layout.sample_dialog, null)
        dialog.setContentView(view)
        dialog.setCanceledOnTouchOutside(true)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val tvTitle = view.findViewById<TextView>(R.id.tv_title)
        val tvContent = view.findViewById<TextView>(R.id.tv_content)
        val btnCancel = view.findViewById<TextView>(R.id.tv_btn_cancel)
        val btnSubmit = view.findViewById<TextView>(R.id.tv_btn_submit)

        tvTitle.text = title
        tvContent.text = content

        cancelText?.let { btnCancel.text = it }
        confirmText?.let { btnSubmit.text = it }

        btnCancel.setOnClickListener {
            dismissAllowingStateLoss()
            onCancel?.invoke()
        }
        btnSubmit.setOnClickListener {
            dismissAllowingStateLoss()
            onConfirm?.invoke()
        }

        return dialog
    }

    companion object {
        fun show(
            fragment: Fragment,
            title: String,
            content: String,
            cancelText: String? = null,
            confirmText: String? = null,
            onCancel: (() -> Unit)? = null,
            onConfirm: (() -> Unit)? = null
        ) {
            DialogAppConfirm().apply {
                this.title = title
                this.content = content
                this.cancelText = cancelText
                this.confirmText = confirmText
                this.onCancel = onCancel
                this.onConfirm = onConfirm
            }.show(fragment.parentFragmentManager, "DialogAppConfirm")
        }
    }
}

