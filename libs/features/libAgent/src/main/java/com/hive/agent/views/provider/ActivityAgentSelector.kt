// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.agent.views.provider

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.view.View
import com.hive.agent.R
import com.hive.base.BaseActivity
import com.hive.plugin.agent.InferenceType
import com.hive.utils.utils.IntentUtils

class ActivityAgentSelector : BaseActivity() {

    @SuppressLint("CommitTransaction")
    override fun doOnCreate() {
        findViewById<View>(R.id.ivBack).setOnClickListener {
            finish()
        }
        val type = InferenceType.parserType(intent.getIntExtra("type", 0))
        val aiSelectorFragment = AIModelSelectionFragment.newInstance(type)
        supportFragmentManager.beginTransaction()
            .replace(R.id.layoutFragment, aiSelectorFragment).commit()
    }

    override fun getLayoutId(): Int = R.layout.activity_agent_selector

    companion object {
        fun start(context: Context, type: InferenceType) {
            IntentUtils.safeStartActivityForResult(
                context,
                Intent(context, ActivityAgentSelector::class.java).apply {
                    putExtra("type", type.type)
                }, type.type
            )
        }
    }
}