// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script

import android.content.Intent
import android.provider.Settings
import android.widget.TextView
import com.hive.base.BaseActivity
import com.hive.script.driver.ServiceAccessibility
import com.hive.utils.system.CommonUtils
import java.io.File

/**
 *
 * @author jiadou
 * @date 6/18/21
 */
class ActivityScriptShortcut : BaseActivity() {

    private var scriptPath: String? = null
    private var tv_title: TextView? = null
    private var tv_btn_submit: TextView? = null
    private var tv_btn_cancel: TextView? = null

    override fun doOnCreate() {
        scriptPath = intent.getStringExtra("scriptPath")
        tv_title= findViewById(R.id.tv_title)
        tv_btn_submit = findViewById(R.id.tv_btn_submit)
        tv_btn_cancel = findViewById(R.id.tv_btn_cancel)
        tv_btn_cancel?.setOnClickListener {
            finish()
        }
        updateDialog()
    }

    private fun updateDialog() {
        if (CommonUtils.isAccessibilitySettingsOn(this@ActivityScriptShortcut, ServiceAccessibility::class.java.name)) {
            tv_title?.text = getString(com.hive.i8n.R.string.sc_short_run_title,
                File(scriptPath).name ?: "-"
            )
            tv_btn_submit?.text = getString(com.hive.i8n.R.string.sc_short_run_start)
            tv_btn_submit?.setOnClickListener {
                ScriptProvider().executeScript(scriptPath!!,true)
                finish()
            }

        } else {
            tv_title?.text = getString(com.hive.i8n.R.string.sc_short_permission_title)
            tv_btn_submit?.text = getString(com.hive.i8n.R.string.sc_short_permission_start)
            tv_btn_submit?.setOnClickListener {
                ScriptProvider.startToAccessibilitySetting()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        updateDialog()
    }

    override fun getLayoutId() = R.layout.activity_script_shortcut
}