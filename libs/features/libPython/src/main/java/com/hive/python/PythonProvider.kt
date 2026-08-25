// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.python

import android.content.Context
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.hive.plugin.provider.IPythonProvider
import com.hive.plugin.provider.IPythonOutputListener
import com.hive.utils.GlobalApp
import com.hive.utils.debug.DLog
import java.io.File

/**
 * Python 执行服务提供者
 * 使用 Chaquopy 在 Android 内嵌 Python 运行时执行代码
 * 职责：仅执行传入的可运行代码块，不做变量注入或输出包装
 * 支持实时输出回调，Python print() 会立即打印到 Logcat
 * 支持脚本中断控制，可从 Kotlin 端停止 Python 循环
 *
 * @author jiadou
 * @date 2025-03-05
 */
class PythonProvider : IPythonProvider {

    private var context: Context? = null
    private var outputListener: IPythonOutputListener? = null

    // 执行控制标志（原子操作保证线程安全）
    @Volatile
    private var stopFlag = false

    override fun init(context: Context) {
        this.context = context.applicationContext
    }

    private fun ensurePythonStarted() {
        if (!Python.isStarted()) {
            context?.let { Python.start(AndroidPlatform(it)) }
        }
    }

    override fun isAvailable(): Boolean {
        return try {
            ensurePythonStarted()
            true
        } catch (e: Exception) {
            false
        }
    }

    override fun setOutputListener(listener: IPythonOutputListener?) {
        this.outputListener = listener
    }

    // IPythonExecutionControl 实现
    override fun shouldStop(): Boolean = stopFlag

    override fun setStopFlag() {
        stopFlag = true
        DLog.w("PythonProvider", "设置停止标志，Python 脚本将立即退出")
    }

    /**
     * 重置停止标志（新脚本执行前调用）
     */
    private fun resetStopFlag() {
        stopFlag = false
    }

    override fun executeCode(code: String): IPythonProvider.Result {
        return runScript(code)
    }

    override fun executeFile(filePath: String): IPythonProvider.Result {
        val file = File(filePath)
        if (!file.exists()) {
            return IPythonProvider.Result(
                -1,
                "",
                GlobalApp.getString(com.hive.i8n.R.string.script_python_file_not_exist, filePath)
            )
        }
        return runScript(file.readText())
    }

    private fun runScript(script: String): IPythonProvider.Result {
        return try {
            ensurePythonStarted()

            // 重置停止标志（新执行开始）
            resetStopFlag()

            val py = Python.getInstance()
            val module = py.getModule("runner")

            // 创建实时输出监听器（内部实现）
            val realtimeListener = createRealtimeListener()

            // 传递监听器对象 + 执行控制对象到 Python
            val result = module.callAttr("run_script", script, realtimeListener, this)

            val exitCode = result.asList()[0].toInt()
            val output = result.asList()[1].toString()
            val error = result.asList()[2].toString()

            // 检查是否因中断而退出
            if (stopFlag && exitCode == 0) {
                DLog.w("PythonProvider", "脚本已中断执行")
            }

            IPythonProvider.Result(exitCode, output, error)
        } catch (e: Exception) {
            DLog.e("PythonProvider", "执行失败: ${e.message}")
            IPythonProvider.Result(
                -1,
                "",
                GlobalApp.getString(com.hive.i8n.R.string.script_python_exec_failed_msg, e.message ?: "")
            )
        } finally {
            // 执行完成后重置标志
            resetStopFlag()
        }
    }

    /**
     * 创建实时输出监听器实现
     * 内部监听器：打印到 Logcat（DLog）
     * 外部监听器：回调给上层（用户设置的监听器）
     */
    private fun createRealtimeListener(): IPythonOutputListener {
        return object : IPythonOutputListener {
            override fun onStdout(text: String) {
                // 打印到 Logcat（实时）
                DLog.d("Python_stdout", text)

                // 回调外部监听器（如果有）
                outputListener?.onStdout(text)
            }

            override fun onStderr(text: String) {
                // 打印到 Logcat（实时）
                DLog.e("Python_stderr", text)

                // 回调外部监听器（如果有）
                outputListener?.onStderr(text)
            }
        }
    }
}
