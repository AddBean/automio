// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.ui.agent

import android.content.Context
import android.content.res.ColorStateList
import android.os.Build
import com.hive.app.script.R
import com.hive.i8n.R as i8nR
import com.hive.utils.GlobalApp
import com.hive.views.fragment.PagerTag
import com.hive.views.fragment.PagerTitleView
import com.hive.views.widgets.TextDrawableView
import com.hive.views.widgets.UIResourceIconView

/**
 * 智能体 Tab 标题视图
 */
class AgentTitleView(context: Context?) : PagerTitleView(context) {

    private var tv_title: TextDrawableView? = null

    override fun initView() {
        tv_title = findViewById(R.id.tv_title)
    }

    override fun onSetPagerTag(pagerTag: PagerTag) {
        tv_title?.text = pagerTag.name
        // 根据位置设置图标，使用 UIResourceIconView 的类型映射
        val iconRes = when (pagerTag.position) {
            0 -> R.drawable.ic_chat // AI对话
            1 -> i8nR.drawable.ic_resource_skill // 能力管理 -> UIResourceIconView.TYPE_SKILL
            else -> R.drawable.ic_chat
        }
        tv_title?.setDrawableLeft(GlobalApp.getDrawable(iconRes))
    }

    override fun getLayoutId(): Int {
        return R.layout.mcp_title_view
    }

    override fun onPageSelected(isSelected: Boolean, tag: PagerTag) {
        if (isSelected) {
            onTabClicked(1f)
        } else {
            onTabClicked(0f)
        }
    }

    override fun getMeasureWidthPercent() = 0.5f

    private fun onTabClicked(progress: Float) {
        super.onScrolling(progress)
        tv_title?.setTextColor(mixColors(0x5E6272, 0xffffff, 1 - progress))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            tv_title?.compoundDrawableTintList = ColorStateList(
                arrayOf(
                    intArrayOf()
                ),
                intArrayOf(
                    mixColors(0x5E6272, 0xffffff, 1 - progress)
                )
            )
        }
    }
}