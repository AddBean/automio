// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.views.list_view

import android.content.Context
import android.util.AttributeSet
import android.util.Pair
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hive.utils.debug.DLog
import com.hive.views.list_view.ListRecyclerAdapter.DragViewHolder
import java.util.Collections


/**
 *
 * @author jiadou
 * @date 4/22/21
 */
open class ListRecyclerView(context: Context, attrs: AttributeSet?) : RecyclerView(context, attrs) {
    private var mDragEnable: Boolean = true
    private var itemTouchHelper: ItemTouchHelper = ItemTouchHelper(ItemTouchHelperCallback())
    private var mInnerAdapter = ListRecyclerAdapter(itemTouchHelper)
    private var listLayoutManager: LayoutManager? = LinearLayoutManager(context, VERTICAL, false)
    private var fixedPositions= mutableListOf<Int>()

    constructor(context: Context, listLayoutManager: LayoutManager) : this(context, null) {
        this.listLayoutManager = listLayoutManager
    }

    init {
        adapter = mInnerAdapter
    }

    fun setFixedPositions(fixedPositions: MutableList<Int>) {
        this.fixedPositions = fixedPositions
    }

    fun setItemViewFactory(factory: IListRecyclerViewFactory) {
        if (layoutManager == null && listLayoutManager?.isAttachedToWindow == false) {
            layoutManager = listLayoutManager
        }
        mInnerAdapter.dragViewFactory = factory
        addOnScrollListener(mOnScrollListener)
    }

    fun submitDataSets(dataList: List<Any?>) {
        if (mInnerAdapter.dragViewFactory == null) {
            throw RuntimeException(com.hive.utils.GlobalApp.getString(com.hive.i8n.R.string.base_set_item_view_factory_required))
        }
        mInnerAdapter.submitData(dataList.map { Pair(0, it) })
        mInnerAdapter.notifyDataSetChanged()
    }

    fun submitDataSetsWithType(dataList: List<Pair<Int, Any?>>) {
        if (mInnerAdapter.dragViewFactory == null) {
            throw RuntimeException(com.hive.utils.GlobalApp.getString(com.hive.i8n.R.string.base_set_item_view_factory_required))
        }
        mInnerAdapter.submitData(dataList)
        mInnerAdapter.notifyDataSetChanged()
    }

    fun setEnableDrag(enableDrag: Boolean) {
        mInnerAdapter.setEnableDrag(enableDrag)
        if (enableDrag) {
            itemTouchHelper.attachToRecyclerView(this)
        }
    }

    fun getDataSets(): List<Pair<Int, Any?>>? {
        return mInnerAdapter.dataList
    }

    fun notifyDataSetChanged() {
        mInnerAdapter.notifyDataSetChanged()
    }

    fun notifyItemInserted(pos: Int) {
        mInnerAdapter.notifyItemInserted(pos)
    }

    fun notifyItemChanged(pos: Int) {
        mInnerAdapter.notifyItemChanged(pos)
    }

    fun notifyItemRemoved(pos: Int) {
        mInnerAdapter.notifyItemRemoved(pos)
    }

    fun setOnItemEventListener(onItemEventListener: ListRecyclerItemView.OnItemEventListener) {
        mInnerAdapter.onItemEventListener = onItemEventListener
    }

    fun setHeaderView(headerView: ListRecyclerItemView?) {
        mInnerAdapter.headerView = headerView
    }

    private val mOnScrollListener: OnScrollListener = object : OnScrollListener() {
        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            super.onScrollStateChanged(recyclerView, newState)
            if (getLoadMoreLastCount() == -1) return
            val layoutManager = recyclerView.layoutManager
            if (layoutManager is LinearLayoutManager) {
                //获取最后一个可见view的位置
                val lastItemPosition = layoutManager.findLastVisibleItemPosition()
                //倒数第mLoadConditionPaddingSize个开始加载；
                if (lastItemPosition == recyclerView.adapter!!.itemCount - getLoadMoreLastCount()) {
                    DLog.e("onScrollStateChanged", "满足加载条件$lastItemPosition")
                    onLoadMore()
                }
            }
        }
    }

    open fun onLoadMore() {

    }

    //倒数第几个开始加载；
    open fun getLoadMoreLastCount(): Int = 0


    inner class ItemTouchHelperCallback : ItemTouchHelper.Callback() {
        override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: ViewHolder): Int {
            return if (recyclerView.layoutManager is GridLayoutManager) {
                val dragFlags =
                    ItemTouchHelper.UP or ItemTouchHelper.DOWN or ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
                val swipeFlags = 0
                makeMovementFlags(dragFlags, swipeFlags)
            } else {
                val dragFlags = ItemTouchHelper.UP or ItemTouchHelper.DOWN
                val swipeFlags = 0
                makeMovementFlags(dragFlags, swipeFlags)
            }
        }

        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: ViewHolder,
            target: ViewHolder
        ): Boolean {

            val fromPosition = viewHolder.adapterPosition
            val toPosition = target.adapterPosition
            if (fixedPositions.contains(fromPosition) || fixedPositions.contains(toPosition)) {
                return false
            }

            if (fromPosition < toPosition) {
                for (i in fromPosition until toPosition) {
                    Collections.swap(mInnerAdapter.dataList, i, i + 1)
                }
            } else {
                for (i in fromPosition downTo toPosition + 1) {
                    Collections.swap(mInnerAdapter.dataList, i, i - 1)
                }
            }
            mInnerAdapter.notifyItemMoved(fromPosition, toPosition)
            return true
        }

        override fun onSelectedChanged(viewHolder: ViewHolder?, actionState: Int) {
            if (actionState != ItemTouchHelper.ACTION_STATE_IDLE) {
                if (viewHolder is DragViewHolder) {
                    viewHolder.item.onDragSelected()
                }
            }
            super.onSelectedChanged(viewHolder, actionState)
        }


        override fun clearView(recyclerView: RecyclerView, viewHolder: ViewHolder) {
            super.clearView(recyclerView!!, viewHolder)
            if (viewHolder is DragViewHolder) {
                viewHolder.item.onDragClear()
            }
        }

        override fun onSwiped(viewHolder: ViewHolder, direction: Int) {

        }

    }

}