// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.dialog

import android.content.Context
import android.view.View
import android.widget.TextView
import com.hive.script.R
import com.hive.script.views.cards.ToolItemSelectorView
import com.hive.script.views.widgets.BaseScriptDialog
import com.hive.utils.utils.DeviceCompatHelper
import com.hive.views.StatefulLayout
import com.hive.views.list_view.ListRecyclerItemView
import com.hive.views.list_view.IListRecyclerViewFactory
import com.hive.views.list_view.ListRecyclerView
import java.io.File

/**
 * 工具选择弹框
 */
class DialogToolSelector(context: Context) : BaseScriptDialog(context),
    ListRecyclerItemView.OnItemEventListener {

    private var cancelBySubmit = false
    private var title: String? = null
    private var onToolSelectListener: OnToolSelectListener? = null
    private var toolItems: List<ToolItemSelectorView.ToolItem> = emptyList()

    private var ivClose: View? = null
    private var layoutState: StatefulLayout? = null
    private var recyclerView: ListRecyclerView? = null
    private var tvTitle: TextView? = null

    override fun initWindow() {
        ivClose = findViewById(R.id.iv_close)
        layoutState = findViewById(R.id.layout_state)
        recyclerView = findViewById(R.id.recycler_view)
        tvTitle = findViewById(R.id.tvTitle)
        recyclerView?.setItemViewFactory(object : IListRecyclerViewFactory {
            override fun createItemView(viewType: Int): ListRecyclerItemView =
                ToolItemSelectorView(context!!).apply {
                    onItemEventListener = this@DialogToolSelector
                }
        })

        ivClose?.setOnClickListener { dismiss() }
        layoutState?.setOnClickListener { dismiss() }
        title?.let { tvTitle?.text = it }
        post { updateToolList() }
    }

    fun setTitle(title: String): DialogToolSelector {
        this.title = title
        tvTitle?.text = title
        return this
    }

    fun setToolItems(items: List<ToolItemSelectorView.ToolItem>): DialogToolSelector {
        this.toolItems = items
        return this
    }

    private fun updateToolList() {
        recyclerView?.submitDataSets(toolItems.toMutableList())
        if (toolItems.isEmpty()) {
            layoutState?.showEmpty()
        } else {
            layoutState?.showContent()
        }
    }

    override fun onItemEvent(itemData: Any?, eventData: Any?) {
        (eventData as? ToolItemSelectorView.ToolItem)?.let { item ->
            cancelBySubmit = true
            onToolSelectListener?.onSelected(this@DialogToolSelector, item.dir, item.name)
        }
    }

    override fun getWindowLayoutId(): Int = R.layout.view_tool_list_selector

    override fun getMarginParams(): Array<Int> =
        arrayOf(0, if (DeviceCompatHelper.isLandscape()) 0 else 160 * DP, 0, 0)

    fun setOnToolSelectListener(listener: OnToolSelectListener): DialogToolSelector {
        onToolSelectListener = listener
        return this
    }

    override fun onDismiss() {
        super.onDismiss()
        if (!cancelBySubmit) {
            onToolSelectListener?.onDismissed()
        }
        cancelBySubmit = false
    }

    interface OnToolSelectListener {
        fun onSelected(dialog: DialogToolSelector, toolDir: File, toolName: String)
        fun onDismissed()
    }
}