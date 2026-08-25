// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.ui.skill

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.hive.app.script.R

class DialogSkillCreateMode : DialogFragment() {

    private var onManualCreate: (() -> Unit)? = null
    private var onAiCreate: (() -> Unit)? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
        val view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_skill_create_mode, null)
        dialog.setContentView(view)
        dialog.setCanceledOnTouchOutside(true)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setWindowAnimations(R.style.BottomSlideDialogAnimation)

        view.findViewById<View>(R.id.btn_ai_create)?.setOnClickListener {
            dismissAllowingStateLoss()
            onAiCreate?.invoke()
        }
        view.findViewById<View>(R.id.btn_manual_create)?.setOnClickListener {
            dismissAllowingStateLoss()
            onManualCreate?.invoke()
        }
        view.findViewById<View>(R.id.mask_view)?.setOnClickListener {
            dismissAllowingStateLoss()
        }
        return dialog
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            setGravity(Gravity.BOTTOM)
        }
    }

    companion object {
        fun show(
            fragmentManager: androidx.fragment.app.FragmentManager,
            onAiCreate: () -> Unit,
            onManualCreate: () -> Unit
        ) {
            DialogSkillCreateMode().apply {
                this.onAiCreate = onAiCreate
                this.onManualCreate = onManualCreate
            }.show(fragmentManager, "DialogSkillCreateMode")
        }
    }
}
