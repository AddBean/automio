// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.agent.views.memory

import android.content.Context
import android.widget.TextView
import com.hive.agent.R
import com.hive.agent.utils.AgentMemoryNoteHelper
import com.hive.views.list_view.ListRecyclerItemView

class MemoryListItemView(context: Context) : ListRecyclerItemView(context) {

    private lateinit var itemRoot: android.view.View
    private lateinit var tvKey: TextView
    private lateinit var tvPreview: TextView
    private lateinit var tvCharCount: TextView

    init {
        inflate(context, R.layout.item_agent_memory, this)
        itemRoot = findViewById(R.id.itemRoot)
        tvKey = findViewById(R.id.tvKey)
        tvPreview = findViewById(R.id.tvPreview)
        tvCharCount = findViewById(R.id.tvCharCount)

        itemRoot.setOnClickListener { postEvent("click") }
    }

    override fun bindData(data: Any?) {
        if (data !is AgentMemoryNoteHelper.MemoryItem) return
        tvKey.text = data.key
        val preview = if (data.value.length > 50) {
            data.value.take(50) + "..."
        } else {
            data.value
        }
        tvPreview.text = preview
        tvCharCount.text = context.getString(com.hive.i8n.R.string.agent_memory_char_count, data.value.length)
    }
}
