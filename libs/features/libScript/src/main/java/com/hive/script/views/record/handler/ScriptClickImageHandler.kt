// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.record.handler

import android.graphics.Bitmap
import android.graphics.RectF
import android.view.View
import com.hive.extension.visibleOrInvisible
import com.hive.script.ScriptProvider
import com.hive.script.ScriptScreenShotService
import com.hive.script.base.ScriptClickActionHelper
import com.hive.script.base.ScriptCommand
import com.hive.script.base.ScriptConst
import com.hive.script.cmd.CmdClickImage
import com.hive.script.views.dialog.DialogImagePreview
import com.hive.script.views.edit.DialogScriptCardEdit
import com.hive.script.views.edit.ScriptEditFactory
import com.hive.script.views.manager.ScriptInsertManager
import com.hive.script.views.manager.ScriptManager
import com.hive.script.views.manager.ScriptMenuManager
import com.hive.script.views.manager.ScriptRecordManager
import com.hive.script.views.record.IScriptRecordView
import com.hive.script.views.record.ScriptRecordContainerView
import com.hive.script.views.record.ScriptRecordEventHandler
import com.hive.script.views.record.ScriptRecordViewManager
import com.hive.utils.file.FileUtils
import com.hive.views.widgets.CommonToast

class ScriptClickImageHandler(recordView: IScriptRecordView) :
    ScriptRecordEventHandler(recordView) {

    override fun handleEvent(action: RecordResultAction, obj: Any?) {

        val p = obj as Pair<Int, Any?>?
        val type = p?.first
        if (type == 5) {
            when (ScriptRecordManager.clickImageType) {
                ScriptRecordManager.RecordClickImageType.DEFAULT -> showClickImageDialog(
                    obj.second as RectF
                )

                ScriptRecordManager.RecordClickImageType.INSERT_IMAGE -> showInsertImageDialog(
                    obj.second as RectF
                )

            }

        } else if (type == -1) {
            recordView.setViewState(
                recordView.getViewState()
                    .ofFalse(ScriptRecordViewManager.RecordViewType.CLICK_IMAGE)
            )
            ScriptInsertManager.notifyInsertDismiss()
        }
    }

    private fun showClickImageDialog(rect: RectF) {
        recordView.setViewState(
            recordView.getViewState()
                .ofFalse(ScriptRecordViewManager.RecordViewType.CLICK_IMAGE)
        )
        ScriptRecordContainerView.get()?.visibleOrInvisible(false)
        ScriptMenuManager.saveMenuState()
        ScriptMenuManager.hiddenMenuView()
        ScriptRecordContainerView.get()?.invalidate()
        (recordView as View).postDelayed({
            try {
                val targetBitmap = ScriptScreenShotService.instance?.getScreenShot()
                ScriptRecordContainerView.get()?.visibleOrInvisible(true)

                val cmd = createCommand(targetBitmap, rect)
                val editView = ScriptEditFactory.createItemEditView(
                    ScriptProvider.getViewContext(),
                    cmd,
                    false
                )
                DialogScriptCardEdit(ScriptProvider.getViewContext()).setTitle(
                    cmd.getCommandName() ?: ""
                )
                    .setEdtView(editView).setOnInflateFinished {
                        editView.bindCommand(cmd)
                    }.setOnConfirmClicked { dialog ->
                        try {
                            editView.checkCommandOrThrowError()
                            recordView.setViewState(
                                recordView.getViewState()
                                    .ofFalse(ScriptRecordViewManager.RecordViewType.CLICK_IMAGE)
                            )
                            ScriptMenuManager.restoreMenuState()
                            dialog.dismissNoNotify {
                                editView.postDelayed({
                                    ScriptManager.addAndExecuteCommand(cmd){
                                        ScriptRecordContainerView.get()?.invalidate()
                                        ScriptInsertManager.notifyInsertDismiss()
                                    }
                                }, 300)
                            }
                        } catch (e: Exception) {
                            CommonToast.show(e.message)
                        }
                    }.setOnDismissListener {
                        ScriptRecordContainerView.get()?.invalidate()
                        ScriptInsertManager.notifyInsertDismiss()
                    }.show()

            } catch (ignore: Exception) {
                ignore.printStackTrace()
                ScriptRecordContainerView.get()?.invalidate()
                ScriptInsertManager.notifyInsertDismiss()
            }

        }, 100)

    }


    private fun createCommand(targetBitmap: Bitmap?, targetRect: RectF): ScriptCommand {
        val cmd = CmdClickImage.createCommand(
            ScriptClickActionHelper.ACTION_CLICK,
            ScriptConst.Cmd_Spot_Accuracy,
            5,
            ScriptConst.Cmd_Fast_Click_Gap_Default,
            ScriptConst.Cmd_Long_Click_Default,
            "-"
        )
        targetBitmap?.run {
            val clipBitmap = Bitmap.createBitmap(
                targetBitmap,
                targetRect.left.toInt(),
                targetRect.top.toInt(),
                targetRect.width().toInt(),
                targetRect.height().toInt(),
                null,
                false
            )
            val tempPath = ScriptConst.newRandomFullPath()
            FileUtils.saveBitmapToFile(tempPath, clipBitmap)
            val relativePath = ScriptConst.newMd5RelativePath(tempPath)
            val savePath = ScriptConst.Save_Script_Temp_Path + relativePath
            FileUtils.saveBitmapToFile(savePath, clipBitmap)
            cmd.attachmentFiles = mutableListOf(relativePath)

        }
        return cmd
    }


    private fun showInsertImageDialog(rect: RectF) {
        recordView.setViewState(
            recordView.getViewState()
                .ofFalse(ScriptRecordViewManager.RecordViewType.CLICK_IMAGE)
        )
        ScriptRecordContainerView.get()?.visibleOrInvisible(false)
        ScriptMenuManager.saveMenuState()
        ScriptMenuManager.hiddenMenuView()
        ScriptRecordContainerView.get()?.invalidate()
        ScriptRecordContainerView.get()?.postDelayed({
            try {
                val targetBitmap = ScriptScreenShotService.instance?.getScreenShot()
                ScriptRecordContainerView.get()?.visibleOrInvisible(true)
                DialogImagePreview(ScriptProvider.getViewContext()).loadBitmap(targetBitmap, rect)
                    .setOnBitmapSaveListener(object : DialogImagePreview.OnBitmapSaveListener {
                        override fun onConfirmClicked(path: String) {
                            recordView.setViewState(
                                recordView.getViewState()
                                    .ofFalse(ScriptRecordViewManager.RecordViewType.CLICK_IMAGE)
                            )
                            ScriptInsertManager.notifyInsertImage(path)
                            ScriptMenuManager.restoreMenuState()
                            ScriptRecordContainerView.get()?.invalidate()
                        }

                        override fun onDismissed() {
                            ScriptInsertManager.notifyInsertDismiss()
                            ScriptRecordContainerView.get()?.invalidate()
                        }
                    }).show()
            } catch (ignore: Exception) {
                ignore.printStackTrace()
                ScriptRecordContainerView.get()?.invalidate()
                ScriptInsertManager.notifyInsertDismiss()
            }

        }, 100)

    }
}