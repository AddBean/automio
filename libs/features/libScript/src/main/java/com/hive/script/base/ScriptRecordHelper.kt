// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.base

import com.hive.script.base.core.ScriptInterpreterObserver
import com.hive.script.views.manager.ScriptManager
import com.hive.script.views.manager.ScriptRecordManager
import com.hive.utils.thread.UIHandlerUtils
import com.hive.script.utils.ScriptHelper
/**
 *
 * @author jiadou
 * @date 6/9/21
 */
class ScriptRecordHelper {

    var rootCommand: ScriptCommandRoot = ScriptCommandRoot()

    fun reset(cmd: ScriptCommandRoot? = null) {
        rootCommand = cmd ?: ScriptCommandRoot()
        if (rootCommand.scriptMate == null) {
            rootCommand.scriptMate = ScriptMate()
        }
        ScriptHelper.runInMain {
            ScriptManager.getLoggerView()?.resetDataView()
            ScriptRecordManager.getRecordInnerView()?.resetDataView()
        }
    }

    fun addCommand(command: ScriptCommand) {
        rootCommand.addCommandQueue(command)
        ScriptInterpreterObserver.notifyCommandRecordAdded(command)
    }

    fun removeLastCommand(): Boolean {
        if (rootCommand.commandQueue.isNotEmpty()) {
            val last = rootCommand.commandQueue.last()
            rootCommand.commandQueue.remove(last)
            ScriptInterpreterObserver.notifyCommandRecordRemoved(last)
            return true
        }
        return false
    }

    fun getRunningCommand(): ScriptCommand? {
        var cmd: ScriptCommand? = null
        traverseScript {
            if (it.isRunning) {
                cmd = it
                return@traverseScript
            }
        }
        return cmd
    }

    fun isRecordHasData() = rootCommand.commandQueue.isNotEmpty()

    fun getTotalCommandCount(): Pair<Int, Int> {
        var count = 0
        var curIndex = 0
        traverseScript {
            if (it.isRunning) {
                curIndex = count
            }
            count++
        }
        return count to curIndex
    }

    fun getRealCommandCount(): Int {
        var count = 0
        traverseScript {
            count++
        }
        return count
    }

    fun traverseScript(callback: (it: ScriptCommand) -> Unit) {
        innerTraverseScript(rootCommand, callback)
    }

    private fun innerTraverseScript(parent: ScriptCommand, function: (it: ScriptCommand) -> Unit) {
        parent.commandQueue.forEach {
            function.invoke(it)
            innerTraverseScript(it, function)
        }
    }

    companion object {
        @JvmStatic
        val instance: ScriptRecordHelper = ScriptRecordHelper()
    }
}