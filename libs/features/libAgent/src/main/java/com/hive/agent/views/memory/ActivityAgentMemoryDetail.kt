// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.agent.views.memory

import android.content.Context
import android.content.Intent
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.hive.agent.R
import com.hive.agent.utils.AgentMemoryNoteHelper
import com.hive.base.BaseActivity
import com.hive.utils.system.ClipboardUtil
import com.hive.utils.utils.IntentUtils
import com.hive.views.widgets.CommonToast

class ActivityAgentMemoryDetail : BaseActivity() {

    private var tvContent: TextView? = null
    private var btnCopy: Button? = null
    private var btnDelete: Button? = null

    private var memoryKey: String = ""
    private var memoryValue: String = ""

    override fun doOnCreate() {
        memoryKey = intent.getStringExtra(EXTRA_KEY).orEmpty()
        memoryValue = intent.getStringExtra(EXTRA_VALUE).orEmpty()

        val header = findViewById<com.carlos.ui.header.CommonHeader>(R.id.header)
        header?.setCenterText(memoryKey.ifEmpty { getString(com.hive.i8n.R.string.agent_memory_title) })

        tvContent = findViewById(R.id.tvContent)
        btnCopy = findViewById(R.id.btnCopy)
        btnDelete = findViewById(R.id.btnDelete)

        tvContent?.text = memoryValue

        btnCopy?.setOnClickListener {
            ClipboardUtil.getInstance(this).copyText(memoryKey, memoryValue)
            CommonToast.getInstance().showToast(com.hive.i8n.R.string.sc_copy_success)
        }

        btnDelete?.setOnClickListener {
            AgentMemoryNoteHelper.removeMemory(memoryKey)
            Toast.makeText(this, com.hive.i8n.R.string.agent_memory_deleted, Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun getLayoutId(): Int = R.layout.activity_agent_memory_detail

    companion object {
        private const val EXTRA_KEY = "extra_key"
        private const val EXTRA_VALUE = "extra_value"

        fun start(context: Context, key: String, value: String) {
            IntentUtils.safeStartActivity(
                context,
                Intent(context, ActivityAgentMemoryDetail::class.java).apply {
                    putExtra(EXTRA_KEY, key)
                    putExtra(EXTRA_VALUE, value)
                }
            )
        }
    }
}
