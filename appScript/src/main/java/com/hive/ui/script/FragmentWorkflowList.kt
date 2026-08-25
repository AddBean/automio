// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.ui.script

import android.view.ViewGroup
import android.widget.FrameLayout
import com.hive.app.script.R
import com.hive.base.BaseFragment
import com.hive.script.views.ScriptManagerLayoutForWorkflow
import com.hive.views.fragment.PagerFragment

/**
 * 工作流列表 Fragment：包裹 ScriptManagerLayoutForWorkflow，作为 FragmentWorkflowPage 的 Tab 0。
 */
class FragmentWorkflowList : PagerFragment() {

    override fun getLayoutId(): Int = R.layout.fragment_workflow_list

    override fun initView() {
        val layoutContent = view?.findViewById<FrameLayout>(R.id.layoutContent)
        val main = ScriptManagerLayoutForWorkflow(requireContext()).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        layoutContent?.addView(main)
    }
}
