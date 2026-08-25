// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.base

import android.graphics.RectF
import android.text.TextUtils
import com.hive.script.base.core.ScriptInterpreter
import com.hive.script.base.core.ScriptInterpreterObserver
import com.hive.script.base.core.ScriptParser
import com.hive.script.base.core.ScriptRegularInterface
import com.hive.script.base.core.ScriptThreadManager
import com.hive.script.base.params.ScriptParamEnv
import com.hive.script.driver.ScriptEventHelper
import com.hive.script.extensions.getCommandFirstLine
import com.hive.script.setting.ScriptSetting
import com.hive.script.utils.ScriptCommonUtils
import com.hive.script.utils.ScriptHelper
import com.hive.script.views.beans.PointVectorFloat
import com.hive.utils.GlobalApp
import com.hive.utils.file.FileUtils
import java.io.File
import java.io.Serializable

/**
 *
 * @author jiadou
 * @date 6/8/21
 */
@Suppress("UNREACHABLE_CODE")
abstract class ScriptCommand : IScriptFileInterface, Serializable {

    //注释备注
    var comment: String? = ""

    var type: Int = -1

    var isRunning = false

    var startDelay = ScriptConst.Cmd_Default

    var endDelay = ScriptConst.Cmd_Default

    //识别或命令有效区域，归一化坐标
    var limitRect = RectF(0f, 0f, 1f, 1f)

    var offsetVector = PointVectorFloat()

    var dragType: Int = 1

    var dragDuration: Long = ScriptConst.Cmd_Drag_DURATION

    var dragPressDuration: Long = ScriptConst.Cmd_Long_Click_Default

    var dragVector = PointVectorFloat()

    var replayTimes = 1

    var attachmentFiles: List<String>? = null

    var conditionReverse = false//是否取反

    var conditionMeetAll = false//是否全部满足条件

    @Transient
    var commandQueue: MutableList<ScriptCommand> = mutableListOf()

    @Transient
    var conditionList: MutableList<ScriptCommandCondition>? = null

    @Transient
    var enableAutoUnlock = false

    @Transient
    var parentCommand: ScriptCommand? = null

    @Transient
    var unReachable = false //该命令无法被运行到，可能前面存在死循环指令

    @Synchronized
    open fun doExecute(): CmdExecuteResult {
        var executeResult = CmdExecuteResult.success()
        if (!Thread.currentThread().isInterrupted) {
            if (!isGroupCommand()) {
                isRunning = true
            }
            //检查是否需要解锁屏幕
            if (checkIfNeedUnlock()) {
                startUnlockScreen()
            }
            executeResult = executeCommand()
            isRunning = false
        }
        return executeResult
    }

    private fun executeCommand(): CmdExecuteResult {
        ScriptThreadManager.checkInterrupted()
        var executeResult = CmdExecuteResult.success()
        val env = getRunningEnvironment()
        val jumpModel = env?.getJumpControl()?.isJumpModel() == true
        if (!jumpModel) {
            ScriptInterpreterObserver.notifyCommandExecuteBefore(this)
        }
        try {
            if (!jumpModel) {
                if (checkCondition()) {
                    executeResult = startExecuteInner()
                }
                val executeDelay = ScriptCommonUtils.getRandomDuration(startDelay, endDelay)
                ScriptInterpreterObserver.notifyCommandExecuteWait(this, executeDelay)
                ScriptThreadManager.delay(executeDelay)
            } else {
                if (env.getJumpControl().checkJump(this)) {
                    env.getJumpControl().stopJump()
                    ScriptInterpreterObserver.notifyCommandExecuteBefore(this)
                    executeResult = startExecuteInner()
                } else {
                    onExecuteJump(env.getJumpControl().getJumpCommand())
                }
            }
        } finally {
            if (!jumpModel) {
                ScriptInterpreterObserver.notifyCommandExecuteAfter(this)
            }
        }
        return executeResult
    }

    open fun onExecuteJump(cmd: ScriptCommand?) {}

    protected fun startExecuteInner(): CmdExecuteResult {
        return onExecute()
    }

    protected abstract fun onExecute(): CmdExecuteResult

    open fun onConditionMeet(condition: ScriptCommandCondition?) {

    }

    open fun checkCondition(): Boolean {
        if (conditionList?.isNotEmpty() == true) {
            ScriptThreadManager.delay(ScriptConst.Cmd_Default_Condition_Delay)
            var conditionResult: ScriptCommandCondition? = null
            var result = if (conditionMeetAll) {
                val meet = conditionList?.all {
                    it.isMeet(this)
                } == true
                if (meet) {
                    conditionResult = conditionList?.first()
                }
                meet
            } else {
                conditionResult = conditionList?.find {
                    it.isMeet(this)
                }
                conditionResult != null
            }
            if (conditionReverse) {
                result = !result
            }
            if (result) {
                onConditionMeet(conditionResult)
            }
            return result
        }
        return true
    }

    fun hasCondition(): Boolean {
        return conditionList?.isNotEmpty() == true
    }

    fun isConditionReverse(): Boolean {
        return conditionReverse
    }

    /**
     * 获取真正写入文件的命令行。Phase 0.6 格式：缩进 + 命令 + @delay(...) @rect(...) + #注释（修饰符在命令之后）
     * if for 等 group 命令需要重写该方法
     */
    open fun getCommandLines(): String {
        val indentStr = getCommandIndentation()
        val commandStr = getCommand()
        val commonStr = getCommonLinesInfo()
        val commentStr = if (!comment.isNullOrEmpty()) " #$comment" else ""
        return "$indentStr$commandStr$commonStr$commentStr"
    }

    /**
     * 行级修饰符，输出 @ 注解风格（Phase 0.6）：置于命令之后，采用 kv 格式。
     * 如 " @delay(start=500,end=1000) @rect(left=0.1,top=0.2,right=0.9,bottom=0.8)"
     */
    protected fun getCommonLinesInfo(): String {
        val delayStr = if (isSupportDelay()) "@delay(start=$startDelay,end=$endDelay)" else ""
        val rectStr =
            if (isSupportRect()) "@rect(left=${limitRect.left},top=${limitRect.top},right=${limitRect.right},bottom=${limitRect.bottom})" else ""
        val offsetStr =
            if (isSupportOffset()) "@drift(fromX=${offsetVector.fromX},fromY=${offsetVector.fromY},toX=${offsetVector.toX},toY=${offsetVector.toY})" else ""
        val dragStr =
            if (isSupportDrag()) "@drag(fromX=${dragVector.fromX},fromY=${dragVector.fromY},toX=${dragVector.toX},toY=${dragVector.toY},type=$dragType,duration=$dragDuration,pressDuration=$dragPressDuration)" else ""
        val parts = listOf(delayStr, rectStr, offsetStr, dragStr).filter { !TextUtils.isEmpty(it) }
        return if (parts.isEmpty()) "" else " " + parts.joinToString(" ") { it }
    }

    /**
     * 是否支持delay操作
     */
    open fun isSupportDelay(): Boolean = true

    /**
     * 是否支持区域识别操作
     */
    open fun isSupportRect(): Boolean = false

    /**
     * 是否支持偏移
     */
    open fun isSupportOffset(): Boolean = false

    /**
     * 是否支持拖曳
     */
    open fun isSupportDrag(): Boolean = false

    /**
     * 正常解析应该调用该方法，去除缩进、解析注释等
     */
    private fun parseCmdLine(line: String) {
        val cmdMap = ScriptParser.parserCmdLine(line)
        if (this@ScriptCommand is ScriptRegularInterface) {
            parseCmd(cmdMap["cmdLine"] ?: "")
        }
        startDelay = cmdMap["delayStart"]?.toLongOrNull() ?: ScriptConst.Cmd_Default
        endDelay = cmdMap["delayEnd"]?.toLongOrNull() ?: ScriptConst.Cmd_Default
        limitRect.left = cmdMap["rectLeft"]?.toFloat() ?: 0f
        limitRect.top = cmdMap["rectTop"]?.toFloat() ?: 0f
        limitRect.right = cmdMap["rectRight"]?.toFloat() ?: 1f
        limitRect.bottom = cmdMap["rectBottom"]?.toFloat() ?: 1f

        offsetVector.fromX = cmdMap["offsetFromX"]?.toFloatOrNull() ?: 0f
        offsetVector.fromY = cmdMap["offsetFromY"]?.toFloatOrNull() ?: 0f
        offsetVector.toX = cmdMap["offsetToX"]?.toFloatOrNull() ?: 0f
        offsetVector.toY = cmdMap["offsetToY"]?.toFloatOrNull() ?: 0f

        dragVector.fromX = cmdMap["dragFromX"]?.toFloatOrNull() ?: 0f
        dragVector.fromY = cmdMap["dragFromY"]?.toFloatOrNull() ?: 0f
        dragVector.toX = cmdMap["dragToX"]?.toFloatOrNull() ?: 0f
        dragVector.toY = cmdMap["dragToY"]?.toFloatOrNull() ?: 0f

        dragType = cmdMap["dragType"]?.toIntOrNull() ?: 0
        dragDuration = cmdMap["dragDuration"]?.toLongOrNull() ?: ScriptConst.Cmd_Drag_DURATION
        dragPressDuration =
            cmdMap["dragPressDuration"]?.toLongOrNull() ?: ScriptConst.Cmd_Long_Click_Default

        comment = cmdMap["comment"]
    }

    abstract fun getCommand(): String

    abstract fun getCommandName(): String

    abstract fun getCommandIcon(): Int

    abstract fun getCommandDescribe(): String

    open fun isGroupCommand(): Boolean = false

    fun getParamEnv(): ScriptParamEnv {
        return getRootScript()?.envParam ?: ScriptParamEnv.getParamEnv()
    }


    /**
     * 读取变量,格式：\${变量组名.变量名}
     */
    fun parseParamText(text: String?): String? {
        text ?: return null
        return getParamEnv().parseParamText(text, 0)
    }

    fun readParam(paramId: String?): String? {
        return getParamEnv().readParam(paramId)
    }

    fun writeParam(paramId: String?, value: String?, persist: Boolean = false) {
        if (!TextUtils.isEmpty(paramId)) {
            getParamEnv().writeParam(paramId!!, value, persist = persist)
        }
    }

    fun getRootScript(): ScriptCommandRoot? {
        if (this is ScriptCommandRoot) {
            return this
        }
        var temp: ScriptCommand? = parentCommand
        while (temp != null) {
            if (temp is ScriptCommandRoot) {
                return temp
            }
            temp = temp.parentCommand
        }
        return null
    }

    open fun getScriptBasePath(): String {
        var filePath = getRootScript()?.getScriptBasePath()
        if (TextUtils.isEmpty(filePath)) {
            filePath = ScriptConst.Save_Script_Temp_Path
        }
        return filePath!!
    }

    override fun getAttachmentFullPaths(): List<String>? = null

    override fun getAttachmentRelativePaths(): List<String>? = null

    override fun setAttachmentFilePaths(paths: List<String>?) {
        attachmentFiles = paths
    }

    override fun getAttachFiles(): List<File>? {
        return getAttachmentFullPaths()?.map { File(it) }
    }


    fun addCommandQueue(cmd: ScriptCommand) {
        cmd.parentCommand = this
        commandQueue.add(cmd)
    }

    fun getString(id: Int) = GlobalApp.getString(id)

    fun getString(id: Int, vararg args: Any?) = GlobalApp.getString(id, *args)

    /**
     * 代码自动缩进
     */
    protected fun getCommandIndentation(): String {
        val sb = StringBuilder()
        var temp: ScriptCommand? = this
        while (temp != null) {
            if (temp.parentCommand != null && temp.parentCommand !is ScriptCommandRoot) {
                sb.append("  ")
            }
            temp = temp.parentCommand
        }
        return sb.toString()
    }

    /**
     * 检测是否需要解锁屏幕
     */
    open fun checkIfNeedUnlock(): Boolean {
        return if (enableAutoUnlock && ScriptSetting.script_setting_auto_unlock
        ) {
            return ScriptEventHelper.get().checkIfNeedUnLockScreen()
        } else {
            false
        }
    }

    /**
     * 开始自动解锁屏幕
     */
    open fun startUnlockScreen() {
        ScriptEventHelper.get().wakeScreen()
        ScriptInterpreter.getUnlockScript()?.doExecute()
        ScriptThreadManager.delay(ScriptConst.Cmd_Default_Unlock_Screen_Delay)
    }

    /**
     * 获取该命令的有效区域，如果执行时，为防止其他视图干扰，如果其它视图和该命令的有效区域有交集，则隐藏其他视图
     * 该区域为归一化坐标，如果为null，则表示不需要隐藏其他视图
     */
    open fun getNormalizedActiveArea(): RectF? = null

    /**
     * 深拷贝
     */
    open fun deepCopy(): ScriptCommand {
        val des = this::class.java.newInstance()
        val cmd = this.getCommandFirstLine()
        des.parentCommand = this.parentCommand
        if (des is ScriptRegularInterface) {
            des.parseCmdLine(cmd)
            des.commandQueue = mutableListOf()
            this.commandQueue.forEach {
                val nCmd = it.deepCopy()
                nCmd.parentCommand = des
                des.commandQueue.add(nCmd)
            }
            des.setAttachmentFilePaths(copyScriptFile(this))
            des.conditionList?.forEach {
                if (it is IScriptFileInterface) {
                    it.setAttachmentFilePaths(copyScriptFile(it))
                }
            }
        }
        return des
    }

    private fun getRunningEnvironment() = getRootScript()?.envRunning

    private fun copyScriptFile(handler: IScriptFileInterface): List<String>? {
        handler.getAttachFiles()?.forEach {
            if (FileUtils.isFileExist(it.path)) {
                ScriptHelper.copyToTempDir(it.path)
            }
        }
        return handler.getAttachmentRelativePaths()
    }


    open fun getPermissionRequest(): List<String>? =
        mutableListOf(ScriptHelper.PERMISSION_BIND_ACCESSIBILITY_SERVICE)

    data class CmdExecuteResult(
        val success: Boolean,
        val message: String? = null,
        val data: Any? = null
    ) : Serializable {
        override fun toString(): String {
            return "CmdExecuteResult(success=$success, message=$message, data=$data)"
        }

        companion object {
            fun success(data: Any? = null, message: String? = null): CmdExecuteResult {
                return CmdExecuteResult(
                    success = true,
                    message = message
                        ?: GlobalApp.getString(com.hive.i8n.R.string.script_command_execute_success),
                    data = data
                )
            }

            fun maySuccess(data: Any? = null, message: String? = null): CmdExecuteResult {
                return CmdExecuteResult(
                    success = true,
                    message = message
                        ?: GlobalApp.getString(com.hive.i8n.R.string.script_command_execute_may_success),
                    data = data
                )
            }

            fun failure(message: String? = null): CmdExecuteResult {
                return CmdExecuteResult(
                    success = false,
                    message = message ?: "Command execution failed"
                )
            }
        }

    }
}