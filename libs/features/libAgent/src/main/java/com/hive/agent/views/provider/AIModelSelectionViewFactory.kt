// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.agent.views.provider

import android.content.Context
import com.hive.views.list_view.ListRecyclerItemView
import com.hive.views.list_view.IListRecyclerViewFactory

/**
 * AI模型选择视图工厂
 */
class AIModelSelectionViewFactory(private val context: Context) : IListRecyclerViewFactory {

    companion object {
        const val TYPE_PROVIDER_HEADER = 0
        const val TYPE_MODEL = 1
    }

    override fun createItemView(viewType: Int): ListRecyclerItemView {
        return when (viewType) {
            TYPE_PROVIDER_HEADER -> AIProviderHeaderView(context)
            TYPE_MODEL -> AIModelSelectionItemView(context)
            else -> AIProviderHeaderView(context)
        }
    }
} 