// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.agent.views.memory

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import com.hive.agent.R
import com.hive.agent.utils.AgentMemoryNoteHelper
import com.hive.base.BaseActivity
import com.hive.utils.utils.IntentUtils
import com.hive.views.list_view.ListRecyclerItemView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ActivityAgentMemoryView : BaseActivity() {

    private var listRecyclerView: com.hive.views.list_view.ListRecyclerView? = null
    private var layoutEmpty: LinearLayout? = null
    private var btnClearAll: Button? = null

    override fun doOnCreate() {
        listRecyclerView = findViewById(R.id.listRecyclerView)
        layoutEmpty = findViewById(R.id.layoutEmpty)
        btnClearAll = findViewById(R.id.btnClearAll)

        setupListView()
        loadData()
        btnClearAll?.setOnClickListener { showClearConfirmDialog() }
    }

    override fun getLayoutId(): Int = R.layout.activity_agent_memory

    override fun onResume() {
        super.onResume()
        loadData()
    }

    private fun setupListView() {
        listRecyclerView?.apply {
            layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this@ActivityAgentMemoryView)
            setItemViewFactory(MemoryListViewFactory(this@ActivityAgentMemoryView))
            setOnItemEventListener(object : ListRecyclerItemView.OnItemEventListener {
                override fun onItemEvent(itemData: Any?, eventData: Any?) {
                    if (eventData == "click" && itemData is AgentMemoryNoteHelper.MemoryItem) {
                        ActivityAgentMemoryDetail.start(this@ActivityAgentMemoryView, itemData.key, itemData.value)
                    }
                }
            })
        }
    }

    private fun loadData() {
        CoroutineScope(Dispatchers.Main).launch {
            val items = withContext(Dispatchers.IO) {
                AgentMemoryNoteHelper.getMemoryNoteContent()
            }
            val pairs = items.map {
                android.util.Pair(MemoryListViewFactory.TYPE_MEMORY_ITEM, it as Any?)
            }
            listRecyclerView?.submitDataSetsWithType(pairs)
            listRecyclerView?.notifyDataSetChanged()

            val isEmpty = items.isEmpty()
            layoutEmpty?.visibility = if (isEmpty) View.VISIBLE else View.GONE
            btnClearAll?.visibility = if (isEmpty) View.GONE else View.VISIBLE
        }
    }

    private fun showClearConfirmDialog() {
        AlertDialog.Builder(this)
            .setTitle(com.hive.i8n.R.string.agent_memory_clear_confirm_title)
            .setMessage(com.hive.i8n.R.string.agent_memory_clear_confirm_message)
            .setPositiveButton(com.hive.i8n.R.string.agent_memory_clear_all) { _, _ ->
                AgentMemoryNoteHelper.clearAllMemory()
                Toast.makeText(this, com.hive.i8n.R.string.agent_memory_cleared, Toast.LENGTH_SHORT).show()
                loadData()
            }
            .setNegativeButton(com.hive.i8n.R.string.ai_cancel, null)
            .show()
    }

    companion object {
        fun start(context: Context) {
            IntentUtils.safeStartActivity(
                context,
                Intent(context, ActivityAgentMemoryView::class.java)
            )
        }
    }
}
