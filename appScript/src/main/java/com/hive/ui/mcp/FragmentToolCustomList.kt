// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.ui.mcp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.hive.ui.widgets.ResourceListItemView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hive.app.script.R
import com.hive.i8n.R as i8nR
import com.hive.ui.common.DialogAppConfirm
import com.hive.plugin.mcp.McpConst
import com.hive.plugin.ComponentManager
import com.hive.plugin.mcp.model.McpTool
import com.hive.script.mcp.ScriptMcpRegister
import com.hive.script.base.ScriptConst
import com.hive.script.scope.LocalResourceListRepository
import com.hive.plugin.provider.IMcpProvider
import com.hive.plugin.provider.IScriptProvider
import com.hive.ui.common.ResourceQuickRunController
import com.hive.ui.common.ResourceRunStateStore
import com.hive.ui.mcp.ActivityCreateMcpTool
import com.hive.utils.debug.DLog
import com.hive.utils.GlobalApp
import com.hive.views.fragment.PagerFragment
import org.greenrobot.eventbus.EventBus
import com.hive.views.widgets.CommonToast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * MCP 工具列表页面
 *
 * @author jiadou
 * @email 172111432@qq.com
 * @date 2024/12/19
 */
class FragmentToolCustomList : PagerFragment() {

    var mcpProvider: IMcpProvider? = null

    private var scriptProvider: IScriptProvider? = null
    private var toolAdapter: ToolAdapter? = null
    private val toolList = mutableListOf<McpTool>()
    private var recycler_view: RecyclerView? = null
    private var add_tool_btn: View? = null
    private var cleanup_orphan_btn: View? = null
    private var tv_empty_message: TextView? = null
    private var layout_empty: View? = null
    private val runStateListener: () -> Unit = {
        toolAdapter?.notifyDataSetChanged()
    }
    override fun getLayoutId(): Int = R.layout.fragment_mcp_custom_list

    override fun initView() {
        super.initView()

        mcpProvider = ComponentManager.getInstance()
            .getProvider(IMcpProvider::class.java) as IMcpProvider?

        scriptProvider = ComponentManager.getInstance()
            .getProvider(IScriptProvider::class.java) as IScriptProvider?

        initViews()
        loadTools()
        // mView?.setPadding(0, 0, 0, TabHelper.tabHeight )
    }

    private fun initViews() {
        add_tool_btn = view?.findViewById(R.id.add_tool_btn)
        cleanup_orphan_btn = view?.findViewById(R.id.cleanup_orphan_btn)
        recycler_view = view?.findViewById(R.id.recycler_view)
        tv_empty_message = view?.findViewById(R.id.tv_empty_message)
        layout_empty = view?.findViewById(R.id.layout_empty)
        ResourceRunStateStore.ensureRegistered()
        ResourceRunStateStore.addListener(runStateListener)
        // 初始化 RecyclerView
        toolAdapter = ToolAdapter(toolList)
        recycler_view?.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = toolAdapter
        }

        // 设置空状态提示
        tv_empty_message?.text = getString(com.hive.i8n.R.string.agent_tool_list_empty)
        add_tool_btn?.setOnClickListener {
            ActivityCreateMcpTool.start(requireContext())
        }
        cleanup_orphan_btn?.setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                val result = scriptProvider?.cleanupOrphanSkillsAndTools()
                    ?: com.hive.plugin.provider.OrphanCleanupResult(0, 0, 0)
                withContext(Dispatchers.Main) {
                    val total = result.getTotalRemoved()
                    if (total > 0) {
                        CommonToast.getInstance().showToast(
                            GlobalApp.getString(
                                com.hive.i8n.R.string.sc_orphan_cleanup_toast,
                                result.skillsRemoved,
                                result.toolsRemoved,
                                result.customToolsRemovedFromSp
                            )
                        )
                        loadTools()
                        EventBus.getDefault().post(com.hive.script.event.RefreshScriptListEvent())
                    } else {
                        CommonToast.getInstance().showToast(com.hive.i8n.R.string.sc_orphan_cleanup_none)
                    }
                }
            }
        }
    }

    private fun loadTools() {
        val provider = mcpProvider
        if (provider == null) {
            DLog.w("FragmentMcpToolList", "MCP Provider 未初始化")
            updateUI()
            return
        }

        try {
            val tools = provider.getRegisteredTools().filter { it.extraType != McpConst.Tool_Type_BuildIn }
            toolList.clear()
            toolList.addAll(tools)

            updateUI()

            DLog.i("FragmentMcpToolList", "加载工具列表: ${tools.size} 个工具")
        } catch (e: Exception) {
            DLog.e("FragmentMcpToolList", "加载工具列表失败: ${e.message}")
            updateUI()
        }
    }

    private fun updateUI() {
        if (toolList.isEmpty()) {
            recycler_view?.visibility = View.GONE
            layout_empty?.visibility = View.VISIBLE
        } else {
            recycler_view?.visibility = View.VISIBLE
            layout_empty?.visibility = View.GONE
        }

        toolAdapter?.notifyDataSetChanged()
    }

    override fun onResume() {
        super.onResume()
        loadTools()
    }

    fun loadData() {
        if (mcpProvider != null) {
            loadTools()
        }
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

    inner class ToolAdapter(private val tools: List<McpTool>) :
        RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        private val TYPE_COUNT = 0
        private val TYPE_TOOL = 1

        override fun getItemViewType(position: Int): Int {
            return if (position == 0) TYPE_COUNT else TYPE_TOOL
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return when (viewType) {
                TYPE_COUNT -> {
                    val view = LayoutInflater.from(parent.context)
                        .inflate(R.layout.item_mcp_count, parent, false)
                    CountViewHolder(view)
                }

                else -> {
                    val view = LayoutInflater.from(parent.context)
                        .inflate(R.layout.item_mcp_tool_custom, parent, false)
                    ToolViewHolder(view)
                }
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            when (holder) {
                is CountViewHolder -> {
                    holder.bind(tools.size)
                }

                is ToolViewHolder -> {
                    val tool = tools[position - 1]
                    holder.bind(tool)
                }
            }
        }

        override fun getItemCount(): Int = tools.size + 1

        inner class CountViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            fun bind(count: Int) {
                itemView.findViewById<TextView>(R.id.tv_count_info)?.text = getString(com.hive.i8n.R.string.agent_tool_count_info, count)
            }
        }

        inner class ToolViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

            private val itemRoot: ResourceListItemView

            init {
                itemRoot = itemView.findViewById<ResourceListItemView>(R.id.item_tool_custom_root)
                itemRoot.configure(
                    resourceType = "tool",
                    showDescription = true,
                    showDeleteButton = true,
                    showArrow = false,
                    showStatusDot = true
                )
            }

            fun bind(tool: McpTool) {
                val running = ResourceRunStateStore.isToolRunning(tool.name)

                itemRoot.bindData(
                    name = tool.extraName,
                    description = tool.description,
                    isRunning = running,
                    isCustom = true
                )

                itemRoot.setPlayClickListener {
                    ResourceQuickRunController.runTool(
                        requireContext(),
                        ResourceQuickRunController.buildToolTarget(
                            tool,
                            ScriptMcpRegister.getCustomTool(tool.name)
                        )
                    )
                }

                itemRoot.setDeleteClickListener {
                    showDeleteConfirmDialog(tool)
                }

                itemRoot.setItemClickListener {
                    DLog.i("ToolAdapter", "点击工具: ${tool.extraName}")
                    val customTool = ScriptMcpRegister.getCustomTool(tool.name)
                    context?.let { ctx ->
                        ActivityMcpToolDetail.start(
                            context = ctx,
                            toolName = tool.name,
                            toolDisplayName = tool.extraName,
                            toolDescription = tool.description,
                            toolType = tool.extraType,
                            toolSchema = tool.inputSchema.toString(),
                            customTool = customTool
                        )
                    }
                }
            }
        }
    }

    /**
     * 显示删除确认对话框
     */
    private fun showDeleteConfirmDialog(tool: McpTool) {
        DialogAppConfirm.show(
            fragment = this,
            title = getString(com.hive.i8n.R.string.agent_tool_delete_title),
            content = getString(com.hive.i8n.R.string.agent_tool_delete_message, tool.extraName),
            cancelText = getString(com.hive.i8n.R.string.cancel),
            confirmText = getString(com.hive.i8n.R.string.delete),
            onConfirm = { deleteTool(tool) }
        )
    }

    /**
     * 删除工具
     */
    private fun deleteTool(tool: McpTool) {
        try {
            val provider = scriptProvider
            if (provider == null) {
                DLog.w("FragmentMcpToolList", "MCP Provider 未初始化")
                return
            }

            // 调用 MCP Provider 的删除方法
            provider.unregisterCustomTool(tool.name)
            // 同时删除本地 tool 目录，避免注册表与文件系统状态不一致
            val toolUid = tool.name.removePrefix(ScriptConst.SCRIPT_TOOL_ID_PREFIX)
            val toolDir = java.io.File(ScriptConst.Save_Tool_Path, toolUid)
            if (toolDir.exists()) {
                toolDir.deleteRecursively()
                DLog.i("FragmentMcpToolList", "Tool directory deleted: ${toolDir.absolutePath}")
            }
            val success = true // 假设删除成功，因为 unregisterTool 没有返回值
            if (success) {
                DLog.i("FragmentMcpToolList", "工具删除成功: ${tool.extraName}")
                // 重新加载工具列表
                loadTools()
                // 显示成功提示
                showToast(getString(com.hive.i8n.R.string.agent_tool_delete_success))
            } else {
                DLog.w("FragmentMcpToolList", "工具删除失败: ${tool.extraName}")
                showToast(getString(com.hive.i8n.R.string.agent_tool_delete_failed))
            }
        } catch (e: Exception) {
            DLog.e("FragmentMcpToolList", "删除工具时发生异常: ${e.message}")
            showToast(getString(com.hive.i8n.R.string.agent_tool_delete_error))
        }
    }

    /**
     * 显示提示信息
     */
    private fun showToast(message: String) {
        try {
            android.widget.Toast.makeText(requireContext(), message, android.widget.Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            DLog.e("FragmentMcpToolList", "显示 Toast 失败: ${e.message}")
        }
    }

    override fun onDestroyView() {
        ResourceRunStateStore.removeListener(runStateListener)
        super.onDestroyView()
    }
} 
