// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.edit

import android.content.Context
import android.text.TextUtils
import android.view.View
import com.hive.script.R
import com.hive.script.base.ScriptCommand
import com.hive.script.cmd.CmdFor
import com.hive.script.cmd.CmdScriptEnd
import com.hive.script.cmd.CmdScriptStart
import com.hive.script.extensions.isUnReachable
import com.hive.script.extensions.moveDown
import com.hive.script.extensions.moveUp
import com.hive.script.extensions.replaceTo
import com.hive.script.extensions.updateAllParent
import com.hive.script.extensions.updateChildParent
import com.hive.script.views.dialog.DialogCommandInsertSelector
import com.hive.script.views.dialog.DialogCommonSelector
import com.hive.script.views.dialog.DialogInputMessage
import com.hive.script.views.edit.xeditor.utils.XEditorSnapManager
import com.hive.script.views.manager.ScriptEditRunningManager
import com.hive.script.views.manager.ScriptMenuManager
import com.hive.script.views.menu.ScriptControlView
import com.hive.utils.GlobalApp
import com.hive.views.widgets.CommonToast

object ScriptMenuEditHelper {

    enum class ClickType {
        INSERT_RECORD, INSERT_RECORD_BEFORE, INSERT_RECORD_INNER, COPY, DELETE, RUN_CMD, RUN_NEXT_ALL, COMMENT, MANGER_SUB_TASK, UP, DOWN, INSERT_REFRACT, REFRESH
    }

    enum class InsertType {
        INSERT_BEFORE, INSERT_AFTER, INSERT_INNER
    }

    fun showEditDialog(
        context: Context,
        command: ScriptCommand,
        isDelayEdit: Boolean = false,
        onPostEvent: ((clickType: ClickType, cmd: ScriptCommand?) -> Unit),
        dataChanged: (command: ScriptCommand) -> Unit
    ) {
        val editCommand = command.deepCopy()
        val editView = ScriptEditFactory.createItemEditView(context, editCommand, isDelayEdit)
        if (editView.isSupportEdit()) {
            var title = editCommand.getCommandName()
            if (isDelayEdit) {
                title = GlobalApp.getString(com.hive.i8n.R.string.cmd_name_delay)
            }
            val dialog = DialogScriptCardEdit(context)
                .setTitle(title)
                .setEdtView(editView)
                .setCommonDelay(isDelayEdit)
                .setOnInflateFinished {
                    editView.bindCommand(editCommand)
                }
                .setOnConfirmClicked { dialog ->
                    try {
                        editView.checkCommandOrThrowError()
                        command.replaceTo(editCommand)
                        dataChanged.invoke(editCommand)
                        dialog.dismiss()

                    } catch (e: Exception) {
                        CommonToast.show(e.message)
                    }
                }.show()
            editView.setPostEventHandler(onPostEvent)
            editView.run {
                val h = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
                val w = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
                this.measure(w, h)
            }
            editView.onDismissed = {
                dialog.dismiss()
            }
        } else {
            CommonToast.show(GlobalApp.getString(com.hive.i8n.R.string.sc_not_support_edit))
        }
    }


    fun showAddDialog(
        context: Context,
        rootCmd: ScriptCommand,
        index: Int,
        dialogView: DialogScriptEdit?
    ) {
        DialogCommandInsertSelector(context, rootCmd, index, dialogView).show()
    }

    fun handleMenuEdit(
        context: Context,
        cmd: ScriptCommand?,
        cmd2: ScriptCommand? = null,
        type: ClickType,
        insertType: InsertType = InsertType.INSERT_AFTER,
        dialogView: DialogScriptEdit? = null,
    ) {

        when (type) {
            ClickType.COPY -> {
                val queue = cmd?.parentCommand?.commandQueue
                val pos = queue?.indexOf(cmd) ?: -1
                if (pos == -1 || queue == null) return
                insertCommand(queue, pos, cmd)
                dialogView?.updateData()
            }

            ClickType.DELETE -> {
                val queue = cmd?.parentCommand?.commandQueue
                val pos = queue?.indexOf(cmd) ?: -1
                if (pos == -1 || queue == null) return
                deleteCommand(queue, cmd)
                dialogView?.updateData()
            }

            ClickType.RUN_CMD -> {
                ScriptEditRunningManager.runCommand(dialogView, cmd, false)
            }

            ClickType.RUN_NEXT_ALL -> {
                ScriptEditRunningManager.runCommand(dialogView, cmd, true)
            }

            ClickType.COMMENT -> {
                insertComment(context, cmd!!, dialogView)
            }

            ClickType.UP -> {
                if (cmd!!.moveUp()) {
                    dialogView?.updateData()
                } else {
                    CommonToast.show(GlobalApp.getString(com.hive.i8n.R.string.sc_edit_move_up_failed))
                }
            }

            ClickType.DOWN -> {
                if (cmd!!.moveDown()) {
                    dialogView?.updateData()
                } else {
                    CommonToast.show(GlobalApp.getString(com.hive.i8n.R.string.sc_edit_move_down_failed))
                }
            }

            ClickType.INSERT_RECORD -> {
                val queue = cmd?.parentCommand?.commandQueue
                val pos = queue?.indexOf(cmd) ?: -1
                if (pos == -1 || queue == null) return
                showAddDialog(context, cmd.parentCommand!!, pos, dialogView)
            }

            ClickType.INSERT_RECORD_INNER -> {
                showAddDialog(context, cmd!!, -1, dialogView)
            }

            ClickType.INSERT_RECORD_BEFORE -> {
                showAddDialog(context, cmd!!.parentCommand!!, -1, dialogView)
            }

            ClickType.MANGER_SUB_TASK -> {
                dialogView?.loadData(cmd!!) {
                    dialogView.resetLocation()
                }
            }

            ClickType.INSERT_REFRACT -> {
                cmd ?: return
                insertAndRefract(cmd, cmd2, insertType)
                dialogView?.updateData()
            }

            ClickType.REFRESH -> {
                dialogView?.updateData()
            }
        }
    }

    private fun insertAndRefract(cmd: ScriptCommand, cmd2: ScriptCommand?, insertType: InsertType) {

        when (insertType) {
            InsertType.INSERT_AFTER -> {
                cmd.parentCommand?.commandQueue?.remove(cmd)
                val queue = cmd2?.parentCommand?.commandQueue
                val pos = queue?.indexOf(cmd2) ?: -1
                if (queue == null) return
                cmd.parentCommand = cmd2.parentCommand
                queue.add(pos + 1, cmd)
            }

            InsertType.INSERT_BEFORE -> {
                cmd.parentCommand?.commandQueue?.remove(cmd)
                val queue = cmd2?.parentCommand?.commandQueue
                val pos = queue?.indexOf(cmd2) ?: -1
                if (queue == null) return
                cmd.parentCommand = cmd2.parentCommand
                queue.add(pos, cmd)
            }

            InsertType.INSERT_INNER -> {
                cmd.parentCommand?.commandQueue?.remove(cmd)
                cmd2?.commandQueue?.add(cmd)
                cmd.parentCommand = cmd2
                cmd.updateAllParent()
            }
        }
        cmd2?.updateAllParent()
        XEditorSnapManager.get().save(cmd)
    }


    fun insertCommand(queue: MutableList<ScriptCommand>?, pos: Int, cmd: ScriptCommand?) {
        if (queue == null || cmd == null) {
            return
        }
        val nCmd = cmd.deepCopy()
        nCmd.parentCommand = queue.find { it.parentCommand != null }?.parentCommand
        queue.add(pos + 1, nCmd)
        XEditorSnapManager.get().save(queue.first())
    }

    fun insertCommand(
        anchorCommand: ScriptCommand,
        anchorCommandQueue: MutableList<ScriptCommand>,
        insertPosition: Int,
        insertQueue: MutableList<ScriptCommand>
    ) {
        val nQueue = mutableListOf<ScriptCommand>()
        insertQueue.forEach {
            val nCmd = it.deepCopy()
            nCmd.parentCommand = anchorCommandQueue.find { it.parentCommand != null }?.parentCommand
            nQueue.add(nCmd)
        }
        insertQueue.firstOrNull()?.parentCommand?.run {
            nQueue.forEach {
                it.parentCommand = this
            }
        }
        anchorCommandQueue.addAll(insertPosition + 1, nQueue)
        anchorCommand.updateChildParent()
        XEditorSnapManager.get().save(anchorCommand)
        if (nQueue.firstOrNull()?.isUnReachable() == true) {
            CommonToast.show(com.hive.i8n.R.string.sc_edit_untouch_wraning_info)
        }
    }

    fun inertGroupCommand(
        mCommand: ScriptCommand,
        queue: MutableList<ScriptCommand>,
        pos: Int,
        commandQueue: MutableList<ScriptCommand>,
        scriptName: String? = ""
    ) {
        val nQueue = mutableListOf<ScriptCommand>()
        val parentCommand = commandQueue.find { it.parentCommand != null }?.parentCommand
        commandQueue.forEach {
            val nCmd = it.deepCopy()
            nQueue.add(nCmd)
        }
        val cmdFor = CmdFor.createCommand(1, nQueue)
        cmdFor.comment = scriptName
        insertCommand(queue, pos, cmdFor)
        mCommand.updateChildParent()
    }

    private fun deleteCommand(queue: MutableList<ScriptCommand>?, itemCmd: ScriptCommand?) {
        if (queue == null || itemCmd == null) {
            return
        }
        queue.remove(itemCmd)
        XEditorSnapManager.get().save(itemCmd)
    }

    private fun insertComment(
        context: Context,
        itemCmd: ScriptCommand,
        dialogView: DialogScriptEdit? = null
    ) {
        DialogInputMessage(
            context,
            GlobalApp.getString(com.hive.i8n.R.string.sc_edit_comment_title),
            GlobalApp.getString(com.hive.i8n.R.string.sc_edit_comment_hint),
            itemCmd.comment,
            0,
            { editText ->
                val input = editText.text.toString()
                if (TextUtils.isEmpty(input)) {
                    throw Exception(GlobalApp.getString(com.hive.i8n.R.string.sc_edit_comment_empty))
                }
            }, { dialog, input ->
                itemCmd.comment = input
                dialogView?.notifyData()
                dialog.dismiss()
                XEditorSnapManager.get().save(itemCmd)
            }).show()
    }


    fun confirmInsertType(
        context: Context,
        mCommand: ScriptCommand,
        index: Int,
        commandQueue: MutableList<ScriptCommand>,
        dialogView: DialogScriptEdit?
    ) {
        DialogCommonSelector(context)
            .setTitle(com.hive.i8n.R.string.sc_add_type_title)
            .setDataSet(
                arrayListOf(
                    0 to GlobalApp.getString(com.hive.i8n.R.string.sc_add_type_1),
                    1 to GlobalApp.getString(com.hive.i8n.R.string.sc_add_type_2)
                )
            )
            .setSelectListener(object : DialogCommonSelector.OnSelectListener {
                override fun onSelected(
                    dialog: DialogCommonSelector,
                    pos: Int,
                    pair: Pair<Int, String>
                ) {
                    dialog.dismiss()
                    when (pair.first) {
                        0 -> {
                            insertCommand(mCommand, mCommand.commandQueue, index, commandQueue)
                        }

                        1 -> {
                            inertGroupCommand(
                                mCommand,
                                mCommand.commandQueue,
                                index,
                                commandQueue
                            )
                        }
                    }
                    mCommand.updateChildParent()
                    dialogView?.updateStatus()
                    dialogView?.updateData()
                    ScriptMenuManager.switchMenuMode(ScriptControlView.MenuMode.MAIN_MENU)
                    dialogView?.show()
                }

                override fun onCancel() {
                    dialogView?.updateData()
                    dialogView?.updateStatus()
                    dialogView?.show()
                }
            }).show()
    }
}