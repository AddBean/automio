// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.views.list_view

/**
 *
 * @author jiadou
 * @email 172111432@qq.com
 * @date 4/22/21
 */
interface IListRecyclerViewFactory {
    fun createItemView(viewType: Int): ListRecyclerItemView
}