// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.record.handler

import android.graphics.RectF
import com.hive.script.ScriptProvider
import com.hive.script.views.dialog.DialogScriptAlert
import com.hive.script.views.manager.ScriptInsertManager
import com.hive.script.views.manager.ScriptRecordManager
import com.hive.script.views.record.IScriptRecordView
import com.hive.script.views.record.ScriptRecordEventHandler

class ScriptSetSizeHandler(recordView: IScriptRecordView) :
    ScriptRecordEventHandler(recordView) {

    override fun handleEvent(action: RecordResultAction, obj: Any?) {
        if (obj == null) {
            ScriptRecordManager.notifyRecordResultListener(RecordResultAction.ACTION_CANCEL, null)
            ScriptInsertManager.notifyInsertDismiss()
        } else {
            val p = obj as Pair<Int, Any>
            showLayoutSizeConfirmDialog(p.second as RectF)
        }
    }

    /**
     * 设置区域大小确认对话框
     */
    private fun showLayoutSizeConfirmDialog(rect: RectF) {
        DialogScriptAlert(ScriptProvider.getViewContext())
            .setTitle(com.hive.i8n.R.string.sc_edit_record_set_size_dialog_title)
            .setContent(com.hive.i8n.R.string.sc_edit_record_set_size_dialog_msg)
            .setConfirmText(com.hive.i8n.R.string.sc_edit_record_set_size_dialog_confirm)
            .setOnDialogEventListener(object : DialogScriptAlert.OnDialogEventListener {
                override fun onClickEvent(dialog: DialogScriptAlert, isCancel: Boolean) {
                    dialog.dismiss()
                    if (isCancel) {
                        ScriptRecordManager.notifyRecordResultListener(
                            RecordResultAction.ACTION_CANCEL,
                            null
                        )
                    } else {
                        ScriptRecordManager.notifyRecordResultListener(
                            RecordResultAction.ACTION_SIZE,
                            rect
                        )
                    }
                }
            }).show()
    }
}