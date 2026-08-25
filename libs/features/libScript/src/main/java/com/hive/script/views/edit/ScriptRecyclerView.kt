// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.edit

import android.content.Context
import android.util.AttributeSet
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hive.script.base.ScriptCommand


/**
 *
 * @author jiadou
 * @date 4/22/21
 */
class ScriptRecyclerView(context: Context, attrs: AttributeSet) : RecyclerView(context, attrs) {
    private var mDragEnable: Boolean = true
    private var mItemTouchCallback = ItemTouchHelperCallback()
    private var itemTouchHelper: ItemTouchHelper = ItemTouchHelper(mItemTouchCallback)
    var mInnerAdapter = ScriptEditAdapter(itemTouchHelper).apply {
        mItemTouchCallback.mInnerAdapter = this
    }

    init {
        adapter = mInnerAdapter
        layoutManager = LinearLayoutManager(context, VERTICAL, false)
    }

    fun setItemViewFactory(factory: IListViewFactory) {
        mInnerAdapter.dragViewFactory = factory
    }

    fun setEnableDrag(enableDrag: Boolean) {
        mInnerAdapter.setEnableDrag(enableDrag)
        if (enableDrag) {
            itemTouchHelper.attachToRecyclerView(this)
        }
    }

    fun submitData(dataSets: MutableList<ScriptCommand>) {
        mInnerAdapter.submitData(dataSets)
    }

    fun notifyDataSetChanged() {
        mInnerAdapter.notifyDataSetChanged()
    }

}