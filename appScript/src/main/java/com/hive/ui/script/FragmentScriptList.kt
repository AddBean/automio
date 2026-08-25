// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.ui.script

import android.view.ViewGroup
import com.hive.TabHelper
import com.hive.app.script.R
import com.hive.i8n.R as i8nR
import com.hive.framework.coper.ScriptManagerImpl
import com.hive.views.fragment.PagerFragment

class FragmentScriptList : PagerFragment() {

    private var layoutContent: ViewGroup? = null

    override fun initView() {
        layoutContent= view?.findViewById(R.id.layoutContent)
        view?.setPadding(0, 0, 0, TabHelper.tabHeight)
        val viewMain = ScriptManagerImpl.retrieveScriptManagerView(requireContext())
        layoutContent?.addView(viewMain)
    }

    override fun getLayoutId() = R.layout.fragment_script_list
}
