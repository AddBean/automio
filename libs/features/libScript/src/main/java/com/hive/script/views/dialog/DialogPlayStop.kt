// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.dialog

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.hive.script.R
import com.hive.script.base.ScriptCommand
import com.hive.script.views.widgets.BaseScriptTips
import com.hive.utils.GlobalApp
import com.hive.utils.utils.StringUtils
import java.lang.ref.WeakReference

/**
 *
 * @author jiadou
 * @date 7/15/21
 */
class DialogPlayStop(context: Context) : BaseScriptTips(context) {

    class Task(var hostRef: WeakReference<DialogPlayStop>) : Runnable {
        override fun run() {
            hostRef.get()?.dismiss()
        }

    }

    lateinit var mTask: Task

    lateinit var handlers: Handler


    override fun initWindow() {
        super.initWindow()
        mTask = Task(WeakReference(this))
        handlers = Handler(Looper.getMainLooper())
        handlers.postDelayed(mTask, 3000)
        setTitleText(GlobalApp.getString(com.hive.i8n.R.string.dialog_play_stop_title))
        setOptEnable(false)
    }

    fun loadCmd(cmd: ScriptCommand?, time: Long): DialogPlayStop {
        setMsgText(
            String.format(
                getString(com.hive.i8n.R.string.dialog_play_stop_time),
                StringUtils.formatMMSSTime(time)
            )
        )
        return this
    }

    override fun onDismiss() {
        super.onDismiss()
        handlers.removeCallbacksAndMessages(mTask)
    }

    override fun isTouchOutsideDismissed() = true

}