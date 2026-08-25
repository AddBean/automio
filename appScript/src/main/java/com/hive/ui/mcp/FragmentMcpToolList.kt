// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.ui.mcp

import android.content.Context
import android.util.Pair
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageView
import com.hive.ui.widgets.ResourceListItemView
import android.widget.TextView
import com.hive.app.script.R
import com.hive.i8n.R as i8nR
import com.hive.plugin.mcp.McpConst
import com.hive.plugin.ComponentManager
import com.hive.plugin.mcp.model.McpTool
import com.hive.plugin.provider.IMcpProvider
import com.hive.script.mcp.ScriptMcpRegister
import com.hive.ui.creation.ActivityCreationCenter
import com.hive.ui.common.ResourceQuickRunController
import com.hive.ui.common.ResourceRunStateStore
import com.hive.utils.debug.DLog
import com.hive.views.fragment.PagerFragment
import com.hive.views.list_view.ListRecyclerItemView
import com.hive.views.list_view.IListRecyclerViewFactory
import com.hive.views.list_view.ListRecyclerView
import com.hive.views.popmenu.PopMenuManager

/**
 * MCP 工具列表页面
 *
 * @author jiadou
 * @date 2024/12/19
 */
class FragmentMcpToolList : PagerFragment() {

    private enum class ToolFilter {
        CUSTOM,
        BUILTIN,
        ALL
    }

    var mcpProvider: IMcpProvider? = null

    private val allTools = mutableListOf<McpTool>()
    private val toolList = mutableListOf<McpTool>()
    private var recycler_view: ListRecyclerView? = null
    private var tv_empty_message: TextView? = null
    private var tvEmptyTitle: TextView? = null
    private var layout_empty: View? = null
    private var btnEmptyAction: Button? = null
    private var currentFilter = ToolFilter.CUSTOM
    private var headerView: ToolHeaderItemView? = null
    private val runStateListener: () -> Unit = {
        recycler_view?.notifyDataSetChanged()
    }
    override fun getLayoutId(): Int = R.layout.fragment_mcp_list

    override fun initView() {
        super.initView()
        mcpProvider = ComponentManager.getInstance()
            .getProvider(IMcpProvider::class.java) as IMcpProvider?

        initViews()
        loadTools()
        // mView?.setPadding(0, 0, 0, TabHelper.tabHeight )
    }

    private fun initViews() {
        recycler_view = view?.findViewById(R.id.recycler_view)
        tv_empty_message = view?.findViewById(R.id.tv_empty_message)
        tvEmptyTitle = view?.findViewById(R.id.tv_empty_title)
        layout_empty = view?.findViewById(R.id.layout_empty)
        btnEmptyAction = view?.findViewById(R.id.btn_empty_action)

        ResourceRunStateStore.ensureRegistered()
        ResourceRunStateStore.addListener(runStateListener)
        // Set up header view
        headerView = ToolHeaderItemView(requireContext()).apply {
            setDescription(getString(i8nR.string.workflow_section_tool_desc))
        }
        recycler_view?.setHeaderView(headerView)
        recycler_view?.setItemViewFactory(ToolFactory())

        btnEmptyAction?.setOnClickListener { handleEmptyAction() }
        updateEmptyState()
    }

    private fun loadTools() {
        val provider = mcpProvider
        if (provider == null) {
            DLog.w("FragmentMcpToolList", "MCP Provider 未初始化")
            updateUI()
            return
        }

        try {
            val tools = provider.getRegisteredTools()
            allTools.clear()
            allTools.addAll(tools)
            applyFilter()
            updateUI()

            DLog.i("FragmentMcpToolList", "加载工具列表: ${tools.size} 个工具")
        } catch (e: Exception) {
            DLog.e("FragmentMcpToolList", "加载工具列表失败: ${e.message}")
            updateUI()
        }
    }

    private fun updateUI() {
        updateEmptyState()
        if (toolList.isEmpty()) {
            recycler_view?.visibility = View.GONE
            layout_empty?.visibility = View.VISIBLE
        } else {
            recycler_view?.visibility = View.VISIBLE
            layout_empty?.visibility = View.GONE
        }

        recycler_view?.submitDataSetsWithType(toolList.map { Pair(0, it) })
        recycler_view?.notifyDataSetChanged()
    }

    private fun switchFilter(filter: ToolFilter) {
        if (currentFilter == filter) return
        currentFilter = filter
        applyFilter()
        updateUI()
    }

    private fun applyFilter() {
        val filtered = when (currentFilter) {
            ToolFilter.CUSTOM -> allTools.filter { it.extraType != McpConst.Tool_Type_BuildIn }
            ToolFilter.BUILTIN -> allTools.filter { it.extraType == McpConst.Tool_Type_BuildIn }
            ToolFilter.ALL -> allTools
        }
        toolList.clear()
        toolList.addAll(filtered)
    }

    private fun updateEmptyState() {
        when (currentFilter) {
            ToolFilter.CUSTOM -> {
                btnEmptyAction?.visibility = View.VISIBLE
                tvEmptyTitle?.text = getString(i8nR.string.mcp_tool_empty_custom_title)
                tv_empty_message?.text = getString(i8nR.string.mcp_tool_empty_custom_desc)
                btnEmptyAction?.text = getString(i8nR.string.create_center_title)
            }
            ToolFilter.BUILTIN -> {
                btnEmptyAction?.visibility = View.GONE
                tvEmptyTitle?.text = getString(i8nR.string.mcp_tool_empty_builtin_title)
                tv_empty_message?.text = getString(i8nR.string.mcp_tool_empty_builtin_desc)
            }
            ToolFilter.ALL -> {
                btnEmptyAction?.visibility = View.VISIBLE
                tvEmptyTitle?.text = getString(i8nR.string.mcp_tool_empty_title)
                tv_empty_message?.text = getString(i8nR.string.mcp_tool_empty_desc)
                btnEmptyAction?.text = getString(i8nR.string.create_center_title)
            }
        }
    }

    private fun handleEmptyAction() {
        if (currentFilter != ToolFilter.BUILTIN) {
            openCreationCenter()
        }
    }

    fun showFilterMenu(anchor: View) {
        val options = listOf(
            ToolFilter.CUSTOM to getString(i8nR.string.mcp_tool_filter_custom),
            ToolFilter.BUILTIN to getString(i8nR.string.mcp_tool_filter_builtin),
            ToolFilter.ALL to getString(i8nR.string.mcp_tool_filter_all)
        )
        PopMenuManager.instance.showMenu(anchor, options.map { it.second }, object :
            PopMenuManager.OnItemClickListener<String> {
            override fun onItemClicked(view: View, data: String, pos: Int) {
                switchFilter(options[pos].first)
            }
        })
    }

    fun getCurrentFilterLabel(): String {
        return getFilterLabel(currentFilter)
    }

    private fun getFilterLabel(filter: ToolFilter): String {
        return when (filter) {
            ToolFilter.CUSTOM -> getString(i8nR.string.mcp_tool_filter_custom)
            ToolFilter.BUILTIN -> getString(i8nR.string.mcp_tool_filter_builtin)
            ToolFilter.ALL -> getString(i8nR.string.mcp_tool_filter_all)
        }
    }

    private fun openCreationCenter() {
        context?.let { ActivityCreationCenter.start(it) }
    }

    private fun isCustomTool(tool: McpTool): Boolean {
        return tool.extraType != McpConst.Tool_Type_BuildIn
    }

    private fun getToolDisplayName(tool: McpTool): String {
        return if (tool.extraName.isNotBlank()) tool.extraName else tool.name
    }

    fun loadData() {
        if (mcpProvider != null) {
            loadTools()
        }
    }

    override fun onResume() {
        super.onResume()
        loadTools()
    }

    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)
        if (isVisibleToUser && isResumed) {
            // 如果provider为空，尝试获取
            if (mcpProvider == null) {
                mcpProvider = ComponentManager.getInstance()
                    .getProvider(IMcpProvider::class.java) as IMcpProvider?
            }
            loadTools()
        }
    }

    inner class ToolFactory : IListRecyclerViewFactory {
        override fun createItemView(viewType: Int): ListRecyclerItemView {
            return ToolItemView(requireContext())
        }
    }

    inner class ToolItemView(context: Context) : ListRecyclerItemView(context) {
        private val itemRoot: ResourceListItemView

        init {
            LayoutInflater.from(context).inflate(R.layout.item_mcp_tool, this, true)
            itemRoot = findViewById(R.id.item_tool_root)
            itemRoot.configure(
                resourceType = "tool",
                showDescription = true,
                showDeleteButton = false,
                showArrow = false,
                showStatusDot = false
            )
            itemRoot.setItemClickListener {
                val tool = itemData as? McpTool
                if (tool != null) {
                    val isCustom = isCustomTool(tool)
                    DLog.i("ToolAdapter", "点击工具: ${tool.name}")
                    val customTool = if (isCustom) ScriptMcpRegister.getCustomTool(tool.name) else null
                    context?.let { ctx ->
                        ActivityMcpToolDetail.start(
                            context = ctx,
                            toolName = tool.name,
                            toolDisplayName = getToolDisplayName(tool),
                            toolDescription = tool.description,
                            toolType = tool.extraType,
                            toolSchema = tool.inputSchema.toString(),
                            customTool = customTool
                        )
                    }
                }
            }
            itemRoot.setPlayClickListener {
                val tool = itemData as? McpTool
                if (tool != null) {
                    ResourceQuickRunController.runTool(
                        requireContext(),
                        ResourceQuickRunController.buildToolTarget(
                            tool,
                            ScriptMcpRegister.getCustomTool(tool.name)
                        )
                    )
                }
            }
        }

        override fun bindData(data: Any?) {
            val tool = data as? McpTool ?: return
            val isCustom = isCustomTool(tool)
            val running = ResourceRunStateStore.isToolRunning(tool.name)

            itemRoot.bindData(
                name = getToolDisplayName(tool),
                description = tool.description.ifBlank {
                    getString(i8nR.string.agent_tool_description)
                },
                isRunning = running,
                isCustom = isCustom
            )
        }
    }

    override fun onDestroyView() {
        ResourceRunStateStore.removeListener(runStateListener)
        super.onDestroyView()
    }
} 
