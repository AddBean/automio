// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.ui.script

import com.hive.TabHelper
import com.hive.app.script.R
import com.hive.i8n.R as i8nR
import com.hive.framework.coper.ScriptManagerImpl
import com.hive.views.fragment.PagerFragment

class FragmentScriptSchedule : PagerFragment() {
    override fun initView() {
        view?.setPadding(0, 0, 0, TabHelper.tabHeight)
        childFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, ScriptManagerImpl.retrieveTimerFragment())
            .commitAllowingStateLoss()
    }

    override fun getLayoutId() = R.layout.fragment_script_schedule
}
