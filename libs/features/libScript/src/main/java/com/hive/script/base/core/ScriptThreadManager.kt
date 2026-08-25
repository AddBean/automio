// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.base.core

import com.blankj.utilcode.util.ThreadUtils
import com.hive.net.ServerTimeHelper
import com.hive.plugin.ComponentManager
import com.hive.plugin.provider.IPythonProvider
import com.hive.script.base.ScriptMate
import com.hive.script.exception.ScriptInterruptedException
import com.hive.script.views.manager.ScriptManager
import com.hive.utils.debug.DLog
import com.hive.views.widgets.CommonToast

/**
 * 脚本线程管理器
 * 支持暂停、恢复、停止脚本执行
 * 支持中断 Python 脚本执行（通过 IPythonExecutionControl）
 *
 * @author jiadou
 * @email 172111432@qq.com
 * @date 6/11/21
 */
object ScriptThreadManager {
    private val lock = Object()

    @Volatile
    private var pauseFlag = false

    private var thread: ScriptThread? = null

    fun checkInterrupted() {
        if (thread?.isInterrupted == true || thread?.isAlreadyStop == true) {
            throw ScriptInterruptedException()
        }
    }

    fun isExpired(time: Long?): Boolean {
        time ?: return false
        if (time <= 0L) return false   // 0表示永久有效
        val serverTime = ServerTimeHelper.getServerTimeMillis()
        return time in 1 until serverTime
    }

    private fun checkExpired(mate: ScriptMate?) {
        mate ?: return
        if (isExpired(mate.expireTime)) {
            CommonToast.getInstance().showToast(com.hive.i8n.R.string.script_expired)
            throw ScriptInterruptedException()
        }
    }

    fun delay(duration: Long) {
        if (duration <= 0) return
        thread = ScriptThread.currentThread()
        if (ThreadUtils.isMainThread()) {
            return
        }
        try {
            Thread.sleep(duration)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            return
        }
        while (pauseFlag) {
            try {
                synchronized(lock) {
                    lock.wait()
                }
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                return
            }
        }
        checkInterrupted()
        checkExpired(ScriptManager.getRunningScript()?.scriptMate)
    }

    fun pause() {
        pauseFlag = true
        ScriptManager.ctlControlView("pause")
    }

    fun isPaused() = pauseFlag

    fun resume() {
        pauseFlag = false
        ScriptManager.ctlControlView("start")
        synchronized(lock) {
            lock.notifyAll()
        }
    }

    fun stop() {
        val stack = android.util.Log.getStackTraceString(Exception())
        DLog.w("ScriptThreadManager", "stop() called, thread=${thread?.name}\n$stack")
        (ComponentManager.getInstance()
            .getProvider(IPythonProvider::class.java) as? IPythonProvider
                )?.setStopFlag()
        thread?.interrupt()
        resume()
        ScriptManager.ctlControlView("stop")
    }

    fun stopAll() {
        DLog.w("ScriptThreadManager", "stopAll() called")
        stop()
        ScriptThread.stopAll()
    }

    fun ensurePlay() {
        pauseFlag = false
        synchronized(lock) {
            lock.notifyAll()
        }
    }
}