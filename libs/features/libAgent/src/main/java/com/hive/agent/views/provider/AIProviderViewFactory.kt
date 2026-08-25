// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.agent.views.provider

import android.content.Context
import com.hive.views.list_view.ListRecyclerItemView
import com.hive.views.list_view.IListRecyclerViewFactory

/**
 * AI Provider视图工厂
 */
class AIProviderViewFactory(private val context: Context) : IListRecyclerViewFactory {

    companion object {
        const val TYPE_PROVIDER = 0
    }

    override fun createItemView(viewType: Int): ListRecyclerItemView {
        return when (viewType) {
            TYPE_PROVIDER -> AIProviderItemView(context)
            else -> AIProviderItemView(context)
        }
    }
} 