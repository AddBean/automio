// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.agent.views

import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.hive.agent.R
import com.hive.base.BaseFragment

class FragmentAgentDebug : BaseFragment() {

    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager2
    private lateinit var pagerAdapter: AgentPagerAdapter

    override fun initView() {
        initializeViews()
        setupViewPager()
        setupTabLayout()
    }

    private fun initializeViews() {
        tabLayout = view?.findViewById(R.id.tabLayout) ?: return
        viewPager = view?.findViewById(R.id.viewPager) ?: return
    }

    private fun setupViewPager() {
        pagerAdapter = AgentPagerAdapter(requireActivity())
        viewPager.adapter = pagerAdapter
    }

    private fun setupTabLayout() {
        // 设置Tab标题
        tabLayout.addTab(tabLayout.newTab().setText(getString(com.hive.i8n.R.string.agent_tab_chat)))
        tabLayout.addTab(tabLayout.newTab().setText(getString(com.hive.i8n.R.string.agent_tab_debug)))
        // 连接TabLayout和ViewPager2
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                tab?.let {
                    viewPager.currentItem = it.position
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
                // 不需要处理
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
                // 不需要处理
            }
        })

        // 监听ViewPager2的页面变化
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                tabLayout.selectTab(tabLayout.getTabAt(position))
            }
        })
    }

    override fun getLayoutId() = R.layout.fragment_agent_debug_main
}