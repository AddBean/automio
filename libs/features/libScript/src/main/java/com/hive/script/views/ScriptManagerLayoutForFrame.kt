// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.TextView
import com.hive.script.R
import com.hive.script.views.cards.ScriptItemView
import com.hive.views.StatefulLayout
import com.hive.views.list_view.ListRecyclerView

class ScriptManagerLayoutForFrame(context: Context, attributes: AttributeSet?) :
    ScriptManagerLayout(context, attributes) {

    constructor(context: Context) : this(context, null)

    override fun getFilterButtonView(): TextView? {
        return findViewById(R.id.tv_filter_title)
    }

    override fun getCancelButtonView(): View? {
        return findViewById(R.id.tv_btn_cancel)
    }

    override fun getDeleteButtonView(): View? {
        return findViewById(R.id.tv_btn_delete)
    }

    override fun getSelectButtonView(): TextView? {
        return findViewById(R.id.tv_btn_selected)
    }

    override fun getEditButtonView(): View? {
        return findViewById(R.id.btn_edit)
    }

    override fun getImportButtonView(): View? {
        return findViewById(R.id.btn_import)
    }

    override fun getCloseButtonView(): View? {
        return findViewById(R.id.iv_close)
    }

    override fun getLayoutStateView(): StatefulLayout? {
        return findViewById(R.id.layout_state)
    }

    override fun getLayoutTaskAddView(): View? {
        return findViewById(R.id.layoutTaskAdd)
    }


    override fun getLayoutTitleView(): View? {
        return findViewById(R.id.layout_title)
    }

    override fun getLayoutSelectView(): View? {
        return findViewById(R.id.layout_edit_opt)
    }

    override fun getListRecyclerView(): ListRecyclerView {
        return findViewById(R.id.recycler_view)
    }

    override fun getListItemView(): ScriptItemView {
        return ScriptItemView(context)
    }

    override fun getManagerLayout() = R.layout.script_manager_list_frame
}
