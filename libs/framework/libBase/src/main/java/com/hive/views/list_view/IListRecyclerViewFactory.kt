// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.views.list_view

/**
 *
 * @author jiadou
 * @date 4/22/21
 */
interface IListRecyclerViewFactory {
    fun createItemView(viewType: Int): ListRecyclerItemView
}