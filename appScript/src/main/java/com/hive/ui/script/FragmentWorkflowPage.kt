// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.ui.script

import android.view.View
import androidx.viewpager.widget.ViewPager
import com.hive.app.script.R
import com.hive.base.BaseFragment
import com.hive.ui.agent.AgentCapabilityFragment
import com.hive.ui.creation.ActivityCreationCenter
import com.hive.utils.ModeControl
import com.hive.views.fragment.PagerFragmentAdapter
import com.hive.views.fragment.PagerTag
import com.hive.views.widgets.UIRoundCornerTextView

/**
 * 主 Tab「工作流」页：双 Tab 结构（工作流 / AI 能力），对齐 script-design ResourcePage。
 * - Tab 0: 工作流列表（ScriptManagerLayoutForWorkflow）
 * - Tab 1: AI 能力管理（AgentCapabilityFragment，含 AI 技能 / AI 工具子 Tab）
 *
 * 当 enableAgentFeature=false 时，仅显示工作流 Tab，隐藏 AI 能力 Tab。
 */
class FragmentWorkflowPage : BaseFragment() {

    companion object {
        const val TAB_WORKFLOW = 0
        const val TAB_AI_CAPABILITY = 1

        fun isAgentFeatureEnabled(): Boolean = ModeControl.isAgentFeatureEnabled()
    }

    private var tabWorkflow: UIRoundCornerTextView? = null
    private var tabAiCapability: UIRoundCornerTextView? = null
    private var tabContainer: View? = null
    private var viewPager: ViewPager? = null
    private var adapter: PagerFragmentAdapter? = null
    private var fabAddResource: View? = null
    private var currentTabPosition = 0

    private val tabFragments = mutableListOf<com.hive.views.fragment.IPagerFragment>()
    private val enableAgentFeature = isAgentFeatureEnabled()

    override fun getLayoutId(): Int = R.layout.fragment_workflow_page

    override fun initView() {
        tabWorkflow = mView?.findViewById(R.id.tab_workflow)
        tabAiCapability = mView?.findViewById(R.id.tab_ai_capability)
        tabContainer = mView?.findViewById(R.id.tab_container)
        viewPager = mView?.findViewById(R.id.view_pager)
        fabAddResource = mView?.findViewById(R.id.fab_add_resource)

        // 根据 enableAgentFeature 控制 AI 能力 Tab 的显示
        if (!enableAgentFeature) {
            tabAiCapability?.visibility = View.GONE
        }

        fabAddResource?.setOnClickListener {
            context?.let { ActivityCreationCenter.start(it) }
        }

        adapter = PagerFragmentAdapter(childFragmentManager)
        viewPager?.adapter = adapter

        setupFragments()
        setupTabClickListeners()
        updateTabStyles()
    }

    private fun setupFragments() {
        tabFragments.clear()

        // Tab 0: 工作流列表
        val workflowPage = FragmentWorkflowList()
        workflowPage.setPagerTag(PagerTag(getString(com.hive.i8n.R.string.workflow_tab_workflow), 0))
        tabFragments.add(workflowPage)

        // Tab 1: AI 能力管理（仅在 enableAgentFeature=true 时添加）
        if (enableAgentFeature) {
            val capabilityFragment = AgentCapabilityFragment()
            capabilityFragment.setPagerTag(PagerTag(getString(com.hive.i8n.R.string.workflow_tab_ai_capability), 1))
            tabFragments.add(capabilityFragment)
        }

        adapter?.setFragments(tabFragments)
        adapter?.notifyDataSetChanged()

        viewPager?.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}

            override fun onPageSelected(position: Int) {
                currentTabPosition = position
                updateTabStyles()
            }

            override fun onPageScrollStateChanged(state: Int) {}
        })
    }

    private fun setupTabClickListeners() {
        tabWorkflow?.setOnClickListener {
            if (currentTabPosition != TAB_WORKFLOW) {
                viewPager?.currentItem = TAB_WORKFLOW
            }
        }

        // AI 能力 Tab 点击监听（仅在启用时生效）
        if (enableAgentFeature) {
            tabAiCapability?.setOnClickListener {
                if (currentTabPosition != TAB_AI_CAPABILITY) {
                    viewPager?.currentItem = TAB_AI_CAPABILITY
                }
            }
        }
    }

    private fun updateTabStyles() {
        val activeTextColor = requireContext().getColor(com.hive.i8n.R.color.textColorPrimary)
        val inactiveTextColor = requireContext().getColor(com.hive.i8n.R.color.colorTextSecondary)

        if (enableAgentFeature) {
            when (currentTabPosition) {
                TAB_WORKFLOW -> {
                    tabWorkflow?.setTextColor(activeTextColor)
                    tabWorkflow?.setBackgroundResource(R.drawable.bg_agent_subtab_selected)
                    tabAiCapability?.setTextColor(inactiveTextColor)
                    tabAiCapability?.background = null
                }
                TAB_AI_CAPABILITY -> {
                    tabWorkflow?.setTextColor(inactiveTextColor)
                    tabWorkflow?.background = null
                    tabAiCapability?.setTextColor(activeTextColor)
                    tabAiCapability?.setBackgroundResource(R.drawable.bg_agent_subtab_selected)
                }
            }
        } else {
            // 仅工作流 Tab 模式：始终显示选中状态
            tabWorkflow?.setTextColor(activeTextColor)
            tabWorkflow?.setBackgroundResource(R.drawable.bg_agent_subtab_selected)
        }
    }

    /** 导航到指定的资源类型子 tab，供外部调用 */
    fun navigateToResourceType(resourceType: String?) {
        when (resourceType?.lowercase()) {
            "workflow" -> {
                if (currentTabPosition != TAB_WORKFLOW) {
                    viewPager?.currentItem = TAB_WORKFLOW
                }
            }
            "skill", "tool" -> {
                // 仅在 enableAgentFeature=true 时导航到 AI 能力 Tab
                if (enableAgentFeature && currentTabPosition != TAB_AI_CAPABILITY) {
                    viewPager?.currentItem = TAB_AI_CAPABILITY
                }
                val capabilityFragment = tabFragments.getOrNull(TAB_AI_CAPABILITY) as? AgentCapabilityFragment
                capabilityFragment?.navigateToResourceType(resourceType)
            }
        }
    }
}
