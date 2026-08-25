// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive

import android.content.Context
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import com.hive.app.script.R
import com.hive.base.BaseFragment
import com.hive.i8n.R as i8nR
import com.hive.net.data.HomeTabs
import com.hive.ui.design.DesignSpecPlaceholderFragment
import com.hive.ui.script.FragmentWorkflowPage
import com.hive.ui.profile.FragmentProfilePage
import com.hive.ui.agent.AgentMainFragment
import com.hive.utils.GCDefaultConst
import com.hive.utils.GlobalApp
import com.hive.utils.system.UIUtils
import com.hive.views.TabButtonLayout

object TabHelper {
    var tabHeight = UIUtils.dp2px(GlobalApp.getContext(), 64)
        private set

    /** 与 script-desgin App 底部 Nav 一致，使用 i8n 同步的 Lucide PNG（ic_*） */
    private fun getSelectedResIdByTag(tag: String): Int = when (tag) {
        "f2" -> i8nR.drawable.ic_message_square
        "f3" -> i8nR.drawable.ic_layers
        "f4" -> i8nR.drawable.ic_user_circle
        else -> i8nR.drawable.ic_compass
    }

    private fun getUnselectedResIdByTag(tag: String): Int = when (tag) {
        "f2" -> i8nR.drawable.ic_message_square
        "f3" -> i8nR.drawable.ic_layers
        "f4" -> i8nR.drawable.ic_user_circle
        else -> i8nR.drawable.ic_compass
    }


    fun createFragmentByTag(tag: String): BaseFragment {
        return when (tag) {
            "f2" -> AgentMainFragment()
            "f3" -> FragmentWorkflowPage()
            "f4" -> FragmentProfilePage()
            else -> DesignSpecPlaceholderFragment.newInstance(i8nR.string.design_nav_agent)
        }
    }

    /**
     * 获取底部按键tab；
     *
     * @return
     */
    private fun getButtonLayout(context: Context, tab: HomeTabs): TabButtonLayout? {
        val layout = TabButtonLayout(context)
        layout.gravity = Gravity.CENTER
//        if (tab.tag == "f1") {
//            layout.iconView?.setPadding(0, 0, 0, 0)
//            layout.iconView?.scaleX = 2.7f
//            layout.iconView?.scaleY = 2.7f
//            layout.iconView?.translationY = 10f * GlobalApp.DP
//        } else {
//            layout.setColorChecked(GlobalApp.getColor(com.hive.i8n.R.color.textColorPrimary))
//            layout.setColorUnchecked(GlobalApp.getColor(com.hive.i8n.R.color.colorTextSecondary))
//        }

        layout.setColorChecked(GlobalApp.getColor(com.hive.i8n.R.color.design_nav_active))
        layout.setColorUnchecked(GlobalApp.getColor(com.hive.i8n.R.color.design_nav_inactive))

        layout.tag = tab.tag
        layout.pluginViewClassName = tab.view
        layout.pluginName = tab.plugin
        layout.setChecked(false)
        layout.setNameChecked(tab.name)
        layout.setNameUnchecked(tab.name)
        if (TextUtils.isEmpty(tab.tag)) {
            return null
        }
        val resId = getSelectedResIdByTag(tab.tag)
        return if (resId != -1) { //正常代码
            if (TextUtils.isEmpty(tab.icon)) {
                layout.setDrawableChecked(resId)
                layout.setDrawableUnchecked(getUnselectedResIdByTag(tab.tag))
            } else {
                layout.setNetDrawable(tab.icon)
            }
            layout
        } else { //插件tab
            null
        }
    }


    fun initTabs(tabWrapper: LinearLayout) {
        val tabs = GCDefaultConst.getDefaultTabs()
        tabWrapper.removeAllViews()
        for (i in tabs.indices) {
            if (!tabs[i].isEnable) continue
            val lp = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            lp.weight = 1f
            val tabView: View? = getButtonLayout(tabWrapper.context, tabs[i])
            if (tabView != null) {
                tabWrapper.addView(tabView, lp)
            }
        }
        tabHeight = tabWrapper.layoutParams.height
    }


    val defaultTag: String
        get() {
            val tabs = GCDefaultConst.getDefaultTabs()
            return if (tabs.isEmpty()) "" else tabs[0].tag
        }

}
