// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.views.list

import android.content.Context
import android.util.AttributeSet
import android.widget.RelativeLayout
import androidx.annotation.CallSuper

/**
 * 列表项视图基类
 * 提供通用的数据绑定、事件处理和拖拽功能
 *
 * @author jiadou
 * @date 4/22/21
 */
abstract class ListRecyclerItemView<T> @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : RelativeLayout(context, attrs, defStyleAttr) {

    private var onItemEventListener: OnItemEventListener<T>? = null

    private var listRecyclerAdapterRef: ListRecyclerAdapter<T>? = null
    private var itemDataRef: T? = null
    private var itemPositionRef: Int = -1
    private var isSelectedRef: Boolean = false
    private var isDragEnabledRef: Boolean = true

    /**
     * 设置事件监听器
     */
    fun setOnItemEventListener(listener: OnItemEventListener<T>?) {
        onItemEventListener = listener
    }

    /**
     * 获取当前绑定的数据
     */
    val itemData: T? get() = itemDataRef

    /**
     * 获取当前项的位置
     */
    val itemPosition: Int get() = itemPositionRef

    /**
     * 是否被选中
     */
    val isItemSelected: Boolean get() = isSelectedRef

    /**
     * Adapter
     */
    val listRecyclerAdapter: ListRecyclerAdapter<T>? get() = listRecyclerAdapterRef

    /**
     * 是否启用拖拽
     */
    var isDragEnabled: Boolean
        get() = isDragEnabledRef
        set(value) {
            isDragEnabledRef = value
            onDragEnabledChanged(value)
        }

    /**
     * 绑定数据到视图
     */
    @CallSuper
    fun bindItemData(data: T?, position: Int = -1) {
        itemDataRef = data
        if (position >= 0) {
            itemPositionRef = position
        }
        bindData(data)
    }

    /**
     * 子类实现数据绑定逻辑
     */
    protected abstract fun bindData(data: T?)

    /**
     * 拖拽清除时的回调
     */
    open fun onDragClear() {
        isSelectedRef = false
        alpha = 1.0f
    }

    /**
     * 拖拽选中时的回调
     */
    open fun onDragSelected() {
        isSelectedRef = true
        alpha = 0.8f
    }

    /**
     * 拖拽启用状态改变时的回调
     */
    open fun onDragEnabledChanged(enabled: Boolean) {}

    /**
     * 发送事件
     */
    fun postEvent(eventData: Any?) {
        onItemEventListener?.onItemEvent(itemDataRef, eventData, itemPositionRef)
    }

    /**
     * 发送事件（带自定义数据）
     */
    fun postEvent(eventData: Any?, customData: T?) {
        onItemEventListener?.onItemEvent(customData, eventData, itemPositionRef)
    }

    /**
     * 绑定位置
     */
    fun bindPosition(position: Int) {
        itemPositionRef = position
    }

    /**
     * 设置引用
     */
    fun setRecyclerAdapterRef(listRecyclerAdapter: ListRecyclerAdapter<T>) {
        listRecyclerAdapterRef = listRecyclerAdapter
    }

    /**
     * 设置列表项选中状态
     */
    fun setItemSelected(selected: Boolean) {
        isSelectedRef = selected
        onSelectionChanged(selected)
    }

    /**
     * 选中状态改变时的回调
     */
    open fun onSelectionChanged(selected: Boolean) {}

    /**
     * 回收资源
     */
    open fun onRecycled() {}

    /**
     * 当 ViewHolder 附加到窗口时调用
     * 由 RecyclerView.Adapter 的 onViewAttachedToWindow 触发
     */
    open fun onItemAttachedToWindow() {}

    /**
     * 当 ViewHolder 从窗口分离时调用
     * 由 RecyclerView.Adapter 的 onViewDetachedFromWindow 触发
     */
    open fun onItemDetachedFromWindow() {}

    /**
     * 事件监听器接口
     */
    interface OnItemEventListener<T> {
        fun onItemEvent(itemData: T?, eventData: Any?, position: Int)
    }
}
