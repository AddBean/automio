// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.record.handler

import android.view.LayoutInflater
import android.view.View
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.TextView
import com.hive.script.R
import com.hive.script.ScriptProvider
import com.hive.script.base.ScriptClickActionHelper
import com.hive.script.base.ScriptConst
import com.hive.script.cmd.CmdClickView
import com.hive.script.cmd.CmdInput
import com.hive.script.cmd.CmdReadViewText
import com.hive.script.views.dialog.DialogCmdInputEdit
import com.hive.script.views.dialog.DialogCustomListSelector
import com.hive.script.views.edit.DialogScriptCardEdit
import com.hive.script.views.edit.ScriptEditFactory
import com.hive.script.views.manager.ScriptInsertManager
import com.hive.script.views.manager.ScriptManager
import com.hive.script.views.manager.ScriptRecordManager
import com.hive.script.views.record.IScriptRecordView
import com.hive.script.views.record.ScriptRecordContainerView
import com.hive.script.views.record.ScriptRecordEventHandler
import com.hive.script.views.record.ScriptRecordViewManager
import com.hive.utils.extends.dp
import com.hive.views.widgets.CommonToast

class ScriptClickViewHandler(recordView: IScriptRecordView) : ScriptRecordEventHandler(recordView) {

    override fun handleEvent(action: RecordResultAction, obj: Any?) {
        fun finishOpt() {
            recordView.setViewState(
                recordView.getViewState().ofFalse(ScriptRecordViewManager.RecordViewType.CLICK_VIEW)
            )
            ScriptInsertManager.notifyInsertDismiss()
        }

        val pair = obj as Pair<AccessibilityNodeInfo?, List<AccessibilityNodeInfo>?>?
        val node = pair?.first
        val nodeList = pair?.second
        if (node == null) {
            if (ScriptRecordManager.clickViewType == ScriptRecordManager.RecordClickViewType.INPUT_VIEW) {
                showInputConfirmDialog(
                    null
                )
            } else {
                finishOpt()
            }
        } else {
            fun confirmSelected(node: AccessibilityNodeInfo) {

                when (ScriptRecordManager.clickViewType) {
                    ScriptRecordManager.RecordClickViewType.SELECT_VIEW, ScriptRecordManager.RecordClickViewType.SELECT_EDIT_VIEW -> {
                        ScriptRecordManager.notifyRecordResultListener(
                            RecordResultAction.ACTION_CANCEL, node
                        )
                        ScriptInsertManager.notifyInsertDismiss()
                    }

                    ScriptRecordManager.RecordClickViewType.CLICK_VIEW -> showClickViewConfirmDialog(
                        node
                    )

                    ScriptRecordManager.RecordClickViewType.INPUT_VIEW -> showInputConfirmDialog(
                        node
                    )

                    ScriptRecordManager.RecordClickViewType.READ_VIEW_TEXT -> showReadViewTextConfirmDialog(
                        node
                    )

                    null -> {}
                }
            }
            if ((nodeList?.size ?: 0) > 1) {
                showNodeListSelectedDialog(nodeList, {
                    confirmSelected(it)
                }, {
                    finishOpt()
                    ScriptInsertManager.notifyInsertDismiss()
                })
            } else {
                confirmSelected(node)
            }
        }
    }

    /**
     * 显示节点列表选择对话框
     */
    private fun showNodeListSelectedDialog(
        nodeList: List<AccessibilityNodeInfo>?,
        confirm: (AccessibilityNodeInfo) -> Unit,
        onCancel: () -> Unit = {}
    ) {
        val dialog = DialogCustomListSelector(ScriptProvider.getViewContext())
        dialog.setTitle(com.hive.i8n.R.string.sc_record_node_list_selected_title)
        val list = nodeList?.mapIndexed { _, node ->
            0 to node
        }?.toMutableList() ?: mutableListOf()
        dialog.getContainer()?.setPadding(0, 4.dp, 0, 0)
        dialog.setDataSet(list as MutableList<Pair<Int, Any>>) { data: Pair<Int, Any> ->
            val itemView = LayoutInflater.from(ScriptProvider.getViewContext())
                .inflate(R.layout.sc_record_node_list_selected_item, null)
            val tvId = itemView.findViewById<TextView>(R.id.tvId)
            val tvText = itemView.findViewById<TextView>(R.id.tvText)
            val tvTag = itemView.findViewById<TextView>(R.id.tvTag)
            val data = data as Pair<Int, AccessibilityNodeInfo>
            tvId.text = data.second.viewIdResourceName
            tvText.text = data.second.text
            tvTag.text = data.second.contentDescription
            if (tvText.text == "") {
                tvText.text = "-"
            }
            if (tvId.text == "") {
                tvId.text = "-"
            }
            if (tvTag.text == "") {
                tvTag.text = "-"
            }
            itemView
        }
        dialog.setSelectListener(object : DialogCustomListSelector.OnSelectListener() {
            override fun onSelected(
                dialog: DialogCustomListSelector, pos: Int, pair: Pair<Int, Any>
            ) {
                confirm(pair.second as AccessibilityNodeInfo)
            }

            override fun onCancel() {
                super.onCancel()
                onCancel.invoke()
            }
        })

        dialog.show()
    }

    /**
     * 显示点击视图确认对话框
     */
    private fun showClickViewConfirmDialog(it: AccessibilityNodeInfo) {
        (recordView as View).postDelayed({
            val cmd = CmdClickView.createCommand(
                ScriptClickActionHelper.ACTION_CLICK,
                5,
                ScriptConst.Cmd_Fast_Click_Gap_Default,
                ScriptConst.Cmd_Long_Click_Default,
                it.viewIdResourceName,
                it.text?.toString(),
                it.contentDescription?.toString()
            )

            val editView = ScriptEditFactory.createItemEditView(
                ScriptProvider.getViewContext(), cmd, false
            )

            DialogScriptCardEdit(ScriptProvider.getViewContext()).setTitle(
                cmd.getCommandName() ?: ""
            ).setEdtView(editView).setOnInflateFinished {
                editView.bindCommand(cmd)
            }.setOnConfirmClicked { dialog ->
                try {
                    editView.checkCommandOrThrowError()
                    recordView.setViewState(
                        recordView.getViewState()
                            .ofFalse(ScriptRecordViewManager.RecordViewType.CLICK_VIEW)
                    )
                    dialog.dismissNoNotify {
                        dialog.postDelayed({
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

    /**
     * 显示输入确认对话框
     */
    private fun showInputConfirmDialog(it: AccessibilityNodeInfo?) {
        (recordView as View).postDelayed({
            DialogCmdInputEdit(ScriptProvider.getViewContext()).bindNodeInfo(it)
                .setOnConfirmListener(object : DialogCmdInputEdit.OnConfirmListener {
                    override fun onConfirmClicked(dialog: DialogCmdInputEdit, cmd: CmdInput) {
                        dialog.dismiss()
                        recordView.setViewState(
                            recordView.getViewState()
                                .ofFalse(ScriptRecordViewManager.RecordViewType.CLICK_VIEW)
                        )
                        dialog.postDelayed({
                            ScriptManager.addAndExecuteCommand(cmd)
                        }, 500)
                    }

                    override fun onDismissed() {
                        ScriptInsertManager.notifyInsertDismiss()
                    }
                }).show()
        }, 50)
    }


    /**
     * 显示读取视图文本确认对话框
     */
    private fun showReadViewTextConfirmDialog(it: AccessibilityNodeInfo) {
        (recordView as View).postDelayed({
            val cmdInsert = CmdReadViewText.createCommand(it.viewIdResourceName)
            val editView = ScriptEditFactory.createItemEditView(
                ScriptProvider.getViewContext(), cmdInsert, false
            )
            DialogScriptCardEdit(ScriptProvider.getViewContext()).setTitle(
                cmdInsert.getCommandName() ?: ""
            ).setEdtView(editView).setOnInflateFinished {
                editView.bindCommand(cmdInsert)
            }.setOnConfirmClicked { dialog ->
                try {
                    editView.checkCommandOrThrowError()
                    recordView.setViewState(
                        recordView.getViewState()
                            .ofFalse(ScriptRecordViewManager.RecordViewType.CLICK_VIEW)
                    )

                    dialog.dismissNoNotify {
                        dialog.postDelayed({
                            ScriptManager.addAndExecuteCommand(cmdInsert){
                                ScriptRecordContainerView.get()?.invalidate()
                                ScriptInsertManager.notifyInsertDismiss()
                            }
                        }, 300)
                    }
                } catch (e: Exception) {
                    recordView.setViewState(
                        recordView.getViewState()
                            .ofFalse(ScriptRecordViewManager.RecordViewType.CLICK_VIEW)
                    )
                    ScriptInsertManager.notifyInsertDismiss()
                    CommonToast.show(e.message)
                }
            }.setOnDismissListener {
                ScriptInsertManager.notifyInsertDismiss()
            }.show()

        }, 50)
    }
}