// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.cmd

import android.text.InputType
import com.hive.script.R
import com.hive.script.ScriptProvider
import com.hive.script.base.ScriptCommand
import com.hive.script.base.ScriptConst
import com.hive.script.base.core.ScriptRegularInterface
import com.hive.script.base.core.ScriptThreadManager
import com.hive.script.views.dialog.DialogCmdDialogInput
import com.hive.script.views.dialog.DialogCmdDialogInput.InputItem
import com.hive.script.views.dialog.DialogCmdDialogInput.InputItemTypes
import com.hive.script.base.core.ScriptLineTokenizer
import com.hive.utils.GlobalApp
import com.hive.utils.extends.decode
import com.hive.utils.extends.encode
import com.hive.utils.thread.UIHandlerUtils
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 多输入项对话框命令
 *
 * @author jiadou
 * @email 172111432@qq.com
 * @date 2024/12/19
 */
@AutoCmdRegister(type = IDS.CmdDialogUserInput, name = "dialogUserInput")
class CmdDialogUserInput : ScriptCommand(), ScriptRegularInterface {
    private val context = GlobalApp.getContext()

    var dialogTitle: String? = null

    var dialogInputs: String? = null
    var dialogHints: String? = null
    var dialogRequires: String? = null
    var dialogDefaults: String? = null

    var dialogCountDown: Int = -1

    var dialogShowing: AtomicBoolean = AtomicBoolean(false)

    override fun onExecute(): CmdExecuteResult {
        var inputValues: List<InputItem>? = null
        dialogShowing.set(true)
        UIHandlerUtils.getInstance().post {
            showInputDialog { values ->
                inputValues = values
                dialogShowing.set(false)
            }
        }
        while (dialogShowing.get()) {
            ScriptThreadManager.delay(1000)
        }

        return if (inputValues?.isNotEmpty()==true) {
            val mapValues = inputValues?.map { it ->
                "${it.label}: ${it.value}"
            }?.joinToString(",")
            CmdExecuteResult.success(
                "user inputs result:${mapValues}",
            )
        } else {
            CmdExecuteResult.success("canceled", context.getString(com.hive.i8n.R.string.script_command_execute_may_success))
        }
    }

    private fun getCommandDuration() = ScriptConst.Cmd_Default_Bias

    override fun getCommandName() = getString(com.hive.i8n.R.string.cmd_dialog_input_name)

    override fun getCommandDescribe() = getString(com.hive.i8n.R.string.cmd_dialog_input_des)

    override fun getCommandIcon() = R.drawable.sc_ic_dialogue

    override fun getCommand(): String {
        fun q(s: String?) = when {
            s.isNullOrBlank() -> ""
            else -> s.encode()
        }
        return "${cmdPrefix()} title=${q(dialogTitle)} inputs=${q(dialogInputs)} hints=${q(dialogHints)} requires=${q(dialogRequires)} defaults=${q(dialogDefaults)} countDown=$dialogCountDown"
    }

    override fun parseCmd(cmd: String) {
        val p = ScriptLineTokenizer.parseKeyValueParams(cmd)
        dialogTitle = p["title"]?.decode()?.takeIf { it.isNotBlank() }
        dialogInputs = p["inputs"]?.decode()?.takeIf { it.isNotBlank() }
        dialogHints = p["hints"]?.decode()?.takeIf { it.isNotBlank() }
        dialogRequires = p["requires"]?.decode()?.takeIf { it.isNotBlank() }
        dialogDefaults = p["defaults"]?.decode()?.takeIf { it.isNotBlank() }
        dialogCountDown = p["countDown"]?.toIntOrNull() ?: -1
    }

    override fun getPermissionRequest() = null

    private fun showInputDialog(onInput: (values: List<InputItem>) -> Unit) {
        val inputItems = parseInputItems()

        val dialog = DialogCmdDialogInput(ScriptProvider.getViewContext())
        dialog.setTitle(dialogTitle ?: context.getString(com.hive.i8n.R.string.ui_dialog_input_hint))
        dialog.setInputItems(inputItems)
        dialog.setCountDown(dialogCountDown)
        dialog.setInputListener(object : DialogCmdDialogInput.OnInputListener {
            override fun onConfirmed(dialog: DialogCmdDialogInput, inputs: List<InputItem>) {
                onInput.invoke(inputs)
            }

            override fun onCancel() {
                onInput.invoke(mutableListOf())
            }
        })
        dialog.show()
    }

    /**
     * 解析输入项配置
     * 新的格式：分别解析 inputs、hints、requires、defaults
     * inputs: 输入项标签列表，用|分隔
     * hints: 提示文本列表，用|分隔
     * requires: 是否必填列表，用|分隔
     * defaults: 默认值列表，用|分隔
     */
    private fun parseInputItems(): List<InputItem> {
        val inputs = dialogInputs?.split("|") ?: emptyList()
        val hints = dialogHints?.split("|") ?: emptyList()
        val requires = dialogRequires?.split("|") ?: emptyList()
        val defaults = dialogDefaults?.split("|") ?: emptyList()

        if (inputs.isEmpty()) {
            return emptyList()
        }

        return inputs.mapIndexed { index, label ->
            val hint = if (index < hints.size) hints[index] else context.getString(com.hive.i8n.R.string.ui_dialog_input_hint)
            val required = if (index < requires.size) requires[index].toBoolean() else false
            val defaultValue = if (index < defaults.size) defaults[index] else ""
            InputItem(
                label = label,
                hint = hint,
                required = required,
                inputType = DialogCmdDialogInput.TYPE_TEXT,
                defaultValue = defaultValue
            )
        }
    }

    companion object {
        fun createCommand(
            title: String?,
            inputs: String?,
            hints: String? = null,
            requires: String? = null,
            defaults: String? = null,
            countDown: Int? = null
        ) =
            CmdDialogUserInput().apply {
                this.dialogTitle = title
                this.dialogInputs = inputs
                this.dialogHints = hints
                this.dialogRequires = requires
                this.dialogDefaults = defaults
                this.dialogCountDown = countDown ?: -1
            }
    }
} 