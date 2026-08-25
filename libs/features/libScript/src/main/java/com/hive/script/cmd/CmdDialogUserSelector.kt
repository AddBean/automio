// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.cmd

import com.hive.script.R
import com.hive.script.ScriptProvider
import com.hive.script.base.ScriptCommand
import com.hive.script.base.ScriptConst
import com.hive.script.base.core.ScriptRegularInterface
import com.hive.script.base.core.ScriptThreadManager
import com.hive.script.views.dialog.DialogCmdDialogSelector
import com.hive.script.base.core.ScriptLineTokenizer
import com.hive.utils.GlobalApp
import com.hive.utils.thread.UIHandlerUtils
import java.util.concurrent.atomic.AtomicBoolean

/**
 *
 * @author jiadou
 * @email 172111432@qq.com
 * @date 6/8/21
 */
@AutoCmdRegister(type = IDS.CmdDialogUserSelector, name = "dialogUserSelector")
class CmdDialogUserSelector : ScriptCommand(), ScriptRegularInterface {
    private val context = GlobalApp.getContext()

    var dialogTitle: String? = null

    var dialogItems: String? = null

    var dialogMultiSelect: Boolean = false

    var dialogCountDown: Int = -1

    var dialogShowing: AtomicBoolean = AtomicBoolean(false)

    override fun onExecute(): CmdExecuteResult {
        var selectedPositions = listOf<Int>()
        var selectedItems = listOf<String>()
        dialogShowing.set(true)
        UIHandlerUtils.getInstance().post {
            showSelectorDialog { positions, items ->
                selectedPositions = positions
                selectedItems = items
                dialogShowing.set(false)
            }
        }
        while (dialogShowing.get()) {
            ScriptThreadManager.delay(1000)
        }

        return if (selectedPositions.isNotEmpty()) {
            val itemsText = selectedItems.joinToString("|")
            CmdExecuteResult.success(
                "selected:${itemsText}"
            )
        } else {
            CmdExecuteResult.success(
                "canceled",
                context.getString(com.hive.i8n.R.string.script_command_execute_may_success)
            )
        }
    }

    private fun getCommandDuration() = ScriptConst.Cmd_Default_Bias

    override fun getCommandName() = getString(com.hive.i8n.R.string.cmd_dialog_selector_name)

    override fun getCommandDescribe() = getString(com.hive.i8n.R.string.cmd_dialog_selector_des)

    override fun getCommandIcon() = R.drawable.sc_ic_dialogue

    override fun getCommand(): String {
        fun q(s: String?) = when {
            s.isNullOrBlank() -> ""
            s.contains(" ") || s.contains("\"") || s.contains("|") -> "\"${
                (s ?: "").replace(
                    "\"",
                    "\\\""
                )
            }\""

            else -> s ?: ""
        }
        return "${cmdPrefix()} title=${q(dialogTitle)} items=${q(dialogItems)} multiSelect=$dialogMultiSelect countDown=$dialogCountDown"
    }

    override fun parseCmd(cmd: String) {
        val p = ScriptLineTokenizer.parseKeyValueParams(cmd)
        dialogTitle = p["title"]?.takeIf { it.isNotBlank() }
        dialogItems = p["items"]?.takeIf { it.isNotBlank() }
        dialogMultiSelect = p["multiSelect"]?.toBooleanStrictOrNull() ?: false
        dialogCountDown = p["countDown"]?.toIntOrNull() ?: -1
    }

    override fun getPermissionRequest() = null

    private fun showSelectorDialog(onSelect: (positions: List<Int>, items: List<String>) -> Unit) {
        val items = dialogItems?.split("|")?.mapIndexed { index, item ->
            Pair(index, item)
        }?.toMutableList() ?: mutableListOf()

        val alert = DialogCmdDialogSelector(ScriptProvider.getViewContext())
        alert.setTitle(
            dialogTitle ?: context.getString(com.hive.i8n.R.string.cmd_dialog_selector_name)
        )
        alert.setMultiSelectMode(dialogMultiSelect)
        alert.setDataSet(items)
        alert.setCountDown(dialogCountDown)
        alert.setSelectListener(object : DialogCmdDialogSelector.OnSelectListener {
            override fun onSelected(
                dialog: DialogCmdDialogSelector,
                pos: Int,
                pair: Pair<Int, String>
            ) {
                // 单选模式，保持兼容性
                onSelect.invoke(listOf(pos), listOf(pair.second))
            }

            override fun onMultiSelected(
                dialog: DialogCmdDialogSelector,
                selectedPositions: List<Int>,
                selectedItems: List<Pair<Int, String>>
            ) {
                val selectedItemTexts = selectedItems.map { it.second }
                onSelect.invoke(selectedPositions, selectedItemTexts)
            }

            override fun onCancel() {
                onSelect.invoke(emptyList(), emptyList())
            }
        })
        alert.show()
    }

    companion object {
        fun createCommand(
            title: String?,
            items: String?,
            multiSelect: Boolean = false,
            countDown: Int? = null
        ) =
            CmdDialogUserSelector().apply {
                this.dialogTitle =
                    title ?: context.getString(com.hive.i8n.R.string.cmd_dialog_selector_name)
                this.dialogItems = items
                this.dialogMultiSelect = multiSelect
                this.dialogCountDown = countDown ?: -1
            }
    }
} 