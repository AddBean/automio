// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.files

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import com.hive.base.BaseFragmentActivity
import com.hive.libfiles.R
import com.hive.utils.utils.IntentUtils
import com.hive.views.IBackListener

/**
 *
 * @author jiadou
 * @email 172111432@qq.com
 * @date 4/29/21
 */

class XFileActivity : BaseFragmentActivity() {
    private lateinit var fragment: XFileFragment

    private var layout_back: View? = null

    override fun doOnCreate(savedState: Bundle?) {
        val transaction = supportFragmentManager.beginTransaction()
        fragment = XFileFragment()
        fragment.arguments = intent.extras
        transaction.replace(R.id.layout_content, fragment)
        transaction.commitAllowingStateLoss()
        layout_back= findViewById(R.id.layout_back)
        layout_back?.setOnClickListener {
            finish()
        }
    }

    override fun getLayoutId() = R.layout.activity_file_all

    override fun onBackPressed() {
        if (fragment is IBackListener) {
            if (!(fragment as IBackListener).onBackPressed()) {
                super.onBackPressed()
            }
        } else {
            super.onBackPressed()
        }
    }

    companion object {
        fun start(context: Context, path: String?) {
            IntentUtils.safeStartActivity(context, Intent(context, XFileActivity::class.java).apply {
                putExtras(Bundle().apply {
                    putString(XFileFragment.INTENT_KEY_TARGET_PATH, path)
                })
            })
        }
    }
}