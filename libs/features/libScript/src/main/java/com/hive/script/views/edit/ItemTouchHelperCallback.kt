// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.edit

import android.annotation.SuppressLint
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.hive.views.list_view.ListRecyclerAdapter
import java.util.*

/**
 *
 * @author jiadou
 * @email 172111432@qq.com
 * @date 6/16/21
 */
class ItemTouchHelperCallback : ItemTouchHelper.Callback() {

    var mInnerAdapter: ScriptEditAdapter? = null

    override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
        return if (recyclerView.layoutManager is GridLayoutManager) {
            val dragFlags = ItemTouchHelper.UP or ItemTouchHelper.DOWN or ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
            val swipeFlags = 0
            makeMovementFlags(dragFlags, swipeFlags)
        } else {
            val dragFlags = ItemTouchHelper.UP or ItemTouchHelper.DOWN
            val swipeFlags = 0
            makeMovementFlags(dragFlags, swipeFlags)
        }
    }

    override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
        mInnerAdapter?.run {
            val fromPosition = viewHolder.adapterPosition
            val toPosition = target.adapterPosition
            if (fromPosition < toPosition) {
                for (i in fromPosition until toPosition) {
                    Collections.swap(mInnerAdapter?.dataList!!, i, i + 1)
                }
            } else {
                for (i in fromPosition downTo toPosition + 1) {
                    Collections.swap(mInnerAdapter?.dataList!!, i, i - 1)
                }
            }
            mInnerAdapter?.notifyItemMoved(fromPosition, toPosition)
        }
        return true
    }

    override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
        if (actionState != ItemTouchHelper.ACTION_STATE_IDLE) {
            if (viewHolder is ListRecyclerAdapter.DragViewHolder) {
                viewHolder.item.onDragSelected()
            }
        }
        super.onSelectedChanged(viewHolder, actionState)
    }

    override fun isLongPressDragEnabled() = false

    @SuppressLint("NotifyDataSetChanged")
    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        super.clearView(recyclerView, viewHolder)
        if (viewHolder is ListRecyclerAdapter.DragViewHolder) {
            viewHolder.item.onDragClear()
        }
        mInnerAdapter?.notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        mInnerAdapter?.notifyDataSetChanged()
    }

}