// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.base.core

import android.graphics.Color
import android.graphics.RectF
import com.hive.script.base.ScriptCommand
import com.hive.script.base.ScriptCommandCondition
import com.hive.script.base.ScriptConst
import com.hive.script.base.ScriptMate
import com.hive.script.base.params.ScriptParamEnv
import com.hive.script.cmd.CmdEnd
import com.hive.script.cmd.CmdIf
import com.hive.script.cmd.CommandCategoryRegistry
import com.hive.script.cmd.Cmd_Register_Set
import com.hive.script.condition.Condition_Register_Set
import com.hive.script.exception.ScriptException
import com.hive.script.extensions.getType
import com.hive.script.setting.ScriptSetting
import com.hive.script.views.beans.PointVectorFloat
import com.hive.utils.GlobalApp
import com.hive.utils.debug.DLog


/**
 *
 * @author jiadou
 * @email 172111432@qq.com
 * @date 6/8/21
 */
class ScriptParser {
    companion object {
        private val typeMap = mutableMapOf<Int, Class<out Any>>()

        val commandMap = mutableSetOf<ScriptRegularInterface>()

        val conditionMap = mutableSetOf<ScriptCommandCondition>()

        fun initCommandMap() {
            commandMap.clear()
            val allCommands = mutableListOf<ScriptRegularInterface>().apply {
                add(ScriptMate())
                Cmd_Register_Set.forEach {
                    add(it.newInstance() as ScriptRegularInterface)
                }
            }
            // 按 cmdPrefix 长度升序、同长度按字典序，确保 findLast 命中最具体命令（如 clickImage 优于 click）
            allCommands.sortWith(compareBy({ it.cmdPrefix().length }, { it.cmdPrefix() }))
            allCommands.forEach { commandMap.add(it) }
            commandMap.forEach {
                if (it is ScriptCommand) {
                    typeMap[it.getType()] = it.javaClass
                }
            }
        }

        private fun getColorForCmd(cmd: ScriptCommand): Int {
            val category = CommandCategoryRegistry.getCategoryByCmdId(cmd.getType())
                ?: return ScriptConst.colorCmdArray[0]
            val palette = ScriptConst.colorByCategory[category]
                ?: return ScriptConst.colorCmdArray[0]
            val idx = kotlin.math.abs(cmd::class.java.name.hashCode()) % palette.size
            return palette[idx]
        }

        fun initConditionMap() {
            conditionMap.clear()
            Condition_Register_Set.forEach {
                conditionMap.add(
                    it.getConstructor(ScriptCommand::class.java).newInstance(
                        CmdIf.createCommand(
                            mutableListOf(), mutableListOf()
                        )
                    ) as ScriptCommandCondition
                )
            }
        }

        fun getColor(cmd: ScriptCommand?): Int {
            cmd ?: return Color.WHITE
            return getColorForCmd(cmd)
        }

        fun getXCellColor(cmd: ScriptCommand?): Int {
            cmd ?: return Color.WHITE
            return getColorForCmd(cmd)
        }

        fun getJumpCellColor(id: Int): Int {
            return ScriptConst.colorJumpArray[id % ScriptConst.colorCmdArray.size]
        }

        /**
         * 解析一行命令。采用 ScriptLineTokenizer 词法分析，替代原正则实现。
         * 详见文档 13.5 节、ScriptLineTokenizer
         */
        fun parserCmdLine(cmdLine: String?): Map<String, String?> {
            return ScriptLineTokenizer.parseLine(cmdLine)
        }
    }

    /**
     * 解析脚本信息
     */
    fun parserMate(reader: IScriptReader): ScriptMate {
        val mate = ScriptMate()
        reader.reset()
        var line = reader.readLine()
        while (line != null && mate.matchCmd(line)) {
            mate.parseCmd(line)
            line = reader.readLine()
        }
        reader.backLine()
        return mate
    }

    /**
     * 按行解析指令
     */
    fun parserCmd(reader: IScriptReader, parentCommand: ScriptCommand) {
        var cmdMap = parserCmdLine(reader.readLine())//去除缩进等修饰字符
        var cmdComment = cmdMap["comment"]
        var startDelay = cmdMap["delayStart"]?.toLongOrNull() ?: ScriptConst.Cmd_Default_Delay
        var endDelay = cmdMap["delayEnd"]?.toLongOrNull() ?: ScriptConst.Cmd_Default_Delay

        var limitRect = RectF()
        limitRect.left = cmdMap["rectLeft"]?.toFloat() ?: 0f
        limitRect.top = cmdMap["rectTop"]?.toFloat() ?: 0f
        limitRect.right = cmdMap["rectRight"]?.toFloat() ?: 1f
        limitRect.bottom = cmdMap["rectBottom"]?.toFloat() ?: 1f

        var offsetVector = PointVectorFloat()
        offsetVector.fromX = cmdMap["offsetFromX"]?.toFloatOrNull() ?: 0f
        offsetVector.fromY = cmdMap["offsetFromY"]?.toFloatOrNull() ?: 0f
        offsetVector.toX = cmdMap["offsetToX"]?.toFloatOrNull() ?: 0f
        offsetVector.toY = cmdMap["offsetToY"]?.toFloatOrNull() ?: 0f

        var dragType = cmdMap["dragType"]?.toIntOrNull() ?: 0
        var dragDuration = cmdMap["dragDuration"]?.toLongOrNull() ?: ScriptConst.Cmd_Drag_DURATION
        var dragPressDuration =
            cmdMap["dragPressDuration"]?.toLongOrNull() ?: ScriptConst.Cmd_Long_Click_Default

        var dragVector = PointVectorFloat()
        dragVector.fromX = cmdMap["dragFromX"]?.toFloatOrNull() ?: 0f
        dragVector.fromY = cmdMap["dragFromY"]?.toFloatOrNull() ?: 0f
        dragVector.toX = cmdMap["dragToX"]?.toFloatOrNull() ?: 0f
        dragVector.toY = cmdMap["dragToY"]?.toFloatOrNull() ?: 0f


        var cmdLine = cmdMap["cmdLine"]
        cmdLine ?: return
        try {
            cmdLine.run {
                var command: ScriptRegularInterface? =
                    commandMap.findLast { it.matchCmd(this) }?.let { it::class.java.newInstance() }
                        ?: kotlin.run {
                            //可能是个空脚本
                            if (cmdLine.equals("mate")) {
                                return
                            } else throw ScriptException(
                                ScriptException.ExceptionType.ERROR_CODE,
                                GlobalApp.getString(
                                    com.hive.i8n.R.string.script_parse_error_line,
                                    reader.getCurrentLine(),
                                    cmdLine
                                )
                            )
                        }

                while (command?.matchCmd(cmdLine!!) == true) {
                    if (command is ScriptCommand?) {
                        command.parentCommand = parentCommand
                    }
                    if (command is CmdEnd) {
                        return@parserCmd
                    }
                    command.parseCmd(cmdLine!!)
                    if (command is ScriptCommand) {
                        if (cmdComment != null) command.comment = cmdComment
                        command.startDelay = startDelay
                        command.endDelay = endDelay
                        command.limitRect = limitRect
                        command.offsetVector = offsetVector
                        command.dragVector = dragVector
                        command.dragType = dragType
                        command.dragDuration = dragDuration
                        command.dragPressDuration = dragPressDuration
                        when {
                            command.isGroupCommand() -> {
                                parentCommand.addCommandQueue(command)
                                parserCmd(reader, command)
                            }

                            else -> {
                                parentCommand.addCommandQueue(command)
                            }
                        }
                    }
                    cmdMap = parserCmdLine(reader.readLine())//去除缩进等修饰字符
                    cmdComment = cmdMap["comment"]
                    startDelay =
                        cmdMap["delayStart"]?.toLongOrNull() ?: ScriptConst.Cmd_Default_Delay
                    endDelay = cmdMap["delayEnd"]?.toLongOrNull() ?: ScriptConst.Cmd_Default_Delay
                    limitRect = RectF()
                    limitRect.left = cmdMap["rectLeft"]?.toFloat() ?: 0f
                    limitRect.top = cmdMap["rectTop"]?.toFloat() ?: 0f
                    limitRect.right = cmdMap["rectRight"]?.toFloat() ?: 1f
                    limitRect.bottom = cmdMap["rectBottom"]?.toFloat() ?: 1f


                    offsetVector = PointVectorFloat()
                    offsetVector.fromX = cmdMap["offsetFromX"]?.toFloatOrNull() ?: 0f
                    offsetVector.fromY = cmdMap["offsetFromY"]?.toFloatOrNull() ?: 0f
                    offsetVector.toX = cmdMap["offsetToX"]?.toFloatOrNull() ?: 0f
                    offsetVector.toY = cmdMap["offsetToY"]?.toFloatOrNull() ?: 0f

                    dragType = cmdMap["dragType"]?.toIntOrNull() ?: 0
                    dragDuration =
                        cmdMap["dragDuration"]?.toLongOrNull() ?: ScriptConst.Cmd_Drag_DURATION
                    dragPressDuration =
                        cmdMap["dragPressDuration"]?.toLongOrNull()
                            ?: ScriptConst.Cmd_Long_Click_Default

                    dragVector = PointVectorFloat()
                    dragVector.fromX = cmdMap["dragFromX"]?.toFloatOrNull() ?: 0f
                    dragVector.fromY = cmdMap["dragFromY"]?.toFloatOrNull() ?: 0f
                    dragVector.toX = cmdMap["dragToX"]?.toFloatOrNull() ?: 0f
                    dragVector.toY = cmdMap["dragToY"]?.toFloatOrNull() ?: 0f

                    cmdLine = cmdMap["cmdLine"]
                    if (cmdLine == null) return@parserCmd
                    cmdLine?.run {
                        command = commandMap.findLast { it.matchCmd(cmdLine!!) }
                            ?.let { it::class.java.newInstance() } ?: throw ScriptException(
                            ScriptException.ExceptionType.ERROR_CODE,
                            GlobalApp.getString(
                                com.hive.i8n.R.string.script_parse_error_line,
                                reader.getCurrentLine(),
                                cmdLine
                            )
                        )
                    }
                }
            }
            //如果遇到解析失败，则忽略后继续执行
        } catch (e: ScriptException) {
            DLog.e("ScriptParser", "脚本解析失败: ${e.message}", e)
            if (isIgnoreCmd(cmdLine)) {
                parserCmd(reader, parentCommand)
            } else {
                throw e
            }

        }
    }


    /**
     * 解析参数
     */
    fun parserParams(reader: IScriptReader, paramEnv: ScriptParamEnv) {
        reader.reset()
        reader.readLine()//从第二行开始
        var line = reader.readLine()
        while (line != null) {
            if (paramEnv.matchCmd(line)) {
                paramEnv.parseParam(line)
                line = reader.readLine()
            } else {
                line = null
            }
        }
        reader.backLine()
    }

    private fun isIgnoreCmd(cmdLine: String?): Boolean {
        cmdLine ?: return true
        return ScriptSetting.script_setting_editor_ignore_parse_error || cmdLine.startsWith("mate") || cmdLine.startsWith(
            "null"
        ) || cmdLine.startsWith("def")
    }


}