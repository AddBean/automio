// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.agent.views

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class AgentPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {

    companion object {
        const val TAB_CHAT = 0
        const val TAB_DEBUG = 1
        const val TAB_COUNT = 2
    }

    override fun getItemCount(): Int = TAB_COUNT

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            TAB_DEBUG -> AgentDebugFragment()
            TAB_CHAT -> AgentChatFragment()
            else -> throw IllegalArgumentException("Invalid position: $position")
        }
    }
} 