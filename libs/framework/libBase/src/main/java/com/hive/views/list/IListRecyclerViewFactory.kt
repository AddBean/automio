// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.views.list

import android.view.ViewGroup

/**
 * 列表项视图工厂接口
 * 负责创建和管理不同类型的列表项视图
 *
 * @author jiadou
 * @date 4/22/21
 */
interface IListRecyclerViewFactory<T> {

    /**
     * 创建指定类型的列表项视图
     * @param viewType 视图类型
     * @param parent 父容器
     * @return 创建的列表项视图
     */
    fun createItemView(viewType: Int, parent: ViewGroup): ListRecyclerItemView<T>

    /**
     * 获取视图类型
     * @param data 数据对象
     * @param position 位置
     * @return 视图类型
     */
    fun getViewType(data: T?, position: Int): Int = 0

    /**
     * 是否可以拖拽
     * @param data 数据对象
     * @param position 位置
     * @return 是否可以拖拽
     */
    fun canDrag(data: T?, position: Int): Boolean = false

    /**
     * 是否可以长按
     * @param data 数据对象
     * @param position 位置
     * @return 是否可以长按
     */
    fun canLongClick(data: T?, position: Int): Boolean = true

    /**
     * 回收视图
     * @param itemView 要回收的视图
     */
    fun recycleItemView(itemView: ListRecyclerItemView<T>) {
        itemView.onRecycled()
    }
}
