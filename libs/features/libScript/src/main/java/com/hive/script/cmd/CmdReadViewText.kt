// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.cmd

import android.graphics.RectF
import android.os.Build
import android.view.accessibility.AccessibilityNodeInfo
import com.hive.script.R
import com.hive.script.base.ScriptCommand
import com.hive.script.base.ScriptConst
import com.hive.script.base.core.ScriptInterpreterObserver
import com.hive.script.base.core.ScriptRegularInterface
import com.hive.script.base.core.ScriptThreadManager
import com.hive.script.base.params.ScriptParamEnv
import com.hive.script.driver.ScriptEventHelper
import com.hive.script.base.core.ScriptLineTokenizer
import com.hive.script.views.logger.ScriptLoggerView
import com.hive.utils.GlobalApp
import com.hive.utils.extends.string

/**
 *
 * @author jiadou
 * @email 172111432@qq.com
 * @date 6/8/21
 */
@AutoCmdRegister(type = IDS.CmdReadViewText, name = "readViewText")
class CmdReadViewText : ScriptCommand(), ScriptRegularInterface {
    var readType: ReadType = ReadType.TEXT

    var readScope: ReadScope = ReadScope.SINGLE

    var targetId = ScriptConst.NONE_CHAR

    var targetParamId = ScriptParamEnv.getDefaultParam()?.getFullId()

    var findDirection = 0//0 优先左上角、1 优先右上角、2 优先左下角、3 优先右下角

    var targetNodeList = mutableListOf<AccessibilityNodeInfo?>()

    var resultText: String? = null

    override fun onExecute(): CmdExecuteResult {
        try {
            val finalId = parseParamText(targetId)
            ScriptEventHelper.get().performFindAllLayout(
                finalId,
                null,
                null,
                0,
                findDirection,
                limitRect
            ) { list ->
                targetNodeList.clear()

                when (readScope) {
                    ReadScope.SINGLE -> {
                        val targetNode = list.firstOrNull()
                        targetNodeList.add(targetNode?.first)
                        resultText = getNodeInfo(targetNode?.first)
                        ScriptInterpreterObserver.notifyCommandExecuteEvent(0, this)
                        ScriptInterpreterObserver.notifyLogger(
                            this,
                            ScriptLoggerView.LogType.DEBUG,
                            com.hive.i8n.R.string.sc_read_text.string(resultText ?: "")
                        )
                        writeParam(targetParamId, resultText)
                    }

                    ReadScope.ALL -> {
                        targetNodeList.addAll(list.map { it.first })
                        resultText = targetNodeList.joinToString("\n") { getNodeInfo(it) }
                        ScriptInterpreterObserver.notifyCommandExecuteEvent(0, this)
                        ScriptInterpreterObserver.notifyLogger(
                            this,
                            ScriptLoggerView.LogType.DEBUG,
                            com.hive.i8n.R.string.sc_read_text.string(resultText ?: "")
                        )
                        writeParam(targetParamId, resultText)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        ScriptThreadManager.delay(ScriptConst.Cmd_Default_Base)
        return if (resultText.isNullOrEmpty()){
            CmdExecuteResult.failure()
        }else{
            CmdExecuteResult.success(resultText)
        }
    }

    private fun getNodeInfo(targetNode: AccessibilityNodeInfo?): String {
        val result = when (readType) {
            ReadType.TEXT -> maskNullToEmpty(targetNode?.text?.toString()) ?: ""
            ReadType.DESC -> maskNullToEmpty(targetNode?.contentDescription?.toString()) ?: ""
            ReadType.HINT -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                maskNullToEmpty(targetNode?.hintText?.toString()) ?: ""
            } else {
                ""
            }

            ReadType.ALL -> {
                val text = maskNullToEmpty(targetNode?.text?.toString())
                val desc = maskNullToEmpty(targetNode?.contentDescription?.toString())
                val hint = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    maskNullToEmpty(targetNode?.hintText?.toString()) ?: ""
                } else {
                    ""
                }
                "$text\n$desc\n$hint"
            }
        }
        return result
    }

    private fun maskNullToEmpty(str: String?): String? {
        return if (str == "null") {
            ""
        } else {
            str
        }
    }


    override fun getCommand() =
        "${cmdPrefix()} type=${readType.value} output=$targetParamId target=$targetId direction=$findDirection scope=${readScope.value}"

    override fun getCommandName() = GlobalApp.getString(com.hive.i8n.R.string.cmd_name_read_view_text)

    override fun getCommandDescribe() = GlobalApp.getString(
        com.hive.i8n.R.string.cmd_name_read_view_text_des,
        ScriptParamEnv.getParam(targetParamId)?.name
    )

    override fun getCommandIcon() = R.drawable.sc_icon_view_text

    override fun isSupportRect(): Boolean = true

    override fun parseCmd(cmd: String) {
        val p = ScriptLineTokenizer.parseKeyValueParams(cmd)
        readType = ReadType.of(p["type"] ?: ReadType.TEXT.value)
        targetParamId = p["output"] ?: ScriptParamEnv.getDefaultParam()?.getFullId()
        targetId = p["target"] ?: ScriptConst.NONE_CHAR
        findDirection = p["direction"]?.toIntOrNull() ?: 0
        readScope = ReadScope.of(p["scope"] ?: ReadScope.SINGLE.value)
    }

    override fun getNormalizedActiveArea() = RectF(0f, 0f, 1f, 1f)

    enum class ReadType(var value: String) {
        TEXT("TEXT"),
        DESC("DESC"),
        HINT("HINT"),
        ALL("ALL");

        companion object {
            fun of(value: String): ReadType {
                return values().firstOrNull { it.value == value } ?: TEXT
            }
        }
    }

    enum class ReadScope(var value: String) {
        SINGLE("SINGLE"),
        ALL("ALL");

        companion object {
            fun of(value: String): ReadScope {
                return values().firstOrNull { it.value == value } ?: SINGLE
            }
        }
    }

    companion object {
        fun createCommand(viewId: String?) = CmdReadViewText().apply {
            targetId = viewId ?: ScriptConst.NONE_CHAR
        }
    }
}