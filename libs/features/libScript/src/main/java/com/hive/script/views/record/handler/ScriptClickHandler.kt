// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.record.handler

import android.graphics.PointF
import android.view.View
import com.hive.script.ScriptProvider
import com.hive.script.base.ScriptConst
import com.hive.script.cmd.CmdRepeatTap
import com.hive.script.utils.ScriptCoordinateAdapter
import com.hive.script.views.edit.DialogScriptCardEdit
import com.hive.script.views.edit.ScriptEditFactory
import com.hive.script.views.manager.ScriptInsertManager
import com.hive.script.views.manager.ScriptManager
import com.hive.script.views.record.IScriptRecordView
import com.hive.script.views.record.ScriptRecordContainerView
import com.hive.script.views.record.ScriptRecordEventHandler
import com.hive.script.views.record.ScriptRecordViewManager
import com.hive.views.widgets.CommonToast

class ScriptClickHandler(recordView: IScriptRecordView) :
    ScriptRecordEventHandler(recordView) {

    override fun handleEvent(action: RecordResultAction, obj: Any?) {
        recordView.setViewState(
            recordView.getViewState()
                .ofFalse(ScriptRecordViewManager.RecordViewType.FAST_CLICK)
        )

        val point = obj as PointF?
        if (point != null) {
            showClickViewConfirmDialog(point)
        }
    }


    /**
     * 显示点击视图确认对话框（统一通过 DialogScriptCardEdit 编辑）
     */
    private fun showClickViewConfirmDialog(point: PointF) {
        (recordView as View).postDelayed({
            val cmd = CmdRepeatTap.createCommand(
                ScriptCoordinateAdapter.get().toRealX(point.x),
                ScriptCoordinateAdapter.get().toRealY(point.y),
                ScriptConst.Cmd_Fast_Click_Count_Default,
                ScriptConst.Cmd_Fast_Click_Gap_Default
            )

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
                                .ofFalse(ScriptRecordViewManager.RecordViewType.FAST_CLICK)
                        )
                        dialog.dismissNoNotify {
                            editView.postDelayed({
                                ScriptManager.addAndExecuteCommand(cmd) {
                                    ScriptRecordContainerView.get()?.invalidate()
                                    ScriptInsertManager.notifyInsertDismiss()
                                }
                            }, ScriptConst.Anim_Duration + 100L)
                        }
                    } catch (e: Exception) {
                        CommonToast.show(e.message)
                    }
                }
                .setOnDismissListener {
                    ScriptRecordContainerView.get()?.invalidate()
                    ScriptInsertManager.notifyInsertDismiss()
                }
                .show()
        }, 100)
    }

}