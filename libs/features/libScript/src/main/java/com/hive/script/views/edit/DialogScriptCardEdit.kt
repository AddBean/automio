// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.edit

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import com.hive.extension.visibleOrGone
import com.hive.script.R
import com.hive.script.views.edit.card.edit.BaseCommandEditCard
import com.hive.script.views.widgets.BaseScriptDialog


class DialogScriptCardEdit(context: Context) : BaseScriptDialog(context) {

    private var onFinished: (() -> Unit)? = null

    private var onConfirmed: ((dialog: DialogScriptCardEdit) -> Unit)? = null

    private var editView: BaseCommandEditCard? = null

    private var btnCancel: View? = null
    private var btnManager: View? = null
    private var btnSubmit: View? = null
    private var layoutContent: ViewGroup? = null
    private var tvTitle: TextView? = null

    override fun initWindow() {
        btnCancel = findViewById(R.id.btnCancel)
        btnManager = findViewById(R.id.btnManager)
        btnSubmit = findViewById(R.id.btnSubmit)
        layoutContent = findViewById(R.id.layoutContent)
        tvTitle = findViewById(R.id.tvTitle)
        post {
            onFinished?.invoke()
            btnManager?.setOnClickListener {
                editView?.postEvent(ScriptMenuEditHelper.ClickType.MANGER_SUB_TASK)
                dismiss()
            }
            btnCancel?.setOnClickListener {
                dismiss()
            }
            btnSubmit?.setOnClickListener {
                onConfirmed?.invoke(this)
            }
        }
    }

    fun setTitle(title: String): DialogScriptCardEdit {
        tvTitle?.text = title
        return this
    }

    fun setEdtView(view: BaseCommandEditCard): DialogScriptCardEdit {
        editView = view
        editView?.onCommandBinded = {
            btnManager?.visibleOrGone(it?.isGroupCommand() == true)
        }
        layoutContent?.addView(
            view,
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        )
        return this
    }

    fun setCommonDelay(delayEdit: Boolean): DialogScriptCardEdit {
        editView?.setDelayEdit(delayEdit)
        return this
    }

    fun setOnInflateFinished(onFinished: (() -> Unit)?): DialogScriptCardEdit {
        this.onFinished = onFinished
        return this
    }

    fun setOnConfirmClicked(onConfirmed: ((dialog: DialogScriptCardEdit) -> Unit)?): DialogScriptCardEdit {
        this.onConfirmed = onConfirmed
        return this
    }

    fun setOnDismissListener(onDismiss: () -> Unit): DialogScriptCardEdit {
        setOnDismissListener(object : OnDismissListener {
            override fun onDismiss() {
                onDismiss.invoke()
            }
        })
        return this
    }

    fun dismissNoNotify(onDismiss: () -> Unit) {
        super.dismiss(onDismiss, false)
    }

    override fun enableFadeAnimation() = true


    override fun getWindowLayoutId() = R.layout.dialog_script_card_edit
}