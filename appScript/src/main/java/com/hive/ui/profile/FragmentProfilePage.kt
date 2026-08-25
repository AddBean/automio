// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.ui.profile

import android.view.ViewGroup
import com.hive.app.script.R
import com.hive.base.BaseFragment
import com.hive.ui.profile.ProfilePageView

/**
 * 个人 Tab：design-spec ProfilePage（script-desgin ProfilePage.tsx）
 */
class FragmentProfilePage : BaseFragment() {

    private var profileView: ProfilePageView? = null

    override fun getLayoutId(): Int = R.layout.fragment_profile_page

    override fun initView() {
        val container = view?.findViewById<ViewGroup>(R.id.container_profile) ?: return
        val v = ProfilePageView(requireContext())
        v.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        container.addView(v)
        profileView = v
    }

    override fun onResume() {
        super.onResume()
        profileView?.refreshAll()
    }
}
