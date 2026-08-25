// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.ui.schedule

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import com.carlos.ui.header.CommonHeader
import com.hive.app.script.R
import com.hive.base.BaseFragmentActivity
import com.hive.framework.coper.ScriptManagerImpl
import com.hive.script.views.schedule.ScriptFragmentSchedule
import com.hive.utils.utils.IntentUtils

class ActivityScheduledTasks : BaseFragmentActivity() {

    override fun getLayoutId(): Int = R.layout.activity_scheduled_tasks

    override fun doOnCreate(savedState: Bundle?) {
        findViewById<CommonHeader>(R.id.header)?.apply {
            setLeftClickListener { finish() }
            setRightClickListener {
                (supportFragmentManager.findFragmentById(R.id.fragment_container) as? ScriptFragmentSchedule)
                    ?.showCreateTimerDialog()
            }
        }
        if (savedState == null) {
            val fragment: Fragment = ScriptManagerImpl.retrieveTimerFragment().apply {
                arguments = (arguments ?: Bundle()).apply {
                    putBoolean(ScriptFragmentSchedule.ARG_SHOW_EMBEDDED_CREATE_ACTION, false)
                }
            }
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit()
        }
    }

    companion object {
        fun start(context: Context) {
            IntentUtils.safeStartActivity(
                context,
                Intent(context, ActivityScheduledTasks::class.java)
            )
        }
    }
}
