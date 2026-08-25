// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.dialog

import android.content.Context
import android.view.View
import android.widget.TextView
import com.hive.script.R
import com.hive.script.utils.ScriptHelper
import com.hive.script.views.beans.ScriptInfoModel
import com.hive.script.views.cards.ScriptItemSelectorView
import com.hive.script.views.manager.ScriptRecordManager
import com.hive.script.views.widgets.BaseScriptDialog
import com.hive.utils.utils.DeviceCompatHelper
import com.hive.views.StatefulLayout
import com.hive.views.list_view.ListRecyclerItemView
import com.hive.views.list_view.IListRecyclerViewFactory
import com.hive.views.list_view.ListRecyclerView

/**
 *
 * @author jiadou
 * @email 172111432@qq.com
 * @date 6/11/21
 */
class DialogScriptListSelector(context: Context, private var filterEncrypt: Boolean) :
    BaseScriptDialog(context),
    ListRecyclerItemView.OnItemEventListener {

    private var cancelBySubmit = false

    private var title: String? = null

    private var onScriptSelectListener: OnScriptSelectListener? = null

    private var iv_close: View? = null
    private var layout_state: StatefulLayout? = null
    private var recycler_view: ListRecyclerView? = null
    private var tvTitle: TextView? = null

    override fun initWindow() {
        ScriptRecordManager.hiddenRecordView()
        iv_close = findViewById(R.id.iv_close)
        layout_state = findViewById(R.id.layout_state)
        recycler_view = findViewById(R.id.recycler_view)
        tvTitle = findViewById(R.id.tvTitle)
        recycler_view?.setItemViewFactory(object : IListRecyclerViewFactory {
            override fun createItemView(viewType: Int) = ScriptItemSelectorView(context!!).apply {
                onItemEventListener = this@DialogScriptListSelector
            }
        })

        iv_close?.setOnClickListener {
            dismiss()
        }
        layout_state?.setOnClickListener {
            dismiss()
        }
        title?.run {
            tvTitle?.text = this
        }
        post {
            updateScriptList()
        }
    }

    fun setTitle(title: String): DialogScriptListSelector {
        this.title = title
        tvTitle?.text = title
        return this
    }

    private fun updateScriptList() {
        ScriptHelper.listAllScripts(filterEncrypt)?.run {
            recycler_view?.submitDataSets(this.toMutableList())
            if (this.isEmpty()) {
                layout_state?.showEmpty()
            } else {
                layout_state?.showContent()
            }
        } ?: run {
            layout_state?.showEmpty()
        }

    }

    override fun onItemEvent(itemData: Any?, eventData: Any?) {
        eventData?.run {
            cancelBySubmit = true
            onScriptSelectListener?.onSelected(
                this@DialogScriptListSelector,
                eventData as ScriptInfoModel
            )
        }
    }

    override fun getWindowLayoutId() = R.layout.view_script_list_selector

    override fun getMarginParams() =
        arrayOf(0, if (DeviceCompatHelper.isLandscape()) 0 else 160 * DP, 0, 0)

    fun setOnScriptSelectListener(listener: OnScriptSelectListener): DialogScriptListSelector {
        onScriptSelectListener = listener
        return this
    }

    override fun onDismiss() {
        super.onDismiss()
        if (!cancelBySubmit)
            onScriptSelectListener?.onDismissed()
        cancelBySubmit = false
    }

    interface OnScriptSelectListener {
        fun onSelected(dialog: DialogScriptListSelector, model: ScriptInfoModel)

        fun onDismissed()
    }
}