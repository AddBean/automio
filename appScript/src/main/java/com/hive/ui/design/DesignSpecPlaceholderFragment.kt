// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.ui.design

import android.os.Bundle
import android.widget.TextView
import androidx.annotation.StringRes
import com.hive.app.script.R
import com.hive.base.BaseFragment
import com.hive.i8n.R as i8nR
/**
 * design-spec 主 Tab 占位页；开源版仅保留本地 Agent / Workflow / Profile 相关能力。
 */
class DesignSpecPlaceholderFragment : BaseFragment() {

    @StringRes
    private var titleResId: Int = 0

    override fun initView() {
        val args = arguments
        titleResId = args?.getInt(ARG_TITLE_RES, 0) ?: 0
        val title = view?.findViewById<TextView>(R.id.tv_placeholder_title)
        val hint = view?.findViewById<TextView>(R.id.tv_placeholder_hint)
        if (titleResId != 0) {
            title?.text = getString(titleResId)
        }
        hint?.text = getString(i8nR.string.design_placeholder_hint)
    }

    override fun getLayoutId(): Int = R.layout.fragment_design_spec_placeholder

    companion object {
        private const val ARG_TITLE_RES = "title_res"

        fun newInstance(@StringRes titleResId: Int): DesignSpecPlaceholderFragment {
            val f = DesignSpecPlaceholderFragment()
            f.arguments = Bundle().apply {
                putInt(ARG_TITLE_RES, titleResId)
            }
            return f
        }
    }
}
