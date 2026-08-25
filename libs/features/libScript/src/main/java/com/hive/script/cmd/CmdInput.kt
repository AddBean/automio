// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.cmd

import android.graphics.Rect
import com.hive.script.R
import com.hive.script.ScriptProvider
import com.hive.script.base.ScriptCommand
import com.hive.script.base.ScriptConst
import com.hive.script.base.core.ScriptInterpreterObserver
import com.hive.script.base.core.ScriptRegularInterface
import com.hive.script.base.core.ScriptThreadManager
import com.hive.script.base.params.ScriptParamEnv
import com.hive.script.driver.ScriptAccessHelper
import com.hive.script.driver.ScriptEventHelper
import com.hive.script.driver.ScriptTextInputHelper
import com.hive.script.inputmethod.ScriptInputMethodHelper
import com.hive.script.base.core.ScriptLineTokenizer
import com.hive.script.utils.ScriptCommandHelper
import com.hive.script.utils.ScriptCoordinateAdapter
import com.hive.script.views.logger.ScriptLoggerView
import com.hive.utils.GlobalApp
import com.hive.utils.extends.decode
import com.hive.utils.extends.encode
import com.hive.utils.utils.StringUtils

/**
 *
 * @author jiadou
 * @email 172111432@qq.com
 * @date 6/8/21
 */
@AutoCmdRegister(type = IDS.CmdInput, name = "input")
class CmdInput : ScriptCommand(), ScriptRegularInterface {
    val resultRect = Rect()

    var targetId: String? = null

    /**
     * 目标输入框序号，1-based。兼容旧脚本时默认取第 1 个。
     */
    var targetIndex = 1

    /**
     * 目标输入框中心点 X 坐标（归一化 0-1）。优先于 targetId/targetIndex。
     */
    var targetX: Float? = null

    /**
     * 目标输入框中心点 Y 坐标（归一化 0-1）。优先于 targetId/targetIndex。
     */
    var targetY: Float? = null

    var content: String? = null

    var action = "full"//full or append

    var animInput = false // 是否逐字输入

    var useInputMethod = false // 是否使用输入法（默认使用，更稳定）

    override fun onExecute(): CmdExecuteResult {
        var isSuccess = false
        val msg = parseParamText(content)
        val finalId = parseParamText(targetId)
        ScriptInterpreterObserver.notifyLogger(
            this, ScriptLoggerView.LogType.DEBUG, getCommandName() + " [$finalId:$msg]"
        )

        // 如果有坐标参数，先点击目标位置再输入
        val hasCoord = targetX != null && targetY != null
        if (hasCoord) {
            msg?.also {
                return executeInputWithCoord(it, finalId ?: "")
            }
            return CmdExecuteResult.failure(message = GlobalApp.getString(com.hive.i8n.R.string.cmd_input_failure))
        }

        // 如果使用输入法，尝试使用输入法输入
        msg?.also {
            if (useInputMethod && ScriptProvider.isScriptInputMethodEnabled()) {

                isSuccess = tryInputWithMethod(msg, finalId ?: "")
                if (isSuccess) {
                    ScriptThreadManager.delay(getCommandDuration())
                    return CmdExecuteResult.maySuccess(
                        message = GlobalApp.getString(com.hive.i8n.R.string.cmd_input_method_success)
                    )
                } else {
                    // 输入法失败，回退到传统方法
                    ScriptInterpreterObserver.notifyLogger(
                        this, ScriptLoggerView.LogType.DEBUG,
                        GlobalApp.getString(com.hive.i8n.R.string.cmd_input_method_fallback)
                    )
                    // 继续执行传统方法（在 if 块外）
                }
            } else {
                // 直接使用传统方法输入
                return executeTraditionalInput(msg, finalId ?: "")
            }
            // 输入法失败时的回退：使用传统方法输入
            return executeTraditionalInput(msg, finalId ?: "")

        }
        return CmdExecuteResult.failure(message = GlobalApp.getString(com.hive.i8n.R.string.cmd_input_failure))
    }

    /**
     * 通过坐标点击后输入
     */
    private fun executeInputWithCoord(msg: String, finalId: String): CmdExecuteResult {
        val x = targetX ?: return CmdExecuteResult.failure("targetX is null")
        val y = targetY ?: return CmdExecuteResult.failure("targetY is null")

        // 转换为屏幕坐标
        val screenX = ScriptCoordinateAdapter.get().toRealX(x)
        val screenY = ScriptCoordinateAdapter.get().toRealY(y)

        ScriptInterpreterObserver.notifyLogger(
            this, ScriptLoggerView.LogType.DEBUG,
            "Input with coord: ($x, $y) -> screen ($screenX, $screenY)"
        )

        // 先点击目标位置
        ScriptEventHelper.get().performClick(screenX, screenY)

        // 等待焦点稳定
        ScriptThreadManager.delay(500)

        // 查找当前焦点的可编辑节点
        val focusedNode = ScriptEventHelper.get().serviceEntity?.rootInActiveWindow?.findFocus(android.view.accessibility.AccessibilityNodeInfo.FOCUS_INPUT)
        if (focusedNode == null || !focusedNode.isEditable() || !focusedNode.isFocusable()) {
            return CmdExecuteResult.failure(
                GlobalApp.getString(com.hive.i8n.R.string.cmd_input_target_index_not_found, 0)
            )
        }

        // 执行输入
        val isSuccess = when (action) {
            "append" -> ScriptAccessHelper.appendEditText(focusedNode, animInput, msg)
            else -> ScriptAccessHelper.setEditText(focusedNode, animInput, msg)
        }

        ScriptThreadManager.delay(getCommandDuration())
        return if (isSuccess) {
            CmdExecuteResult.maySuccess(message = GlobalApp.getString(com.hive.i8n.R.string.cmd_input_traditional_success))
        } else {
            CmdExecuteResult.failure(GlobalApp.getString(com.hive.i8n.R.string.cmd_input_traditional_failure))
        }
    }

    /**
     * 使用传统方法进行输入
     */
    private fun executeTraditionalInput(msg: String, finalId: String): CmdExecuteResult {
        val targetNode = findTargetEditNode(finalId)
            ?: return CmdExecuteResult.failure(
                GlobalApp.getString(
                    com.hive.i8n.R.string.cmd_input_target_index_not_found,
                    targetIndex,
                )
            )

        var isSuccess = false
        ScriptAccessHelper.requestFocus(targetNode)
        ScriptThreadManager.delay(1000)

        val refreshedTargetNode = findTargetEditNode(finalId)?.let {
            ScriptAccessHelper.requestFocus(it)
            ScriptTextInputHelper.getRefreshedNodeInfo(it) ?: it
        }

        if (refreshedTargetNode == null) {
            return CmdExecuteResult.failure(
                GlobalApp.getString(
                    com.hive.i8n.R.string.cmd_input_target_index_not_found,
                    targetIndex,
                )
            )
        }

        refreshedTargetNode.getBoundsInScreen(resultRect)
        ScriptInterpreterObserver.notifyCommandExecuteEvent(0, this)
        // 请求获取 node 的焦点
        isSuccess = when (action) {
            "append" -> {
                ScriptAccessHelper.appendEditText(refreshedTargetNode, animInput, msg)
            }

            else -> {
                ScriptAccessHelper.setEditText(refreshedTargetNode, animInput, msg)
            }
        }

        ScriptThreadManager.delay(getCommandDuration())
        return if (isSuccess) {
            CmdExecuteResult.maySuccess(message = GlobalApp.getString(com.hive.i8n.R.string.cmd_input_traditional_success))
        } else {
            CmdExecuteResult.failure(GlobalApp.getString(com.hive.i8n.R.string.cmd_input_traditional_failure))
        }
    }

    /**
     * 使用输入法进行输入
     */
    private fun tryInputWithMethod(msg: String, finalId: String): Boolean {
        return try {
            val context = ScriptEventHelper.get().getAccessService()
                ?: GlobalApp.getContext()

            // 检查输入法是否已启用
            if (!ScriptInputMethodHelper.isInputMethodEnabled(context)) {
                ScriptInterpreterObserver.notifyLogger(
                    this, ScriptLoggerView.LogType.WARN,
                    GlobalApp.getString(com.hive.i8n.R.string.cmd_input_method_not_enabled)
                )
                return false
            }

            // 如果 finalId == "-"，说明没有指定 id，直接使用当前焦点输入框
            val hasTargetId = finalId != ScriptConst.NONE_CHAR

            if (hasTargetId) {
                // 找到目标输入框并获取焦点
                val targetNode = findTargetEditNode(finalId) ?: return false.also {
                    ScriptInterpreterObserver.notifyLogger(
                        this, ScriptLoggerView.LogType.WARN,
                        GlobalApp.getString(
                            com.hive.i8n.R.string.cmd_input_target_index_not_found,
                            targetIndex,
                        )
                    )
                }
                ScriptAccessHelper.requestFocus(targetNode)
                ScriptThreadManager.delay(500)
            } else {
                ScriptThreadManager.delay(500)
                // 没有指定 id，尝试查找当前焦点输入框或所有可编辑输入框
                val targetNode = findTargetEditNode(finalId) ?: return false.also {
                    ScriptInterpreterObserver.notifyLogger(
                        this, ScriptLoggerView.LogType.WARN,
                        GlobalApp.getString(
                            com.hive.i8n.R.string.cmd_input_target_index_not_found,
                            targetIndex,
                        )
                    )
                }
                ScriptAccessHelper.requestFocus(targetNode)
                ScriptThreadManager.delay(500)
            }

            // 切换到自定义输入法
            val switched = ScriptInputMethodHelper.switchToInputMethod(context)
            if (!switched) {
                ScriptInterpreterObserver.notifyLogger(
                    this, ScriptLoggerView.LogType.WARN,
                    GlobalApp.getString(com.hive.i8n.R.string.cmd_input_method_switch_failed)
                )
                return false
            }

            ScriptThreadManager.delay(800)

            // 设置待输入文本
            val clearFirst = action != "append"
            ScriptInputMethodHelper.setPendingText(msg, clearFirst, animInput)

            // 再次获取焦点，触发输入法启动
            val focusedTargetNode = findTargetEditNode(finalId) ?: return false.also {
                ScriptInterpreterObserver.notifyLogger(
                    this, ScriptLoggerView.LogType.WARN,
                    GlobalApp.getString(
                        com.hive.i8n.R.string.cmd_input_target_index_not_found,
                        targetIndex,
                    )
                )
            }
            ScriptAccessHelper.requestFocus(focusedTargetNode)

            ScriptThreadManager.delay(1000)

            // 检查输入法是否已激活并输入文本
            if (ScriptInputMethodHelper.isInputMethodActive()) {
                val result = if (action == "append") {
                    ScriptInputMethodHelper.appendText(msg)
                } else {
                    ScriptInputMethodHelper.inputText(msg, clearFirst, animInput)
                }

                if (result) {
                    ScriptInterpreterObserver.notifyLogger(
                        this, ScriptLoggerView.LogType.DEBUG,
                        GlobalApp.getString(com.hive.i8n.R.string.cmd_input_method_success_text)
                    )
                    return true
                }
            }

            false
        } catch (e: Exception) {
            ScriptInterpreterObserver.notifyLogger(
                this, ScriptLoggerView.LogType.ERROR,
                GlobalApp.getString(com.hive.i8n.R.string.cmd_input_method_error, e.message ?: "")
            )
            false
        }
    }

    private fun getCommandDuration() = ScriptConst.Cmd_Default_Bias

    private fun findTargetEditNode(finalId: String): android.view.accessibility.AccessibilityNodeInfo? {
        val targetNodes = ScriptEventHelper.get().performFindEditText(finalId, limitRect)
        if (targetIndex <= 0) {
            // 按当前焦点查找
            return targetNodes?.firstOrNull { it.isFocused }
        }
        val index = targetIndex.coerceAtLeast(1) - 1
        return targetNodes?.getOrNull(index)
    }

    override fun getCommandName() = GlobalApp.getString(com.hive.i8n.R.string.cmd_name_input)

    override fun getCommandDescribe() =
        if (ScriptParamEnv.isParamText(content)) GlobalApp.getString(com.hive.i8n.R.string.cmd_name_input) else GlobalApp.getString(
            com.hive.i8n.R.string.cmd_name_input_des, StringUtils.decoding(content)
        )

    override fun getCommandIcon() = R.drawable.ic_input

    override fun getCommand() =
        buildString {
            append("${cmdPrefix()} content=\"${content?.encode()}\" target=\"${targetId?.encode()}\"")
            if (targetIndex > 1) {
                append(" targetIndex=$targetIndex")
            }
            if (targetX != null && targetY != null) {
                append(" x=$targetX y=$targetY")
            }
            append(" action=$action anim=$animInput")
        }

    override fun isSupportRect() = true

    override fun parseCmd(cmd: String) {
        val p = ScriptLineTokenizer.parseKeyValueParams(cmd)
        content = p["content"]?.decode() ?: content
        targetId = p["target"]?.decode() ?: targetId
        targetIndex = p["targetIndex"]?.toIntOrNull() ?: 1
        targetX = p["x"]?.toFloatOrNull()
        targetY = p["y"]?.toFloatOrNull()
        action = p["action"] ?: "full"
        animInput = p["anim"]?.toBooleanStrictOrNull() ?: false
    }

    companion object {
        fun createCommand(
            content: String,
            targetId: String?,
            action: String?,
            animInput: Boolean? = null,
            targetIndex: Int? = null,
            targetX: Float? = null,
            targetY: Float? = null,
        ) = CmdInput().apply {
            this.content = content.encode()
            this.targetId = targetId ?: ScriptConst.NONE_CHAR
            this.action = action ?: "full"
            this.animInput = animInput ?: false
            this.targetIndex = targetIndex?.takeIf { it > 0 || it == -1 } ?: 1
            this.targetX = targetX
            this.targetY = targetY
        }
    }
}
