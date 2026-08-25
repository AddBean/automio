// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.edit

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.hive.script.R
import com.hive.script.base.ScriptCommand
import com.hive.script.extensions.getType
/**
 *
 * @author jiadou
 * @email 172111432@qq.com
 * @date 4/22/21
 */
class ScriptEditAdapter(var itemTouchHelper: ItemTouchHelper) : RecyclerView.Adapter<ScriptEditAdapter.DragViewHolder>() {

    private var mEnableDrag: Boolean = false
    var dataList: MutableList<ScriptCommand>? = null
    var dragViewFactory: IListViewFactory? = null

    @SuppressLint("NotifyDataSetChanged")
    fun submitData(dataList: MutableList<ScriptCommand>) {
        this.dataList = dataList
        notifyDataSetChanged()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DragViewHolder = DragViewHolder(getItemView(viewType)).apply {
        if (mEnableDrag) {
            itemView.setOnLongClickListener(this)
            item.findViewById<View>(R.id.drag_view)?.setOnTouchListener { v, event ->
                if (event.action == MotionEvent.ACTION_DOWN) {
                    itemTouchHelper.startDrag(this)
                }
                false
            }
        }
    }

    private fun getItemView(viewType: Int): AbsListItemView = dragViewFactory?.createItemView(viewType)!!

    override fun getItemCount() = dataList?.size ?: 0

    override fun getItemViewType(position: Int) = dataList?.get(position)?.getType() ?: -1

    override fun onBindViewHolder(holder: DragViewHolder, position: Int) {
        dataList?.run {
            holder.item.pos = position
            holder.item.size = itemCount
            holder.item.bindItemData(this[position])
        }
    }

    fun setEnableDrag(enableDrag: Boolean) {
        mEnableDrag = enableDrag
    }

    inner class DragViewHolder(var item: AbsListItemView) : RecyclerView.ViewHolder(item) , View.OnLongClickListener {

        override fun onLongClick(v: View?): Boolean {
            itemTouchHelper.startDrag(this)
            return true
        }
    }

}