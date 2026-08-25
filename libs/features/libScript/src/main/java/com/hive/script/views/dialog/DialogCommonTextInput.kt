// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.dialog

import android.content.Context
import android.text.TextUtils
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import com.hive.extension.setHeight
import com.hive.script.R
import com.hive.script.views.widgets.BaseScriptDialog
import com.hive.script.views.widgets.ScriptSpanParamLayout
import com.hive.utils.extends.dp
import com.hive.utils.system.CommonUtils
import com.hive.views.widgets.CommonToast

/**
 *
 * @author jiadou
 * @date 6/11/21
 */
class DialogCommonTextInput(context: Context?) : BaseScriptDialog(context) {
    private var actionTitle: String? = null

    private var actionList: List<ScriptSpanParamLayout.QuickAction>? = null

    private var commonListener: OnCommonListener? = null

    private var content: String? = null

    private var isEnabledInputEmpty = false

    private var checkFunctionInner: ((content: String?) -> Unit)? = null

    private var btn_submit: View? = null
    private var edit_text: ScriptSpanParamLayout? = null
    private var iv_close: View? = null
    private var iv_expand: ImageView? = null
    private var tv_title: TextView? = null

    private var isExpanded = false
    private var originalDialogParams: RelativeLayout.LayoutParams? = null

    override fun initWindow() {
        iv_close = findViewById(R.id.iv_close)
        iv_expand = findViewById(R.id.iv_expand)
        btn_submit = findViewById(R.id.btn_submit)
        edit_text = findViewById(R.id.edit_text)
        tv_title = findViewById(R.id.tv_title)
        iv_close?.setOnClickListener {
            dismiss()
            commonListener?.onCanceled()
        }
        iv_expand?.setOnClickListener {
            toggleExpand()
        }
        btn_submit?.setOnClickListener {
            try {
                checkInput()
                commonListener?.onSubmitted(content ?: "")
                dismiss()
            } catch (e: java.lang.Exception) {
                CommonToast.show(e.message)
            }

        }
        edit_text?.requestFocus()
        edit_text?.post {
            CommonUtils.openKeyboard(edit_text)
            if (actionTitle != null && actionList != null) {
                edit_text?.setQuickAction(actionTitle!!, actionList!!)
            }
        }
    }

    fun setTitle(title: String): DialogCommonTextInput {
        tv_title?.text = title
        return this
    }

    fun setQuickAction(
        title: String, actionList: List<ScriptSpanParamLayout.QuickAction>
    ): DialogCommonTextInput {
        actionTitle = title
        this.actionList = actionList
        return this
    }

    fun setHint(hint: String): DialogCommonTextInput {
        edit_text?.setHint(hint)
        return this
    }

    fun setText(text: String): DialogCommonTextInput {
        edit_text?.setText(text)
        return this
    }

    fun setOnCommonListener(callback: OnCommonListener): DialogCommonTextInput {
        commonListener = callback
        return this
    }

    fun setActionMenuList(actionMenuList: MutableList<ScriptSpanParamLayout.ActionMenuType>): DialogCommonTextInput {
        edit_text?.setActionMenuList(actionMenuList)
        return this
    }

    fun setSingleLine(enable: Boolean): DialogCommonTextInput {
        edit_text?.setSingleLine(enable)
        if (enable) {
            edit_text?.layoutParams?.height = 96.dp()
            edit_text?.gravity = android.view.Gravity.CENTER_VERTICAL
        } else {
            if (isExpanded) {
                edit_text?.layoutParams?.height = 520.dp()
            } else {
                edit_text?.layoutParams?.height = 240.dp()
            }
            edit_text?.gravity = android.view.Gravity.TOP
        }
        return this
    }

    fun setEnableInputEmpty(enable: Boolean): DialogCommonTextInput {
        isEnabledInputEmpty = enable
        return this
    }

    fun changeEditHeight(height: Int): DialogCommonTextInput {
        edit_text?.layoutParams?.height = height
        return this
    }

    private fun checkInput() {
        content = edit_text?.getText()
        if (!isEnabledInputEmpty) {
            if (TextUtils.isEmpty(content)) {
                throw Exception(getString(com.hive.i8n.R.string.sc_check_scheme_input_check_empty))
            }
        }
        checkFunctionInner?.invoke(content)
    }

    private fun toggleExpand() {
        edit_text ?: return
        if (isExpanded) {
            shrinkDialog()
        } else {
            expandDialog()
        }
        isExpanded = !isExpanded
    }

    private fun expandDialog() {
        edit_text?.setHeight(600.dp())
        iv_expand?.setImageResource(R.drawable.sc_icon_min)
        iv_expand?.contentDescription = context?.getString(com.hive.i8n.R.string.base_collapse)
    }

    private fun shrinkDialog() {
        edit_text?.setHeight(240.dp())
        iv_expand?.setImageResource(R.drawable.sc_icon_max)
        iv_expand?.contentDescription = context?.getString(com.hive.i8n.R.string.base_expand)
    }

    fun setCheckFunction(checkFunction: (content: String?) -> Unit): DialogCommonTextInput {
        this.checkFunctionInner = checkFunction
        return this
    }

    override fun enableFadeAnimation() = true

    override fun getWindowLayoutId() = R.layout.dialog_common_text_input


    interface OnCommonListener {
        fun onSubmitted(content: String)

        fun onCanceled()
    }
}