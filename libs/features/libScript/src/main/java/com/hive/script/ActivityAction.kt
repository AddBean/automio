// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.hive.script.base.core.ScriptThreadManager
import com.hive.script.views.manager.ScriptMenuManager
import com.hive.utils.extends.string
import com.hive.utils.system.ClipboardUtil
import com.hive.utils.utils.IntentUtils
import com.hive.views.widgets.CommonToast
import com.hive.script.utils.ScriptHelper
class ActivityAction : Activity() {

    private var action: ActionEnum? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isRunning = true
        currentClipData = ""
        action = intent.getSerializableExtra("action") as ActionEnum
        ScriptHelper.runInMain({ doFinish() }, 1000)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            when (action) {
                ActionEnum.GET_CLIPBOARD -> {
                    currentClipData = ClipboardUtil.getInstance(this)
                        .getClipText(this) ?: ""
                    doFinish()
                }

                else -> {}
            }
        }
    }

    private fun doFinish() {
        isRunning = false
        finish()
    }

    enum class ActionEnum {
        GET_CLIPBOARD
    }

    companion object {


        private var currentClipData: String? = ""

        //线程安全的Bool
        private var isRunning = false

        fun isRunning(): Boolean {
            return isRunning
        }

        fun getClipData(): String {
            ScriptMenuManager.getMenuView()?.requestFocus()
            val data = ClipboardUtil.getInstance(ScriptProvider.getViewContext())
                .getClipText(ScriptProvider.getViewContext()) ?: ""
            if (data.isNotEmpty()) {
                return data
            }
            start(ActionEnum.GET_CLIPBOARD)
            var count = 10
            while (isRunning && currentClipData.isNullOrEmpty() && count > 0) {
                ScriptThreadManager.delay(100)
                count--
            }
            return currentClipData ?: ""
        }

        fun start(enum: ActionEnum) {
            isRunning = true
            when (enum) {
                ActionEnum.GET_CLIPBOARD -> {
                    currentClipData = ""
                }
            }
            ScriptHelper.runInMain {
                CommonToast.show(com.hive.i8n.R.string.sc_get_clipboard_running.string())
                val intent = Intent(ScriptProvider.getViewContext(), ActivityAction::class.java)
                intent.putExtras(Bundle().apply {
                    putSerializable("action", enum)
                })
                IntentUtils.safeStartActivity(ScriptProvider.getViewContext(), intent)
            }
        }
    }
}