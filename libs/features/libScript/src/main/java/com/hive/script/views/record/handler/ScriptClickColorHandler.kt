// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.record.handler

import com.hive.script.ScriptProvider
import com.hive.script.base.ScriptClickActionHelper
import com.hive.script.base.ScriptConst
import com.hive.script.cmd.CmdClickColor
import com.hive.script.utils.ScriptColorHelper
import com.hive.script.views.edit.DialogScriptCardEdit
import com.hive.script.views.edit.ScriptEditFactory
import com.hive.script.views.manager.ScriptInsertManager
import com.hive.script.views.manager.ScriptManager
import com.hive.script.views.record.IScriptRecordView
import com.hive.script.views.record.ScriptRecordContainerView
import com.hive.script.views.record.ScriptRecordEventHandler
import com.hive.script.views.record.ScriptRecordViewManager
import com.hive.views.widgets.CommonToast

class ScriptClickColorHandler(recordView: IScriptRecordView) :
    ScriptRecordEventHandler(recordView) {

    override fun handleEvent(action: RecordResultAction, obj: Any?) {
        if (action == RecordResultAction.ACTION_CLICK_COLOR) {
            showClickColorDialog(obj as Int)
        } else if (action == RecordResultAction.ACTION_CANCEL) {
            recordView.setViewState(
                recordView.getViewState()
                    .ofFalse(ScriptRecordViewManager.RecordViewType.CLICK_COLOR)
            )
            ScriptInsertManager.notifyInsertDismiss()
        }
    }

    private fun showClickColorDialog(color: Int) {
        recordView.setViewState(
            recordView.getViewState()
                .ofFalse(ScriptRecordViewManager.RecordViewType.CLICK_COLOR)
        )
        ScriptRecordContainerView.get()?.invalidate()
        ScriptRecordContainerView.get()?.postDelayed({

            val cmd = CmdClickColor.createCommand(
                ScriptClickActionHelper.ACTION_CLICK,
                ScriptConst.Cmd_Spot_Color_Threshold,
                5,
                ScriptConst.Cmd_Fast_Click_Gap_Default,
                ScriptConst.Cmd_Long_Click_Default,
                color,
                CmdClickColor.COLOR_FIND_BLOCK
            )
            val editView = ScriptEditFactory.createItemEditView(
                ScriptProvider.getViewContext(),
                cmd,
                false
            )

            DialogScriptCardEdit(ScriptProvider.getViewContext()).setTitle(
                cmd.getCommandName() ?: ""
            ).setEdtView(editView).setOnInflateFinished {
                editView.bindCommand(cmd)
            }.setOnConfirmClicked { dialog ->
                try {
                    editView.checkCommandOrThrowError()
                    ScriptColorHelper.addColorToFirst(cmd.targetColor)
                    recordView.setViewState(
                        recordView.getViewState()
                            .ofFalse(ScriptRecordViewManager.RecordViewType.CLICK_COLOR)
                    )
                    dialog.dismissNoNotify {
                        editView.postDelayed({
                            ScriptManager.addAndExecuteCommand(cmd){
                                ScriptRecordContainerView.get()?.invalidate()
                                ScriptInsertManager.notifyInsertDismiss()
                            }
                        }, 500)
                    }

                } catch (e: Exception) {
                    CommonToast.show(e.message)
                }
            }.setOnDismissListener {
                ScriptRecordContainerView.get()?.invalidate()
                ScriptInsertManager.notifyInsertDismiss()
            }.show()

        }, 50)
    }
}