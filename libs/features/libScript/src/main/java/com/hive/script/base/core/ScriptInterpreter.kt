// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.base.core

import com.hive.script.base.ScriptCommand
import com.hive.script.base.ScriptCommand.CmdExecuteResult
import com.hive.script.base.ScriptCommandRoot
import com.hive.script.base.ScriptConst
import com.hive.script.base.ScriptOperateTimeManager
import com.hive.script.base.ScriptRecordHelper
import com.hive.script.exception.ScriptJumpException
import com.hive.script.extensions.forEachAllCommand
import com.hive.script.extensions.traverseCommand
import com.hive.script.extensions.updateChildParent
import com.hive.script.utils.ScriptHelper
import com.hive.utils.debug.DLog
import com.hive.utils.thread.UIHandlerUtils
import java.io.File
import java.util.concurrent.ArrayBlockingQueue

/**
 *
 * @author jiadou
 * @date 6/8/21
 */
class ScriptInterpreter private constructor() : Runnable {

    private var runningCommandRoot: ScriptCommandRoot? = null

    private var runningCommand: ScriptCommand? = null

    private var recordingCommand: ScriptCommand? = null

    private var executeThread: ScriptThread? = null

    private var isRunning = false

    private var queueTask = ArrayBlockingQueue<ScriptCommand?>(10)

    private var lastTime = 0L

    private var environment = ScripRunningEnv(this)

    private var onExecuteFinishCallbackOnce: (() -> Unit)? = null

    fun start() {
        if (isRunning) {
            DLog.w("ScriptInterpreter", "start() called but isRunning=true, ignoring")
            return
        }
        DLog.d("ScriptInterpreter", "start() creating new thread, executeThread=${executeThread != null}, queueSize=${queueTask.size}")
        initUnlockScript()
        ScriptThreadManager.stopAll()
        ScriptThreadManager.resume()
        executeThread = ScriptThread.newThread(this)
        executeThread?.start()
        DLog.d("ScriptInterpreter", "start() thread created, threadName=${executeThread?.name}")
    }

    fun stopExecute() {
        val stack = android.util.Log.getStackTraceString(Exception())
        DLog.w("ScriptInterpreter", "stopExecute() called, isRunning=$isRunning, executeThread=${executeThread?.name}, queueSize=${queueTask.size}\n$stack")
        lastTime = 0L
        environment.getJumpControl().stopJump()
        ScriptThreadManager.resume()
        queueTask.clear()
        runningCommandRoot = null
        ScriptThreadManager.stop()
        executeThread?.interrupt()
        executeThread = null
        runningCommand?.run {
            runningCommand?.forEachAllCommand {
                it.isRunning = false
            }
            runningCommand = null
            ScriptInterpreterObserver.notifyInterpreterTryStop(this)
        }
        isRunning = false
        DLog.d("ScriptInterpreter", "stopExecute() done")
    }

    override fun run() {
        doExecute()
    }

    private fun doExecute() {
        environment.getJumpControl().stopJump()
        isRunning = true
        val threadName = Thread.currentThread().name
        DLog.d("ScriptInterpreter", "doExecute() started on thread $threadName")
        while (!Thread.currentThread().isInterrupted && executeThread != null) {
            try {
                runningCommand = queueTask.take()
                DLog.d("ScriptInterpreter", "doExecute() took command: ${runningCommand?.javaClass?.simpleName}")
                if (runningCommand is ScriptCommandRoot) {
                    runningCommandRoot = runningCommand as ScriptCommandRoot
                    runningCommandRoot?.envRunning = environment
                }
                runningCommand?.updateChildParent()
                runningCommand?.run {
                    ScriptInterpreterObserver.notifyInterpreterStart(this)
                    try {
                        val result = executeScriptCommand(this)
                        DLog.d("ScriptInterpreter", "doExecute() command finished, result=$result")
                    } catch (e: Exception) {
                        DLog.e("ScriptInterpreter", "doExecute() command threw exception: ${e::class.simpleName} - ${e.message}")
                        throw e
                    } finally {
                        ScriptInterpreterObserver.notifyInterpreterEnd(this)
                    }
                }
            } catch (i: Exception) {
                DLog.e("ScriptInterpreter", "doExecute() caught exception in loop: ${i::class.simpleName} - ${i.message}")
                i.printStackTrace()
            }
        }
        val interrupted = Thread.currentThread().isInterrupted
        DLog.d("ScriptInterpreter", "doExecute() loop exited, interrupted=$interrupted, executeThread=$executeThread")
        runningCommandRoot = null
        executeThread = null
        isRunning = false
        ScriptHelper.runInMain {
            onExecuteFinishCallbackOnce?.invoke()
            onExecuteFinishCallbackOnce = null
        }
    }

    /**
     * 执行脚本命令
     */
    private fun executeScriptCommand(scriptCommand: ScriptCommand): CmdExecuteResult {
        try {
            return scriptCommand.doExecute()
        } catch (jumpExp: ScriptJumpException) {
            environment.getJumpControl().jumpTo(jumpExp.cmd)
            return executeScriptCommand(scriptCommand)
        }
    }

    /**
     * 暂停当前执行，然后跳转到指定的命令执行
     */
    fun jumpToCommand(targetCmd: ScriptCommand) {
        throw ScriptJumpException(targetCmd)
    }

    fun executeCommand(cmd: ScriptCommand, isRecording: Boolean, onFinishedInMain: (() -> Unit)? = null) {
        DLog.d("ScriptInterpreter", "executeCommand() isRecording=$isRecording, cmd=${cmd::class.simpleName}, executeThread=${executeThread?.name}, isInterrupted=${executeThread?.isInterrupted}, isRunning=$isRunning")
        onExecuteFinishCallbackOnce = onFinishedInMain
        //设置
        if (isRecording) {
            var delay = if (lastTime > 0) {
                System.currentTimeMillis() - lastTime
            } else {
                ScriptConst.Cmd_Default_Delay
            }
            ScriptOperateTimeManager.get().endOperate(cmd::class.java.name)
            //减去操作的时间
            delay -= ScriptOperateTimeManager.get().getOperateDuration()
            if (delay < 0) delay = 0
            lastTime = System.currentTimeMillis()
            recordingCommand?.startDelay = delay
            recordingCommand?.endDelay = delay
            recordingCommand = cmd
            ScriptOperateTimeManager.get().resetOperate()
        }
        ScriptThreadManager.resume()
        if (cmd is ScriptCommandRoot) {
            ScriptRecordHelper.instance.reset(cmd)
        }
        queueTask.clear()
        queueTask.add(cmd)
        if (executeThread == null || executeThread?.isInterrupted == true) {
            DLog.d("ScriptInterpreter", "executeCommand() starting new thread")
            start()
        } else {
            DLog.d("ScriptInterpreter", "executeCommand() reusing existing thread ${executeThread?.name}")
            UIHandlerUtils.getInstance().executeInMainThread {
                onFinishedInMain?.invoke()
            }
        }
    }

//    fun executeCommandSync(cmd: ScriptCommand): CmdExecuteResult? {
//        var executeResult: CmdExecuteResult? = null
//        ScriptThreadManager.resume()
//        if (cmd is ScriptCommandRoot) {
//            ScriptRecordHelper.instance.reset(cmd)
//        }
//        queueTask.clear()
//        queueTask.add(cmd)
//        initUnlockScript()
//        ScriptThreadManager.stopAll()
//        ScriptThreadManager.resume()
//        executeThread = null
//        isRunning = true
//        environment.getJumpControl().stopJump()
//        try {
//            runningCommand = queueTask.take()
//            if (runningCommand is ScriptCommandRoot) {
//                runningCommandRoot = runningCommand as ScriptCommandRoot
//                runningCommandRoot?.envRunning = environment
//            }
//            runningCommand?.updateChildParent()
//            runningCommand?.run {
//                try {
//                    ScriptInterpreterObserver.notifyInterpreterStart(this)
//                    executeResult = executeScriptCommand(this)
//                } finally {
//                    ScriptInterpreterObserver.notifyInterpreterEnd(this)
//                }
//            }
//        } catch (i: Exception) {
//            i.printStackTrace()
//        }
//        runningCommandRoot = null
//        isRunning = false
//        return executeResult
//    }

    fun isRecording(): Boolean {
        return runningCommand != null && runningCommand !is ScriptCommandRoot
    }

    fun isRunning() = isRunning


    fun getRunningScript(): ScriptCommandRoot? {
        return runningCommandRoot
    }

    /**
     * 初始化解锁屏幕的脚本
     */
    private fun initUnlockScript() {
        if (!File(ScriptConst.Task_Screen_Lock_Script_Main_Path).exists()) return
        sUnlockScript = ScriptCommandRoot().apply {
            ScriptCommandRoot.loadScriptSync(
                ScriptConst.Task_Screen_Lock_Script_Main_Path,
                this
            )
        }
        sUnlockScript?.enableAutoUnlock = false
        sUnlockScript?.traverseCommand {
            it.enableAutoUnlock = false
        }

    }

    companion object {

        private var sUnlockScript: ScriptCommandRoot? = null


        private val instanceDefault: ScriptInterpreter by lazy {
            ScriptInterpreter()
        }

        /**
         * 默认脚本执行的解释器
         */
        fun getDefault(): ScriptInterpreter {
            return instanceDefault
        }

        fun getUnlockScript(): ScriptCommandRoot? {
            if (sUnlockScript == null) {
                instanceDefault.initUnlockScript()
            }
            return sUnlockScript
        }
    }

}