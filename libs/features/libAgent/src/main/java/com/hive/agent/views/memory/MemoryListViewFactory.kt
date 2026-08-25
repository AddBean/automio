// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.agent.views.memory

import android.content.Context
import com.hive.views.list_view.ListRecyclerItemView
import com.hive.views.list_view.IListRecyclerViewFactory

class MemoryListViewFactory(private val context: Context) : IListRecyclerViewFactory {

    companion object {
        const val TYPE_MEMORY_ITEM = 0
    }

    override fun createItemView(viewType: Int): ListRecyclerItemView {
        return MemoryListItemView(context)
    }
}
