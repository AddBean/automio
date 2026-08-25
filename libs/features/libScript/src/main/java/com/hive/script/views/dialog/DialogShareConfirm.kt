// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.dialog

import android.content.Context
import android.text.TextUtils
import android.view.View
import android.view.ViewGroup
import com.hive.extension.visibleOrGone
import com.hive.script.R
import com.hive.script.views.widgets.BaseScriptDialog
import com.hive.script.views.widgets.ScriptTabSelectorView
import com.hive.script.views.widgets.ScriptValueView
import com.hive.utils.extends.string
import com.hive.views.widgets.CommonToast
import com.hive.views.widgets.SelectorTabView

/**
 *
 * @author jiadou
 * @email 172111432@qq.com
 * @date 6/11/21
 */
class DialogShareConfirm(context: Context?) : BaseScriptDialog(context) {

    private var ctlPwd: String? = null

    private var ctlHour = -1

    private var ctlRun = true

    private var ctlView = true

    private var ctlEdit = false

    private var ctlShare = false

    private var ctlCloud = false

    private var shareConfirmListener: OnShareConfirmListener? = null

    private var enableEncrypt = false

    private var btn_submit: View? = null
    private var ctlCloudView: ScriptTabSelectorView? = null
    private var ctlEditView: ScriptTabSelectorView? = null
    private var ctlPwdView: ScriptValueView? = null
    private var ctlRunView: ScriptTabSelectorView? = null
    private var ctlShareView: ScriptTabSelectorView? = null
    private var ctlTimeView: ScriptValueView? = null
    private var ctlViewView: ScriptTabSelectorView? = null
    private var encryptLayout: ViewGroup? = null
    private var encryptSwitch: ScriptTabSelectorView? = null
    private var iv_close: View? = null

    override fun initWindow() {
        btn_submit = findViewById(R.id.btn_submit)
        ctlCloudView = findViewById(R.id.ctlCloudView)
        ctlEditView = findViewById(R.id.ctlEditView)
        ctlPwdView = findViewById(R.id.ctlPwdView)
        ctlRunView = findViewById(R.id.ctlRunView)
        ctlShareView = findViewById(R.id.ctlShareView)
        ctlTimeView = findViewById(R.id.ctlTimeView)
        ctlViewView = findViewById(R.id.ctlViewView)
        encryptLayout = findViewById(R.id.encryptLayout)
        encryptSwitch = findViewById(R.id.encryptSwitch)
        iv_close = findViewById(R.id.iv_close)

        iv_close?.setOnClickListener {
            dismiss()
        }
        btn_submit?.setOnClickListener {
            try {
                checkInput(ctlPwd)
                val expireTime = if (ctlHour > 0) {
                    System.currentTimeMillis() + ctlHour * 60 * 60 * 1000
                } else {
                    -1
                }
                shareConfirmListener?.onShareConfirm(
                    this,
                    enableEncrypt,
                    ctlPwd,
                    getControlValue(),
                    expireTime
                )
            } catch (e: java.lang.Exception) {
                CommonToast.show(e.message)
            }
        }
        encryptSwitch?.onTabSelectedChangedListener = object :
            SelectorTabView.OnTabSelectedChangedListener {

            override fun onSelectedChanged(p: Pair<String?, String?>?) {
                enableEncrypt = p?.second == "1"
                encryptLayout?.visibleOrGone(enableEncrypt)
            }
        }
        ctlRunView?.onTabSelectedChangedListener = object :
            SelectorTabView.OnTabSelectedChangedListener {

            override fun onSelectedChanged(p: Pair<String?, String?>?) {
                ctlRun = p?.second == "1"
            }
        }

        ctlViewView?.onTabSelectedChangedListener = object :
            SelectorTabView.OnTabSelectedChangedListener {

            override fun onSelectedChanged(p: Pair<String?, String?>?) {
                ctlView = p?.second == "1"
            }
        }

        ctlEditView?.onTabSelectedChangedListener = object :
            SelectorTabView.OnTabSelectedChangedListener {

            override fun onSelectedChanged(p: Pair<String?, String?>?) {
                ctlEdit = p?.second == "1"
            }
        }

        ctlShareView?.onTabSelectedChangedListener = object :
            SelectorTabView.OnTabSelectedChangedListener {

            override fun onSelectedChanged(p: Pair<String?, String?>?) {
                ctlShare = p?.second == "1"
            }
        }

        ctlCloudView?.onTabSelectedChangedListener = object :
            SelectorTabView.OnTabSelectedChangedListener {

            override fun onSelectedChanged(p: Pair<String?, String?>?) {
                ctlCloud = p?.second == "1"
            }
        }

        ctlPwdView?.onMaskClickListener = OnClickListener {
            DialogInputMessage(
                context,
                title = com.hive.i8n.R.string.sc_share_pwd_text_input_title.string(),
                hint = com.hive.i8n.R.string.sc_share_pwd_text_input_hint.string(),
                txtHold = ctlPwd ?: "",
                0,
                checkInputFun = { edit_text ->
                    checkInput(edit_text.text.toString())
                },
                { dialog, pwd ->
                    ctlPwd = pwd
                    updateCtlView()
                }).show()
        }

        ctlTimeView?.onMaskClickListener = OnClickListener {
            DialogCommonSelector(context)
                .setTitle(com.hive.i8n.R.string.sc_time_day_select_title.string())
                .setDataSet(
                    mutableListOf(
                        0 to com.hive.i8n.R.string.sc_time_day_none.string(),
//                        1 to com.hive.i8n.R.string.sc_time_hour_1.string(),
                        2 to com.hive.i8n.R.string.sc_time_hour_2.string(),
                        24 to com.hive.i8n.R.string.sc_time_day_1.string(),
                        24 * 3 to com.hive.i8n.R.string.sc_time_day_3.string(),
                        24 * 7 to com.hive.i8n.R.string.sc_time_day_7.string(),
                        24 * 30 to com.hive.i8n.R.string.sc_time_day_30.string(),
                        24 * 60 to com.hive.i8n.R.string.sc_time_day_60.string()
                    )
                ).setSelectListener(object : DialogCommonSelector.OnSelectListener {
                    override fun onSelected(
                        dialog: DialogCommonSelector, pos: Int, pair: Pair<Int, String>
                    ) {
                        ctlHour = pair.first
                        updateCtlView()
                        dialog.dismiss()
                    }

                    override fun onCancel() {
                    }
                }).show()
        }

        encryptLayout?.visibleOrGone(enableEncrypt)
        post {
            updateCtlView()
        }
    }

    private fun getControlValue(): String {
        return "${if (ctlRun) "1" else "0"}${if (ctlEdit) "1" else "0"}${if (ctlView) "1" else "0"}${if (ctlShare) "1" else "0"}${if (ctlCloud) "1" else "0"}"
    }

    private fun checkInput(ctlPwd: String?) {
        if (enableEncrypt) {
            if (TextUtils.isEmpty(ctlPwd)) {
                throw Exception(getString(com.hive.i8n.R.string.sc_check_pwd_input_check_empty))
            }

            if ((ctlPwd?.length ?: 0) < 6) {
                throw Exception(getString(com.hive.i8n.R.string.sc_check_pwd_input_check_empty_2))
            }
        }
    }

    private fun updateCtlView() {
        ctlPwdView?.setValue(if (ctlPwd.isNullOrEmpty()) com.hive.i8n.R.string.sc_share_pwd_none.string() else com.hive.i8n.R.string.sc_share_pwd_set.string())
        if (ctlHour > 0) {
            ctlTimeView?.setValue(com.hive.i8n.R.string.sc_share_time_info.string(ctlHour / 24f))
        } else {
            ctlTimeView?.setValue(com.hive.i8n.R.string.sc_time_day_none.string())
        }
        ctlRunView?.setValue(if (ctlRun) "1" else "0")
        ctlViewView?.setValue(if (ctlView) "1" else "0")
        ctlEditView?.setValue(if (ctlEdit) "1" else "0")
        ctlShareView?.setValue(if (ctlShare) "1" else "0")
        ctlCloudView?.setValue(if (ctlCloud) "1" else "0")
    }

    fun setOnShareConfirmListener(listener: OnShareConfirmListener): DialogShareConfirm {
        shareConfirmListener = listener
        return this
    }

    override fun enableFadeAnimation() = true

    override fun getWindowLayoutId() = R.layout.dialog_share_confirm

    interface OnShareConfirmListener {
        fun onShareConfirm(
            dialog: DialogShareConfirm,
            encrypt: Boolean,
            pwd: String?,
            ctrValue: String?,
            expireTime: Long = -1
        )
    }
}