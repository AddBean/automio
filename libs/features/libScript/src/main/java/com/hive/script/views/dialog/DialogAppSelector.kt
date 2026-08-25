// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.dialog

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.ImageView
import androidx.recyclerview.widget.GridLayoutManager
import com.hive.files.utils.XAppInfoParser
import com.hive.script.R
import com.hive.script.ScriptProvider
import com.hive.script.base.ScriptConst
import com.hive.script.views.cards.ScriptAppFileCard
import com.hive.script.views.widgets.BaseScriptDialog
import com.hive.utils.global.SPTools
import com.hive.utils.utils.DeviceCompatHelper
import com.hive.views.StatefulLayout
import com.hive.views.list_view.IListRecyclerViewFactory
import com.hive.views.list_view.ListRecyclerView

/**
 *
 * @author jiadou
 * @date 6/12/21
 */
class DialogAppSelector(context: Context?) : BaseScriptDialog(context) {

    private var onAppSelectedListener: OnAppSelectedListener? = null

    private var iv_close: ImageView? = null
    private var layout_state: StatefulLayout? = null
    private var recycler_view: ListRecyclerView? = null
    private var search_edit: EditText? = null

    override fun initWindow() {
        iv_close = findViewById(R.id.iv_close)
        layout_state = findViewById(R.id.layout_state)
        recycler_view = findViewById(R.id.recycler_view)
        search_edit = findViewById(R.id.search_edit)

        recycler_view?.layoutManager = GridLayoutManager(context, 4)
        recycler_view?.setItemViewFactory(object : IListRecyclerViewFactory {
            override fun createItemView(viewType: Int) = ScriptAppFileCard(context!!).apply {
                setOnClickListener {
                    onAppSelectedListener?.onSelected(this@DialogAppSelector, mAppInfo)
                }
            }
        })

        iv_close?.setOnClickListener {
            dismiss()
        }
        search_edit?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updateAppList(s?.toString())
            }

            override fun afterTextChanged(s: Editable?) {

            }
        })
    }

    override fun onShow() {
        super.onShow()
        post {
            if (ScriptProvider.sAppList == null) {
                val grant = SPTools.getInstance()
                    .getBoolean(ScriptConst.SCRIPT_SP_APP_PERMISSION_GRANT, false)
                if (grant) {
                    layout_state?.showProgress()
                    ScriptProvider.updateApp {
                        updateAppList()
                        layout_state?.showContent()
                    }
                } else {
                    DialogScriptAlert(context)
                        .setTitle(com.hive.i8n.R.string.sc_app_alert_title)
                        .setContent(com.hive.i8n.R.string.sc_app_alert_content)
                        .setConfirmText(com.hive.i8n.R.string.sc_app_alert_confirm)
                        .setOnDialogEventListener(object : DialogScriptAlert.OnDialogEventListener {
                            override fun onClickEvent(
                                dialog: DialogScriptAlert,
                                isCancel: Boolean
                            ) {
                                dialog.dismiss()
                                if (!isCancel) {
                                    SPTools
                                        .getInstance()
                                        .putBoolean(
                                            ScriptConst.SCRIPT_SP_APP_PERMISSION_GRANT,
                                            true
                                        )
                                    layout_state?.showProgress()
                                    ScriptProvider.updateApp {
                                        updateAppList()
                                        layout_state?.showContent()
                                    }
                                } else {
                                    this@DialogAppSelector.dismiss()
                                }
                            }
                        }).show()
                }

            } else {
                updateAppList()
            }

        }
    }

    private fun updateAppList(search: String? = null) {
        ScriptProvider.sAppList?.filter {
            val app = it.cardData as XAppInfoParser.AppInfo
            if (search == null) {
                true
            } else {
                app.appName?.contains(search, true) == true
            }
        }?.run {
            recycler_view?.submitDataSets(this.toMutableList())
        }
    }

    fun setOnAppSelectedListener(listener: OnAppSelectedListener?): DialogAppSelector {
        onAppSelectedListener = listener
        return this
    }

    override fun getMarginParams() =
        arrayOf(0, if (DeviceCompatHelper.isLandscape()) 0 else 160 * DP, 0, 0)


    override fun getWindowLayoutId() = R.layout.dialog_app_selector

    interface OnAppSelectedListener {
        fun onSelected(dialog: DialogAppSelector, appInfo: XAppInfoParser.AppInfo?)
    }

}