// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.record.handler

import android.view.View
import com.hive.script.ScriptProvider
import com.hive.script.base.ScriptConst
import com.hive.script.cmd.CmdPinch
import com.hive.script.views.edit.DialogScriptCardEdit
import com.hive.script.views.edit.ScriptEditFactory
import com.hive.script.views.manager.ScriptInsertManager
import com.hive.script.views.manager.ScriptManager
import com.hive.script.views.record.IScriptRecordView
import com.hive.script.views.record.ScriptRecordEventHandler
import com.hive.script.views.record.ScriptRecordViewManager
import com.hive.views.widgets.CommonToast

class ScriptMultipleHandler(recordView: IScriptRecordView) :
    ScriptRecordEventHandler(recordView) {
    override fun handleEvent(action: RecordResultAction, obj: Any?) {
        if (obj == null) {
            recordView.setViewState(
                recordView.getViewState().ofFalse(
                    ScriptRecordViewManager.RecordViewType.MULTIPLE
                )
            )
            ScriptInsertManager.notifyInsertDismiss()
        } else {
            val cmd = obj as CmdPinch
            showMultipleConfirmDialog(cmd)
        }
    }

    /**
     * 显示多指确认对话框（统一通过 DialogScriptCardEdit 编辑）
     */
    private fun showMultipleConfirmDialog(cmd: CmdPinch) {
        (recordView as View).postDelayed({
            val editView = ScriptEditFactory.createItemEditView(
                ScriptProvider.getViewContext(),
                cmd,
                false
            )
            DialogScriptCardEdit(ScriptProvider.getViewContext())
                .setTitle(cmd.getCommandName() ?: "")
                .setEdtView(editView)
                .setOnInflateFinished { editView.bindCommand(cmd) }
                .setOnConfirmClicked { dialog ->
                    try {
                        editView.checkCommandOrThrowError()
                        recordView.setViewState(
                            recordView.getViewState().ofFalse(
                                ScriptRecordViewManager.RecordViewType.MULTIPLE
                            )
                        )
                        dialog.dismissNoNotify {
                            editView.postDelayed({
                                ScriptManager.addAndExecuteCommand(cmd)
                                editView.postDelayed({
                                    ScriptInsertManager.notifyInsertDismiss()
                                }, cmd.getExecuteDuration() + 100L)
                            }, ScriptConst.Anim_Duration + 100L)
                        }
                    } catch (e: Exception) {
                        CommonToast.show(e.message)
                    }
                }
                .setOnDismissListener {
                    ScriptInsertManager.notifyInsertDismiss()
                }
                .show()
        }, 100)
    }
}