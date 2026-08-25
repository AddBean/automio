// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.TextView
import com.hive.script.R
import com.hive.script.views.cards.ScriptItemView
import com.hive.script.views.cards.ScriptItemViewForMenu
import com.hive.utils.extends.findParentViewByClass
import com.hive.views.StatefulLayout
import com.hive.views.list_view.ListRecyclerView
import com.hive.views.widgets.AbsWindowFloatView

class ScriptManagerLayoutForMenu(context: Context, attributes: AttributeSet?) :
    ScriptManagerLayout(context, attributes) {

    constructor(context: Context) : this(context, null)

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
        return ScriptItemViewForMenu(context)
    }

    override fun handleItemMenuEvent(
        itemData: ScriptItemView.ItemData,
        eventData: ScriptItemView.Event
    ) {
        // 悬浮窗中点击 script 直接进入编辑页，不跳转详情页
        if (eventData == ScriptItemView.Event.MENU_DETAIL) {
            super.handleItemMenuEvent(itemData, ScriptItemView.Event.MENU_EDIT)
        } else {
            super.handleItemMenuEvent(itemData, eventData)
        }
    }

    override fun getFilterButtonView(): TextView? {
        return (parent?.parent as View?)?.findViewById(R.id.tv_filter_title)
    }

    override fun switchEditMode() {

    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        super.onTouchEvent(event)
        return true
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        requestNoInterceptTouchEvent()
        super.dispatchTouchEvent(ev)
        return true
    }

    private fun requestNoInterceptTouchEvent() {
        this.findParentViewByClass<AbsWindowFloatView>()?.requestNoInterceptTouchEvent()
    }


    override fun getManagerLayout() = R.layout.script_manager_list_for_menu
}