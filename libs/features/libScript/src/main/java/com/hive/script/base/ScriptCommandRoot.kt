// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.base

import android.text.TextUtils
import com.hive.script.R
import com.hive.script.base.core.ScripRunningEnv
import com.hive.script.base.core.ScriptParser
import com.hive.script.base.core.ScriptReader
import com.hive.script.base.core.ScriptThreadManager
import com.hive.script.base.params.ScriptParamEnv
import com.hive.script.cmd.CmdScriptEnd
import com.hive.script.cmd.CmdScriptStart
import com.hive.script.exception.ScriptInterruptedException
import com.hive.script.extensions.updateParent
import com.hive.script.utils.ScriptHelper
import com.hive.utils.GlobalApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 *
 * @author jiadou
 * @date 6/8/21
 */
class ScriptCommandRoot : ScriptCommand() {

    var scriptMate: ScriptMate? = null

    var scriptPath: String? = null //脚本文件夹路径
        set(value) {
            field = value
            //检查是否/结尾
            if (!TextUtils.isEmpty(field) && !field!!.endsWith("/")) {
                field += "/"
            }
        }

    val envParam = ScriptParamEnv.getParamEnv()

    @Transient
    var envRunning: ScripRunningEnv? = null


    override fun onExecute(): CmdExecuteResult {
        ScriptThreadManager.ensurePlay()
        var loopCount = ScriptConst.scriptLoopCount
        val infinite = loopCount <= 0
        while (loopCount > 0 || infinite) {
            val iterator: MutableIterator<ScriptCommand> = commandQueue.iterator()
            while (iterator.hasNext()) {
                val it = iterator.next()
                it.doExecute()
                if (Thread.currentThread().isInterrupted) {
                    throw ScriptInterruptedException()
                }
            }
            loopCount--
        }
        return CmdExecuteResult.success()
    }

    override fun onExecuteJump(cmd: ScriptCommand?) {
        val iterator: MutableIterator<ScriptCommand> = commandQueue.iterator()
        while (iterator.hasNext()) {
            val it = iterator.next()
            it.doExecute()
            if (Thread.currentThread().isInterrupted) {
                throw ScriptInterruptedException()
            }
        }
    }

    override fun getScriptBasePath(): String {
        if (TextUtils.isEmpty(scriptPath)) {
            return ScriptConst.Save_Script_Temp_Path
        }
        return scriptPath!!
    }

    override fun getCommandLines(): String {
        val sb = StringBuilder()
        commandQueue.forEach {
            sb.append(it.getCommandLines())
            sb.append("\n")
        }
        return sb.toString()
    }

    fun ensureStartEnd() {
        if (commandQueue.isEmpty()) return

        val first = commandQueue.first()
        val start = commandQueue.firstOrNull { it is CmdScriptStart }
        if (first !is CmdScriptStart && start != null) {
            commandQueue.remove(start)
            commandQueue.add(0, start)
            start.parentCommand = this
        } else if (start == null) {
            val begin = CmdScriptStart()
            commandQueue.add(0, begin)
            begin.parentCommand = this
        }

        val last = commandQueue.last()
        val done = commandQueue.firstOrNull { it is CmdScriptEnd }
        if (last !is CmdScriptEnd && done != null) {
            commandQueue.remove(done)
            commandQueue.add(done)
            done.parentCommand = this
        } else if (done == null) {
            val end = CmdScriptEnd()
            commandQueue.add(end)
            end.parentCommand = this
        }
    }

    override fun getCommand(): String {
        val sb = StringBuilder()
        commandQueue.forEach {
            sb.append(it.getCommand())
            sb.append("\n")
        }
        return sb.toString()
    }

    override fun getCommandName() = GlobalApp.getString(com.hive.i8n.R.string.cmd_name_root)!!

    override fun getCommandIcon() = R.drawable.ic_sc_root

    override fun getCommandDescribe() = GlobalApp.getString(com.hive.i8n.R.string.cmd_des_root)!!

    override fun isGroupCommand(): Boolean = true

    override fun getPermissionRequest() = null

    override fun deepCopy(): ScriptCommandRoot {
        val root = ScriptCommandRoot()
        root.scriptMate = scriptMate
        root.scriptPath = scriptPath
        root.envRunning = envRunning
        commandQueue.forEach {
            root.addCommandQueue(it.deepCopy())
        }
        return root
    }

    companion object {

        private var currentRootCommand: ScriptCommandRoot? = null

        suspend fun loadScript(scriptPath: String, root: ScriptCommandRoot) {
            ScriptHelper.checkScriptPath(scriptPath)
            return withContext(Dispatchers.IO) {
                loadScriptSync(scriptPath, root)
            }
        }

        fun loadScriptSync(scriptPath: String, root: ScriptCommandRoot) {
            ScriptHelper.checkScriptPath(scriptPath)
            currentRootCommand = root
            val reader = ScriptReader(scriptPath, null)
            val parser = ScriptParser()
            root.scriptMate = parser.parserMate(reader)
            root.scriptPath = scriptPath
            parser.parserParams(reader, root.envParam)
            parser.parserCmd(reader, root)
            root.scriptMate = parser.parserMate(reader)
            ScriptMate.fullMateInfo(root)
            root.updateParent()
        }


        fun loadCommandList(
            cmdStrList: List<String>,
            root: ScriptCommandRoot
        ) {
            val realList = cmdStrList.toMutableList()
            val reader = ScriptReader(null, realList)
            val parser = ScriptParser()
            parser.parserCmd(reader, root)
            root.scriptMate = parser.parserMate(reader)
            ScriptMate.fullMateInfo(root)
            root.updateParent()
        }


        fun getCurrentRootCommand(): ScriptCommandRoot? {
            return currentRootCommand
        }
    }
}