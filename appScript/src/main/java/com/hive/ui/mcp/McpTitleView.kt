// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.ui.mcp

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

class McpTitleView(context: Context?) : PagerTitleView(context) {

    private var tv_title: TextDrawableView? = null

    override fun initView() {
        tv_title = findViewById(R.id.tv_title)
    }

    override fun onSetPagerTag(pagerTag: PagerTag) {

        tv_title?.text = pagerTag.name
        val resourceType = when (pagerTag.obj) {
            0 -> UIResourceIconView.TYPE_TOOL
            1 -> UIResourceIconView.TYPE_WORKFLOW
            2 -> UIResourceIconView.TYPE_SKILL
            else -> UIResourceIconView.TYPE_TOOL
        }
        tv_title?.setDrawableLeft(getResourceIconDrawable(resourceType))
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

    /** Returns the default icon drawable for a UIResourceIconView type. */
    private fun getResourceIconDrawable(type: Int) = when (type) {
        UIResourceIconView.TYPE_WORKFLOW -> GlobalApp.getDrawable(i8nR.drawable.ic_resource_workflow)
        UIResourceIconView.TYPE_SKILL -> GlobalApp.getDrawable(i8nR.drawable.ic_resource_skill)
        UIResourceIconView.TYPE_TOOL -> GlobalApp.getDrawable(i8nR.drawable.ic_resource_tool)
        else -> GlobalApp.getDrawable(R.drawable.ic_resource)
    }
} 