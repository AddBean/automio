// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.views.list_view

import android.util.Pair
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView

/**
 *
 * @author jiadou
 * @date 4/22/21
 */
class ListRecyclerAdapter(var itemTouchHelper: ItemTouchHelper) :
    RecyclerView.Adapter<ListRecyclerAdapter.DragViewHolder>() {

    private val VIEW_TYPE_HEADER = -1
    private var mEnableDrag: Boolean = false
    var dataList: List<Pair<Int, Any?>>? = null
    var dragViewFactory: IListRecyclerViewFactory? = null
    var onItemEventListener: ListRecyclerItemView.OnItemEventListener? = null
    var headerView: ListRecyclerItemView? = null
    fun submitData(dataList: List<Pair<Int, Any?>>) {
        this.dataList = dataList
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DragViewHolder {
        val itemView = if (viewType == VIEW_TYPE_HEADER && headerView != null) {
            headerView!!
        } else {
            dragViewFactory?.createItemView(viewType)!!
        }
        if (itemView.onItemEventListener == null) {
            itemView.onItemEventListener = onItemEventListener
        }
        return DragViewHolder(itemView).apply {
            if (mEnableDrag) {
                itemView.setOnLongClickListener(this)
            }
        }
    }

    private fun getItemView(viewType: Int): ListRecyclerItemView {
        val itemView = dragViewFactory?.createItemView(viewType)!!
        if (itemView.onItemEventListener == null) {
            itemView.onItemEventListener = onItemEventListener
        }
        return itemView
    }

    override fun getItemCount() = (dataList?.size ?: 0) + if (headerView != null) 1 else 0

    override fun getItemViewType(position: Int) = if (position == 0 && headerView != null) {
        VIEW_TYPE_HEADER
    } else {
        val dataPos = if (headerView != null) position - 1 else position
        dataList?.get(dataPos)?.first ?: 0
    }

    override fun onBindViewHolder(holder: DragViewHolder, position: Int) {
        if (position == 0 && headerView != null) {
            return
        }
        val dataPos = if (headerView != null) position - 1 else position
        dataList?.run {
            holder.item.bindPosition(dataPos)
            holder.item.bindItemData(this[dataPos].second)
        }
    }

    fun setEnableDrag(enableDrag: Boolean) {
        mEnableDrag = enableDrag
    }

    inner class DragViewHolder(var item: ListRecyclerItemView) : RecyclerView.ViewHolder(item),
        View.OnLongClickListener {

        override fun onLongClick(v: View?): Boolean {
            itemTouchHelper.startDrag(this)
            return true
        }
    }

}