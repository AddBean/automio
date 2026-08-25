// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.preview

import android.graphics.Canvas
import com.hive.script.BuildConfig
import com.hive.script.base.ScriptCommand
import com.hive.script.base.core.ScriptInterpreterObserver
import com.hive.script.utils.ScriptCommonDrawer
import com.hive.script.utils.ScriptCommonUtils
import com.hive.script.utils.ScriptHelper
import com.hive.script.views.record.impl.ScriptRecordView
import com.hive.utils.GlobalApp

/**
 *
 * @author jiadou
 * @email 172111432@qq.com
 * @date 6/9/21
 */

abstract class ScriptItemView : ScriptInterpreterObserver.CommandExecuteObserver {
    var totalCount: Int = 0

    var parentView: ScriptRecordView? = null

    var command: ScriptCommand? = null
        set(value) {
            field = value
            onCommandUpdate(value)
        }

    var index = 0

    val dp = GlobalApp.dp2px(1)

    fun dispatchDraw(canvas: Canvas) {
        onDraw(canvas)
        if (BuildConfig.DEBUG) {
            drawDebugInfo(canvas)
        }
    }

    private fun drawDebugInfo(canvas: Canvas) {
        command?.getNormalizedActiveArea()?.run {
            canvas.drawRect(ScriptCommonUtils.covertToRect(this), ScriptCommonDrawer.debugPaint)
        }
    }

    abstract fun onDraw(canvas: Canvas)

    fun isRunning() = command?.isRunning ?: false

    fun onViewAdded() {
        ScriptInterpreterObserver.registerCommandObserver(this)
    }

    fun onViewRemoved() {
        ScriptInterpreterObserver.unRegisterCommandObserver(this)
    }

    final override fun onCommandExecuteBefore(cmd: ScriptCommand) {
        if (cmd == command) ScriptHelper.runInMain {
            this@ScriptItemView.onExecuteBegin(cmd)
        }
    }

    final override fun onCommandExecuteEvent(type: Int, cmd: ScriptCommand, obj: Any?) {
        if (cmd == command) ScriptHelper.runInMain {
            try {
                this@ScriptItemView.onExecuteUpdate(type, cmd, obj)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    final override fun onCommandExecuteAfter(cmd: ScriptCommand) {
        if (cmd == command) ScriptHelper.runInMain {
            try {
                this@ScriptItemView.onExecuteEnd(cmd)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    open fun onExecuteBegin(cmd: ScriptCommand) {

    }

    open fun onExecuteUpdate(type: Int, cmd: ScriptCommand, obj: Any?) {

    }

    open fun onExecuteEnd(cmd: ScriptCommand) {

    }


    open fun onCommandUpdate(cmd: ScriptCommand?) {

    }

    fun getAlphaValueByIndex(): Float {
        var alpha = 0.06f + (1 - (totalCount - index) / 6f)
        if (alpha < 0.06) alpha = 0.06f
        if (alpha > 1f) alpha = 1f
        return alpha
    }

}