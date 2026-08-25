// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.record.handler

import android.view.View
import com.hive.script.ScriptProvider
import com.hive.script.base.ScriptConst
import com.hive.script.cmd.CmdPatternTap
import com.hive.script.views.edit.DialogScriptCardEdit
import com.hive.script.views.edit.ScriptEditFactory
import com.hive.script.views.manager.ScriptInsertManager
import com.hive.script.views.manager.ScriptManager
import com.hive.script.views.record.IScriptRecordView
import com.hive.script.views.record.ScriptRecordEventHandler
import com.hive.script.views.record.ScriptRecordViewManager
import com.hive.views.widgets.CommonToast

class ScriptBatchClickHandler(recordView: IScriptRecordView) :
    ScriptRecordEventHandler(recordView) {

    override fun handleEvent(action: RecordResultAction, obj: Any?) {
        if (obj == null) {
            recordView.setViewState(
                recordView.getViewState()
                    .ofFalse(ScriptRecordViewManager.RecordViewType.BATCH_CLICK)
            )
            ScriptInsertManager.notifyInsertDismiss()
        } else {
            showBatchClickConfirmDialog(obj as CmdPatternTap)
        }
    }

    /**
     * 显示批量点击确认对话框（统一通过 DialogScriptCardEdit 编辑）
     */
    private fun showBatchClickConfirmDialog(cmd: CmdPatternTap) {
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
                            recordView.getViewState()
                                .ofFalse(ScriptRecordViewManager.RecordViewType.BATCH_CLICK)
                        )
                        dialog.dismissNoNotify {
                            editView.postDelayed({
                                ScriptManager.addAndExecuteCommand(cmd){
                                    ScriptInsertManager.notifyInsertDismiss()
                                }
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