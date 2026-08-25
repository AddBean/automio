// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.dialog

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.hive.extension.visibleOrGone
import com.hive.script.R
import com.hive.script.views.widgets.BaseScriptDialog

/**
 *
 * @author jiadou
 * @date 6/11/21
 */
class DialogScriptAlert(context: Context?) : BaseScriptDialog(context) {

    private var listener: OnDialogEventListener? = null
    private var layout_content: ViewGroup? = null
    private var tv_btn_cancel: TextView? = null
    private var tv_btn_submit: TextView? = null
    private var tv_content: TextView? = null
    private var tv_title: TextView? = null

    override fun initWindow() {
        layout_content = findViewById(R.id.layout_content)
        tv_title = findViewById(R.id.tv_title)
        tv_content = findViewById(R.id.tv_content)
        tv_btn_submit = findViewById(R.id.tv_btn_submit)
        tv_btn_cancel = findViewById(R.id.tv_btn_cancel)

        tv_btn_submit?.setOnClickListener {
            listener?.onClickEvent(this, false)
        }
        tv_btn_cancel?.setOnClickListener {
            listener?.onClickEvent(this, true)
        }
    }

    fun setTitle(id: Int): DialogScriptAlert {
        tv_title?.setText(id)
        return this
    }

    fun setContent(id: Int): DialogScriptAlert {
        tv_content?.setText(id)
        tv_content?.visibility = View.VISIBLE
        return this
    }

    fun setConfirmText(id: Int): DialogScriptAlert {
        tv_btn_submit?.setText(id)
        return this
    }

    fun setCancelText(id: Int): DialogScriptAlert {
        tv_btn_cancel?.setText(id)
        return this
    }

    fun setTitle(txt: String): DialogScriptAlert {
        tv_title?.text = txt
        return this
    }

    fun setContentLayout(layoutId: Int): DialogScriptAlert {
        val view = View.inflate(context, layoutId, null)
        layout_content?.removeAllViews()
        layout_content?.addView(view)
        return this
    }

    fun setContent(txt: String): DialogScriptAlert {
        tv_content?.text = txt
        tv_content?.visibility = View.VISIBLE
        return this
    }

    fun setContentGravity(gravity: Int): DialogScriptAlert {
        tv_content?.gravity = gravity
        return this
    }

    fun setConfirmText(txt: String): DialogScriptAlert {
        tv_btn_submit?.text = txt
        return this
    }

    fun setCancelText(txt: String): DialogScriptAlert {
        tv_btn_cancel?.text = txt
        return this
    }

    fun setConfirmEnable(enable: Boolean): DialogScriptAlert {
        tv_btn_submit?.visibleOrGone(enable)
        return this
    }

    fun setCancelEnable(enable: Boolean): DialogScriptAlert {
        tv_btn_cancel?.visibleOrGone(enable)
        return this
    }

    fun setOnDialogEventListener(ls: OnDialogEventListener): DialogScriptAlert {
        listener = ls
        return this
    }

    override fun enableFadeAnimation() = true

    override fun getWindowLayoutId() = R.layout.sc_dialog_alert

    interface OnDialogEventListener {
        fun onClickEvent(dialog: DialogScriptAlert, isCancel: Boolean)
    }
}