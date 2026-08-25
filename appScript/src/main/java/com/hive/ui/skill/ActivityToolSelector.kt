// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.ui.skill

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hive.app.script.R
import com.hive.base.BaseFragmentActivity
import com.hive.plugin.ComponentManager
import com.hive.plugin.agent.AgentToolClient
import com.hive.plugin.agent.model.ToolDefinition
import com.hive.plugin.provider.IAgentProvider
import com.hive.plugin.provider.IMcpProvider
import com.hive.script.scope.ScriptScopeRepository
import java.io.File

class ActivityToolSelector : BaseFragmentActivity() {

    private val selected = linkedSetOf<String>()
    private val items = mutableListOf<ToolItem>()
    private var adapter: ToolAdapter? = null

    private var recyclerView: RecyclerView? = null
    private var tvSelectedCount: TextView? = null
    private var layoutEmpty: View? = null
    private var btnCancel: View? = null
    private var btnConfirm: View? = null

    private val mcpProvider: IMcpProvider? by lazy {
        ComponentManager.getInstance().getProvider(IMcpProvider::class.java) as? IMcpProvider
    }

    override fun doOnCreate(savedState: Bundle?) {
        recyclerView = findViewById(R.id.recycler_view)
        tvSelectedCount = findViewById(R.id.tv_selected_count)
        layoutEmpty = findViewById(R.id.layout_empty)
        btnCancel = findViewById(R.id.btn_cancel)
        btnConfirm = findViewById(R.id.btn_confirm)

        selected.clear()
        loadTools()
        val preselected = intent.getStringArrayListExtra(EXTRA_PRESELECTED_TOOLS)?.orEmpty() ?: emptyList()
        val availableNames = items.map { it.functionName }.toSet()
        selected.addAll(preselected.filter { it in availableNames })
        adapter = ToolAdapter(items)
        recyclerView?.apply {
            layoutManager = LinearLayoutManager(this@ActivityToolSelector)
            adapter = this@ActivityToolSelector.adapter
        }

        btnCancel?.setOnClickListener {
            setResult(Activity.RESULT_CANCELED)
            finish()
        }
        btnConfirm?.setOnClickListener {
            val data = Intent().apply {
                putStringArrayListExtra(EXTRA_SELECTED_TOOLS, ArrayList(selected))
            }
            setResult(Activity.RESULT_OK, data)
            finish()
        }

        updateSelectedCount()
        updateEmptyState()
    }

    override fun getLayoutId(): Int = R.layout.activity_tool_selector

    private fun loadTools() {
        val agentProvider =
            ComponentManager.getInstance().getProvider(IAgentProvider::class.java) as? IAgentProvider
        val tools: List<AgentToolClient> = agentProvider?.getRegisteredTools().orEmpty()

        val definitions: List<ToolDefinition> = tools
            .flatMap { it.toToolDefinitions() }
            .distinctBy { it.function.name }

        val filtered = definitions.filter { def ->
            val name = def.function.name
            !name.endsWith(".tools_list") && !name.endsWith(".resources_list")
        }
        val displayNameMap = linkedMapOf<String, String>().apply {
            (mcpProvider?.getRegisteredTools()?.toList() ?: emptyList()).forEach { tool ->
                val displayName = tool.extraName.takeIf { it.isNotBlank() } ?: tool.name
                if (tool.name.isNotBlank()) put(tool.name, displayName)
                if (tool.extraName.isNotBlank()) put(tool.extraName, displayName)
            }
        }

        val scopeItems = intent.getStringExtra(EXTRA_SCOPE_SCRIPT_PATH)?.let { scriptPath ->
            runCatching {
                ScriptScopeRepository.load(File(scriptPath), validate = false)
            }.getOrNull()?.tools.orEmpty().map { tool ->
                ToolItem(
                    functionName = tool.functionName,
                    displayName = tool.name.ifBlank { tool.functionName },
                    description = tool.description
                )
            }
        }.orEmpty()

        items.clear()
        items.addAll(
            (filtered.sortedBy { it.function.name }.map { def ->
                ToolItem(
                    functionName = def.function.name,
                    displayName = displayNameMap[def.function.name] ?: def.function.name,
                    description = def.function.description ?: ""
                )
            } + scopeItems).distinctBy { it.functionName }
        )
    }

    private fun updateSelectedCount() {
        tvSelectedCount?.text = getString(
            com.hive.i8n.R.string.skill_selected_tools_count,
            selected.size
        )
    }

    private fun updateEmptyState() {
        val hasData = items.isNotEmpty()
        recyclerView?.visibility = if (hasData) View.VISIBLE else View.GONE
        layoutEmpty?.visibility = if (hasData) View.GONE else View.VISIBLE
    }

    private data class ToolItem(
        val functionName: String,
        val displayName: String,
        val description: String
    )

    private inner class ToolAdapter(
        private val list: List<ToolItem>
    ) : RecyclerView.Adapter<ToolAdapter.ToolVH>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ToolVH {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_tool_selector, parent, false)
            return ToolVH(v)
        }

        override fun getItemCount(): Int = list.size

        override fun onBindViewHolder(holder: ToolVH, position: Int) {
            holder.bind(list[position])
        }

        inner class ToolVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val iconBox: View = itemView.findViewById(R.id.layout_tool_icon)
            private val tvName: TextView = itemView.findViewById(R.id.tv_tool_name)
            private val tvDesc: TextView = itemView.findViewById(R.id.tv_tool_description)
            private val ivSelected: ImageView = itemView.findViewById(R.id.iv_selected)

            fun bind(item: ToolItem) {
                tvName.text = item.displayName
                tvDesc.text = item.description
                renderSelectedState(item.functionName)

                itemView.setOnClickListener {
                    val nowChecked = !selected.contains(item.functionName)
                    if (nowChecked) {
                        selected.add(item.functionName)
                    } else {
                        selected.remove(item.functionName)
                    }
                    renderSelectedState(item.functionName)
                    updateSelectedCount()
                }
            }

            private fun renderSelectedState(toolName: String) {
                val checked = selected.contains(toolName)
                ivSelected.visibility = if (checked) View.VISIBLE else View.GONE
                iconBox.setBackgroundResource(
                    if (checked) R.drawable.design_publish_icon_bg_sky else R.drawable.design_publish_input_bg
                )
            }
        }
    }

    companion object {
        const val EXTRA_PRESELECTED_TOOLS = "extra_preselected_tools"
        const val EXTRA_SELECTED_TOOLS = "extra_selected_tools"
        const val EXTRA_SCOPE_SCRIPT_PATH = "extra_scope_script_path"
    }
}
