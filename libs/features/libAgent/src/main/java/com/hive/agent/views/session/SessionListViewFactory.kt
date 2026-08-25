// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.agent.views.session

import android.content.Context
import com.hive.views.list_view.ListRecyclerItemView
import com.hive.views.list_view.IListRecyclerViewFactory

class SessionListViewFactory(
    private val context: Context,
    private val currentSessionKey: String?,
    private val isAgentRunning: Boolean
) : IListRecyclerViewFactory {
    override fun createItemView(viewType: Int): ListRecyclerItemView {
        return SessionListItemView(
            context = context,
            currentSessionKey = currentSessionKey,
            isAgentRunning = isAgentRunning
        )
    }
}
