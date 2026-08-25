// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.richeditor

import android.os.Bundle
import android.view.View
import com.hive.base.BaseFragmentActivity
import com.hive.editor.R
import com.hive.richeditor.views.text.EditTextFragment
import com.hive.utils.GlobalApp
import com.hive.utils.system.SystemProperty

/**
 * @author jiadou
 * @email 172111432@qq.com
 * @date 2021/10/21
 */
class ActivityTextEditor : BaseFragmentActivity() {
    private lateinit var mEditFragment: EditTextFragment

    private var layout_root: View? = null

    override fun doOnCreate(savedState: Bundle?) {
        layout_root = findViewById(R.id.layout_root)
        layout_root?.setPadding(0, SystemProperty.getStatusBarHeight(GlobalApp.getContext()), 0, 40 * DP)
        mEditFragment = EditTextFragment().apply {
            val arg = Bundle()
            arg.putString(FILE_KEY, intent.extras?.getString(FILE_KEY))
            arguments = arg
        }

        supportFragmentManager.beginTransaction().replace(R.id.layout_root, mEditFragment, "edit_text_fragment").commitAllowingStateLoss()
    }

    override fun getLayoutId(): Int = R.layout.activity_text_editor


    override fun onBackPressed() {
        if (!mEditFragment.onBackPressed()) {
            super.onBackPressed()
        }
    }

    companion object {
        @JvmStatic
        var FILE_KEY: String? = "file"

    }
}