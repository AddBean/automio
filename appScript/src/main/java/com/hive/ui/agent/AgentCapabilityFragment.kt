// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.ui.agent

import android.view.View
import android.widget.TextView
import androidx.viewpager.widget.ViewPager
import com.hive.app.script.R
import com.hive.plugin.ComponentManager
import com.hive.plugin.provider.IMcpProvider
import com.hive.ui.mcp.FragmentMcpToolList
import com.hive.ui.skill.FragmentSkillList
import com.hive.views.fragment.PagerFragment
import com.hive.views.fragment.PagerFragmentAdapter
import com.hive.views.fragment.PagerTag

/**
 * 能力管理页面
 * 包含两个子 Tab：AI技能、AI工具
 */
class AgentCapabilityFragment : PagerFragment() {

    companion object {
        const val TAB_SKILL = 0
        const val TAB_TOOL = 1
    }

    private var mcpProvider: IMcpProvider? = null

    private var tabSkill: TextView? = null
    private var tabTool: TextView? = null
    private var btnFilterToolType: com.hive.views.widgets.TextDrawableView? = null
    private var viewPager: ViewPager? = null
    private var adapter: PagerFragmentAdapter? = null
    private var currentTabPosition = 0
    private var pendingTabPosition: Int? = null

    private val tabFragments = mutableListOf<com.hive.views.fragment.IPagerFragment>()

    /** 当切换到 AI 工具 tab 时，将筛选事件转发给工具列表 */
    interface OnToolFilterListener {
        fun showFilterMenu(anchor: View)
    }

    override fun getLayoutId(): Int = R.layout.fragment_agent_capability

    override fun initView() {
        // 获取 MCP 服务提供者
        mcpProvider = ComponentManager.getInstance()
            .getProvider(IMcpProvider::class.java) as IMcpProvider?

        tabSkill = view?.findViewById(R.id.tab_skill)
        tabTool = view?.findViewById(R.id.tab_tool)
        btnFilterToolType = view?.findViewById(R.id.btn_filter_tool_type)
        viewPager = view?.findViewById(R.id.view_pager)

        btnFilterToolType?.setOnClickListener {
            // 转发给当前工具列表 fragment
            val toolFragment = tabFragments.getOrNull(TAB_TOOL) as? FragmentMcpToolList
            toolFragment?.showFilterMenu(btnFilterToolType!!)
        }

        setupViewPager()
        setupTabClickListeners()
        updateTabStyles()
        pendingTabPosition?.let {
            pendingTabPosition = null
            navigateToTab(it)
        }
    }

    private fun setupViewPager() {
        adapter = PagerFragmentAdapter(childFragmentManager)
        viewPager?.adapter = adapter

        tabFragments.clear()

        // 子 Tab 0: AI技能
        val skillFragment = FragmentSkillList()
        skillFragment.setPagerTag(PagerTag(getString(com.hive.i8n.R.string.agent_tab_skill), 0))
        tabFragments.add(skillFragment)

        // 子 Tab 1: AI工具
        val toolFragment = FragmentMcpToolList()
        if (mcpProvider != null) {
            toolFragment.mcpProvider = mcpProvider
        }
        toolFragment.setPagerTag(PagerTag(getString(com.hive.i8n.R.string.agent_tab_tool), 1))
        tabFragments.add(toolFragment)

        adapter?.setFragments(tabFragments)
        adapter?.notifyDataSetChanged()

        viewPager?.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}

            override fun onPageSelected(position: Int) {
                currentTabPosition = position
                updateTabStyles()
                // 加载数据
                when (val fragment = tabFragments[position]) {
                    is FragmentMcpToolList -> fragment.loadData()
                }
            }

            override fun onPageScrollStateChanged(state: Int) {}
        })
    }

    private fun setupTabClickListeners() {
        tabSkill?.setOnClickListener {
            if (currentTabPosition != 0) {
                viewPager?.currentItem = 0
            }
        }

        tabTool?.setOnClickListener {
            if (currentTabPosition != 1) {
                viewPager?.currentItem = 1
            }
        }
    }

    private fun updateTabStyles() {
        val activeTextColor = requireContext().getColor(com.hive.i8n.R.color.textColorPrimary)
        val inactiveTextColor = requireContext().getColor(com.hive.i8n.R.color.colorTextSecondary)

        when (currentTabPosition) {
            0 -> {
                tabSkill?.setTextColor(activeTextColor)
                tabSkill?.setBackgroundResource(R.drawable.bg_filter_pill_amber)
                tabTool?.setTextColor(inactiveTextColor)
                tabTool?.setBackgroundResource(R.drawable.bg_filter_pill_inactive)
                // AI 技能 tab: 隐藏筛选按钮
                btnFilterToolType?.visibility = View.GONE
            }
            1 -> {
                tabSkill?.setTextColor(inactiveTextColor)
                tabSkill?.setBackgroundResource(R.drawable.bg_filter_pill_inactive)
                tabTool?.setTextColor(activeTextColor)
                tabTool?.setBackgroundResource(R.drawable.bg_filter_pill_sky)
                // AI 工具 tab: 显示筛选按钮
                btnFilterToolType?.visibility = View.VISIBLE
                val toolFragment = tabFragments.getOrNull(TAB_TOOL) as? FragmentMcpToolList
                toolFragment?.let {
                    btnFilterToolType?.text = it.getCurrentFilterLabel()
                }
            }
        }
    }

    fun navigateToResourceType(resourceType: String?) {
        when (resourceType?.lowercase()) {
            "skill" -> navigateToTab(TAB_SKILL)
            "tool" -> navigateToTab(TAB_TOOL)
        }
    }

    private fun navigateToTab(position: Int) {
        val pager = viewPager
        if (pager == null) {
            pendingTabPosition = position
            return
        }
        if (position in 0 until tabFragments.size && currentTabPosition != position) {
            pager.currentItem = position
        } else if (position == currentTabPosition) {
            updateTabStyles()
        }
    }
}
