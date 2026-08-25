// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.agent.views.provider

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.view.View
import com.hive.agent.R
import com.hive.base.BaseActivity
import com.hive.utils.utils.IntentUtils

class ActivityAgentSetting : BaseActivity() {

    private val aiFragment = AIServiceManagerFragment()


    @SuppressLint("CommitTransaction")
    override fun doOnCreate() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.layoutFragment, aiFragment).commit()
    }

    override fun getLayoutId(): Int = R.layout.activity_agent_setting

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        aiFragment.onActivityResult(requestCode, resultCode, data)
    }

    companion object {
        fun start(context: Context) {
            IntentUtils.safeStartActivity(
                context,
                Intent(context, ActivityAgentSetting::class.java)
            )
        }
    }
}