// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.views.list

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 列表适配器
 * 支持拖拽、多类型视图和事件处理
 *
 * @author jiadou
 * @date 4/22/21
 */
class ListRecyclerAdapter<T>(
    private var itemTouchHelper: ItemTouchHelper,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var _enableDrag: Boolean = false
    private var _dataList: MutableList<T> = CopyOnWriteArrayList()
    private var _dragViewFactory: IListRecyclerViewFactory<T>? = null
    private var _onItemEventListener: ListRecyclerItemView.OnItemEventListener<T>? = null
    private var _onDataChangedListener: OnDataChangedListener<T>? = null
    private var _loadMoreView: BaseLoadMoreFooterView? = null
    private var _showLoadMore: Boolean = false

    companion object {
        private const val VIEW_TYPE_ITEM = 0
        private const val VIEW_TYPE_LOAD_MORE = -1
        private val gson by lazy { Gson() }
    }

    val enableDrag get() = _enableDrag
    val dragViewFactory get() = _dragViewFactory
    val onItemEventListener get() = _onItemEventListener
    val onDataChangedListener get() = _onDataChangedListener

    /**
     * 设置数据
     */
    fun submitData(dataList: List<T>) {
        _dataList.clear()
        _dataList.addAll(dataList)
        notifyDataSetChanged()
        _onDataChangedListener?.onDataChanged(_dataList)
    }

    /**
     * 静默更新数据（不触发 notifyDataSetChanged）
     */
    internal fun updateDataSilently(dataList: List<T>) {
        _dataList.clear()
        _dataList.addAll(dataList)
        _onDataChangedListener?.onDataChanged(_dataList)
    }

    /**
     * 创建默认的比对函数（使用 Gson 序列化比对）
     */
    private fun createDefaultComparator(): (T, T) -> Boolean {
        return { old: T, new: T ->
            try {
                gson.toJson(old) == gson.toJson(new)
            } catch (e: Exception) {
                old == new
            }
        }
    }

    /**
     * 使用 DiffUtil 高效更新数据
     */
    fun submitDiffData(
        newData: List<T>,
        areItemsTheSame: ((old: T, new: T) -> Boolean)? = null,
        areContentsTheSame: ((old: T, new: T) -> Boolean)? = null
    ) {
        val oldData = _dataList.toList()
        
        val itemsComparator = areItemsTheSame ?: createDefaultComparator()
        val contentsComparator = areContentsTheSame ?: createDefaultComparator()
        
        val diffCallback = object : DiffUtil.Callback() {
            override fun getOldListSize(): Int = oldData.size
            override fun getNewListSize(): Int = newData.size

            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                if (oldItemPosition < 0 || oldItemPosition >= oldData.size ||
                    newItemPosition < 0 || newItemPosition >= newData.size
                ) {
                    return false
                }
                return try {
                    itemsComparator(oldData[oldItemPosition], newData[newItemPosition])
                } catch (e: Exception) {
                    false
                }
            }

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                if (oldItemPosition < 0 || oldItemPosition >= oldData.size ||
                    newItemPosition < 0 || newItemPosition >= newData.size
                ) {
                    return false
                }
                return try {
                    contentsComparator(oldData[oldItemPosition], newData[newItemPosition])
                } catch (e: Exception) {
                    false
                }
            }
        }

        val diffResult = DiffUtil.calculateDiff(diffCallback)
        updateDataSilently(newData)
        diffResult.dispatchUpdatesTo(this)
    }

    /**
     * 使用 DiffUtil 高效更新数据（异步）
     */
    suspend fun submitDiffDataAsync(
        newData: List<T>,
        areItemsTheSame: ((old: T, new: T) -> Boolean)? = null,
        areContentsTheSame: ((old: T, new: T) -> Boolean)? = null
    ) = withContext(Dispatchers.Default) {
        val oldData = _dataList.toList()
        
        val itemsComparator = areItemsTheSame ?: createDefaultComparator()
        val contentsComparator = areContentsTheSame ?: createDefaultComparator()
        
        val diffCallback = object : DiffUtil.Callback() {
            override fun getOldListSize(): Int = oldData.size
            override fun getNewListSize(): Int = newData.size

            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                if (oldItemPosition < 0 || oldItemPosition >= oldData.size ||
                    newItemPosition < 0 || newItemPosition >= newData.size
                ) {
                    return false
                }
                return try {
                    itemsComparator(oldData[oldItemPosition], newData[newItemPosition])
                } catch (e: Exception) {
                    false
                }
            }

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                if (oldItemPosition < 0 || oldItemPosition >= oldData.size ||
                    newItemPosition < 0 || newItemPosition >= newData.size
                ) {
                    return false
                }
                return try {
                    contentsComparator(oldData[oldItemPosition], newData[newItemPosition])
                } catch (e: Exception) {
                    false
                }
            }
        }

        val diffResult = DiffUtil.calculateDiff(diffCallback)
        withContext(Dispatchers.Main) {
            updateDataSilently(newData)
            diffResult.dispatchUpdatesTo(this@ListRecyclerAdapter)
        }
    }

    /**
     * 添加数据
     */
    fun addData(data: T, position: Int = -1) {
        if (position < 0 || position >= _dataList.size) {
            _dataList.add(data)
            notifyItemInserted(_dataList.size - 1)
        } else {
            _dataList.add(position, data)
            notifyItemInserted(position)
        }
        _onDataChangedListener?.onDataChanged(_dataList)
    }

    /**
     * 移除数据
     */
    fun removeData(position: Int): T? {
        if (position >= 0 && position < _dataList.size) {
            val removed = _dataList.removeAt(position)
            notifyItemRemoved(position)
            _onDataChangedListener?.onDataChanged(_dataList)
            return removed
        }
        return null
    }

    /**
     * 更新数据
     */
    fun updateData(position: Int, data: T) {
        if (position >= 0 && position < _dataList.size) {
            _dataList[position] = data
            notifyItemChanged(position)
            _onDataChangedListener?.onDataChanged(_dataList)
        }
    }

    /**
     * 移动数据
     */
    fun moveData(fromPosition: Int, toPosition: Int) {
        if (fromPosition >= 0 && fromPosition < _dataList.size &&
            toPosition >= 0 && toPosition < _dataList.size
        ) {
            val item = _dataList.removeAt(fromPosition)
            _dataList.add(toPosition, item)
            notifyItemMoved(fromPosition, toPosition)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_LOAD_MORE -> {
                val view = _loadMoreView
                    ?: throw IllegalStateException("LoadMoreView 未设置，请先调用 setLoadMoreView()")
                LoadMoreViewHolder(view)
            }
            else -> {
                val itemView = _dragViewFactory?.createItemView(viewType, parent)
                    ?: throw IllegalStateException("IListViewFactory 未设置")
                DragViewHolder(itemView).apply {
                    if (_enableDrag) {
                        itemView.setOnLongClickListener(this)
                    }
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return _dataList.size + if (_showLoadMore) 1 else 0
    }

    override fun getItemViewType(position: Int): Int {
        if (position == _dataList.size && _showLoadMore) {
            return VIEW_TYPE_LOAD_MORE
        }
        if (position < 0 || position >= _dataList.size) {
            return VIEW_TYPE_ITEM
        }
        return _dragViewFactory?.getViewType(_dataList[position], position) ?: VIEW_TYPE_ITEM
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is ListRecyclerAdapter<T>.DragViewHolder -> {
                if (position >= 0 && position < _dataList.size) {
                    val data = _dataList[position]
                    holder.bindData(data, position)
                }
            }
            is ListRecyclerAdapter<T>.LoadMoreViewHolder -> {
                // LoadMoreView 的状态由 ListRecyclerView 管理
                _loadMoreView?.let { view ->
                    _loadMoreStateUpdater?.invoke(view)
                }
            }
        }
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        super.onViewRecycled(holder)
        if (holder is ListRecyclerAdapter<T>.DragViewHolder) {
            _dragViewFactory?.recycleItemView(holder.item)
        }
    }

    override fun onViewAttachedToWindow(holder: RecyclerView.ViewHolder) {
        super.onViewAttachedToWindow(holder)
        if (holder is ListRecyclerAdapter<T>.DragViewHolder) {
            holder.item.onItemAttachedToWindow()
        }
    }

    override fun onViewDetachedFromWindow(holder: RecyclerView.ViewHolder) {
        super.onViewDetachedFromWindow(holder)
        if (holder is ListRecyclerAdapter<T>.DragViewHolder) {
            holder.item.onItemDetachedFromWindow()
        }
    }

    /**
     * 设置拖拽启用状态
     */
    fun setEnableDrag(enableDrag: Boolean) {
        _enableDrag = enableDrag
    }

    /**
     * 设置视图工厂
     */
    fun setDragViewFactory(factory: IListRecyclerViewFactory<T>) {
        _dragViewFactory = factory
    }

    /**
     * 设置事件监听器
     */
    fun setOnItemEventListener(listener: ListRecyclerItemView.OnItemEventListener<T>) {
        _onItemEventListener = listener
    }

    /**
     * 设置数据变化监听器
     */
    fun setOnDataChangedListener(listener: OnDataChangedListener<T>) {
        _onDataChangedListener = listener
    }

    /**
     * 获取数据列表
     */
    fun getDataList(): List<T> = _dataList.toList()

    /**
     * 获取指定位置的数据
     */
    fun getData(position: Int): T? {
        return if (position >= 0 && position < _dataList.size) _dataList[position] else null
    }

    fun onDataChanged() {
        notifyDataSetChanged()
        _onDataChangedListener?.onDataChanged(_dataList.toMutableList())
    }

    /**
     * 清空数据
     */
    fun clearData() {
        val size = _dataList.size
        if (size > 0) {
            _dataList.clear()
            notifyItemRangeRemoved(0, size)
            _onDataChangedListener?.onDataChanged(_dataList)
        }
    }

    /**
     * 设置加载更多尾部视图（必须是 ILoadMoreView 类型）
     */
    fun setLoadMoreView(view: BaseLoadMoreFooterView?) {
        val wasShowing = _showLoadMore
        _loadMoreView = view
        
        if (wasShowing && view == null) {
            _showLoadMore = false
            notifyItemRemoved(_dataList.size)
        }
    }

    /**
     * 设置状态更新回调（由 ListRecyclerView 调用）
     */
    private var _loadMoreStateUpdater: ((BaseLoadMoreFooterView) -> Unit)? = null

    fun setLoadMoreStateUpdater(updater: ((BaseLoadMoreFooterView) -> Unit)?) {
        _loadMoreStateUpdater = updater
    }

    /**
     * 设置是否显示加载更多尾部视图
     */
    fun setShowLoadMore(show: Boolean) {
        val shouldShow = show && _loadMoreView != null
        if (_showLoadMore == shouldShow) return
        
        val oldCount = itemCount
        _showLoadMore = shouldShow
        val newCount = itemCount
        
        when {
            newCount > oldCount -> {
                notifyItemInserted(_dataList.size)
            }
            newCount < oldCount -> {
                notifyItemRemoved(_dataList.size)
            }
        }
    }

    /**
     * 获取 LoadMoreView
     */
    fun getLoadMoreView(): BaseLoadMoreFooterView? = _loadMoreView

    /**
     * 拖拽ViewHolder
     */
    inner class DragViewHolder(val item: ListRecyclerItemView<T>) :
        RecyclerView.ViewHolder(item), View.OnLongClickListener {

        fun bindData(data: T, position: Int) {
            item.setRecyclerAdapterRef(this@ListRecyclerAdapter)
            item.bindItemData(data, position)
            item.setOnItemEventListener(_onItemEventListener)
            item.isDragEnabled = _dragViewFactory?.canDrag(data, position) ?: true
        }

        override fun onLongClick(v: View?): Boolean {
            val data = getData(adapterPosition)
            val canDrag = _dragViewFactory?.canDrag(data, adapterPosition) ?: true
            val canLongClick = _dragViewFactory?.canLongClick(data, adapterPosition) ?: true

            if (canDrag && canLongClick && _enableDrag) {
                itemTouchHelper.startDrag(this)
                return true
            }
            return false
        }
    }

    /**
     * 加载更多尾部ViewHolder
     */
    inner class LoadMoreViewHolder(view: View) : RecyclerView.ViewHolder(view)

    /**
     * 数据变化监听器
     */
    interface OnDataChangedListener<T> {
        fun onDataChanged(dataList: List<T>)
    }
}
