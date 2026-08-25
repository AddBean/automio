// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.manager

import android.content.Context
import android.graphics.PointF
import android.graphics.RectF
import android.view.accessibility.AccessibilityNodeInfo
import com.hive.script.ActivityRequestPermissionCapture
import com.hive.script.ScriptProvider
import com.hive.script.ScriptScreenShotService
import com.hive.script.base.ScriptCommand
import com.hive.script.base.ScriptConst
import com.hive.script.base.ScriptRecordHelper
import com.hive.script.base.core.ScriptInterpreter
import com.hive.script.base.params.ScriptParamEnv
import com.hive.script.base.params.ScriptSystemParam
import com.hive.script.cmd.CmdSet
import com.hive.script.views.dialog.DialogCommonSelector
import com.hive.script.views.menu.ScriptControlView
import com.hive.script.views.record.ScriptRecordViewManager
import com.hive.script.views.widgets.BaseScriptDialog
import com.hive.script.views.widgets.ScriptClickView
import com.hive.utils.extends.string
import com.hive.utils.thread.UIHandlerUtils
import com.hive.views.widgets.CommonToast

object ScriptInsertManager {

    /**
     * 选择一个View的ID
     */
    fun startSelectViewId(isEditView: Boolean, onFindId: (id: String?) -> Unit) {


        if (ScriptManager.checkAccessibility()) return
        if (ScriptInterpreter.getDefault().isRecording()) return

        BaseScriptDialog.saveStateAndHidden()
        ScriptMenuManager.saveMenuState()
        ScriptMenuManager.hiddenMenuView()
        ScriptRecordManager.clearViewState()
        ScriptRecordManager.saveViewState()
        ScriptRecordManager.showRecordView()
        if (isEditView) {
            ScriptRecordManager.setRecordClickViewType(ScriptRecordManager.RecordClickViewType.SELECT_EDIT_VIEW)
        } else {
            ScriptRecordManager.setRecordClickViewType(ScriptRecordManager.RecordClickViewType.SELECT_VIEW)
        }


        ScriptRecordManager.updateRecordView(
            ScriptRecordViewManager.ViewState.default()
                .ofTrue(ScriptRecordViewManager.RecordViewType.CLICK_VIEW)
                .ofTrue(ScriptRecordViewManager.RecordViewType.TOUCHABLE)
                .ofTrue(ScriptRecordViewManager.RecordViewType.PREVIEW_DRAW)
                .ofFalse(ScriptRecordViewManager.RecordViewType.MENU)
        )
        ScriptRecordManager.setRecordResultListener { action, data ->
            ScriptMenuManager.restoreMenuState()
            ScriptRecordManager.restoreViewState()
            ScriptRecordManager.setRecordResultListener(null)
            BaseScriptDialog.restoreState()
            val node = data as? AccessibilityNodeInfo
            onFindId.invoke(node?.viewIdResourceName)
        }

        registerInsertListener(object : OnInsertListener {

            override fun onInsertDismiss() {
                ScriptRecordManager.restoreViewState()
                ScriptRecordManager.setRecordResultListener(null)
                ScriptRecordManager.hiddenRecordView()
                ScriptMenuManager.restoreMenuState()
                ScriptInterpreter.getDefault().stopExecute()
                BaseScriptDialog.restoreState()
            }

        })
    }

    /**
     * 选择一张图片
     */
    fun startPickImage(
        listener: OnInsertListener? = null
    ) {
        val list = object : OnInsertListener {
            override fun onPickImage(image: String?) {
                ScriptRecordManager.restoreViewState()
                ScriptRecordManager.setRecordResultListener(null)
                BaseScriptDialog.restoreState()
                listener?.onPickImage(image)
            }

            override fun onInsertDismiss() {
                ScriptRecordManager.restoreViewState()
                ScriptRecordManager.setRecordResultListener(null)
                BaseScriptDialog.restoreState()
                listener?.onInsertDismiss()
            }
        }

        BaseScriptDialog.saveStateAndHidden()
        ScriptRecordManager.clearViewState()
        ScriptRecordManager.saveViewState()
        requestPermission(list) {
            ScriptRecordManager.setRecordClickImageType(ScriptRecordManager.RecordClickImageType.INSERT_IMAGE)
            startHandleInsert(
                ScriptRecordViewManager.ViewState.default()
                    .ofTrue(ScriptRecordViewManager.RecordViewType.CLICK_IMAGE)
                    .ofTrue(ScriptRecordViewManager.RecordViewType.TOUCHABLE)
                    .ofTrue(ScriptRecordViewManager.RecordViewType.PREVIEW_DRAW)
                    .ofFalse(ScriptRecordViewManager.RecordViewType.MENU),
                object : OnInsertListener {
                    override fun onPickImage(image: String?) {
                        list.onPickImage(image)
                    }

                    override fun onInsertDismiss() {
                        list.onInsertDismiss()
                    }
                }
            )
        }
    }

    fun startInsertFastClick(
        listener: OnInsertListener? = null
    ) {
        ScriptClickView.setNormalizedPoint(PointF(0.5f, 0.5f))
        startHandleInsert(
            ScriptRecordViewManager.ViewState.default()
                .ofTrue(ScriptRecordViewManager.RecordViewType.FAST_CLICK)
                .ofTrue(ScriptRecordViewManager.RecordViewType.TOUCHABLE)
                .ofTrue(ScriptRecordViewManager.RecordViewType.PREVIEW_DRAW)
                .ofFalse(ScriptRecordViewManager.RecordViewType.MENU), listener
        )
    }

    fun startInsertClickOrScroll(
        listener: OnInsertListener? = null
    ) {
        UIHandlerUtils.getInstance().postDelayed({
            startHandleInsert(
                ScriptRecordViewManager.ViewState.default()
                    .ofTrue(ScriptRecordViewManager.RecordViewType.TOUCHABLE)
                    .ofTrue(ScriptRecordViewManager.RecordViewType.PREVIEW_DRAW)
                    .ofFalse(ScriptRecordViewManager.RecordViewType.MENU), listener
            )
        }, 700)

    }

    fun startInsertScaleInOut(
        listener: OnInsertListener? = null
    ) {
        startHandleInsert(
            ScriptRecordViewManager.ViewState.default()
                .ofTrue(ScriptRecordViewManager.RecordViewType.SCALE_IN_OUT)
                .ofTrue(ScriptRecordViewManager.RecordViewType.TOUCHABLE)
                .ofTrue(ScriptRecordViewManager.RecordViewType.PREVIEW_DRAW)
                .ofFalse(ScriptRecordViewManager.RecordViewType.MENU), listener
        )
    }

    fun startInsertScrollMultiple(
        listener: OnInsertListener? = null
    ) {
        startHandleInsert(
            ScriptRecordViewManager.ViewState.default()
                .ofTrue(ScriptRecordViewManager.RecordViewType.MULTIPLE)
                .ofTrue(ScriptRecordViewManager.RecordViewType.TOUCHABLE)
                .ofTrue(ScriptRecordViewManager.RecordViewType.PREVIEW_DRAW)
                .ofFalse(ScriptRecordViewManager.RecordViewType.MENU), listener
        )
    }

    fun startInsertClickImage(
        listener: OnInsertListener? = null
    ) {
        requestPermission(listener) {
            ScriptRecordManager.setRecordClickImageType(ScriptRecordManager.RecordClickImageType.DEFAULT)
            startHandleInsert(
                ScriptRecordViewManager.ViewState.default()
                    .ofTrue(ScriptRecordViewManager.RecordViewType.CLICK_IMAGE)
                    .ofTrue(ScriptRecordViewManager.RecordViewType.TOUCHABLE)
                    .ofTrue(ScriptRecordViewManager.RecordViewType.PREVIEW_DRAW)
                    .ofFalse(ScriptRecordViewManager.RecordViewType.MENU), listener
            )
        }
    }

    fun startInsertClickColor(
        listener: OnInsertListener? = null
    ) {
        requestPermission(listener) {
            startHandleInsert(
                ScriptRecordViewManager.ViewState.default()
                    .ofTrue(ScriptRecordViewManager.RecordViewType.CLICK_COLOR)
                    .ofTrue(ScriptRecordViewManager.RecordViewType.TOUCHABLE)
                    .ofTrue(ScriptRecordViewManager.RecordViewType.PREVIEW_DRAW)
                    .ofFalse(ScriptRecordViewManager.RecordViewType.MENU), listener
            )
        }
    }

    fun startInsertSetCmd(context: Context, onSelectedConfirm: (cmd: CmdSet) -> Unit) {
        DialogCommonSelector(context).setTitle(com.hive.i8n.R.string.sc_condition_edit_set_param_menu_title.string())
            .setDataSet(
                mutableListOf(
                    0 to com.hive.i8n.R.string.sc_condition_edit_set_param_menu_content.string(),
                    1 to com.hive.i8n.R.string.sc_condition_edit_set_param_menu_system.string(),
                    2 to com.hive.i8n.R.string.sc_condition_edit_set_param_menu_regular.string(),
                    3 to com.hive.i8n.R.string.sc_condition_edit_set_param_menu_expression.string(),
                )
            ).setSelectListener(object : DialogCommonSelector.OnSelectListener {
                override fun onSelected(
                    dialog: DialogCommonSelector, pos: Int, pair: Pair<Int, String>
                ) {
                    dialog.dismiss()
                    val fullParamId =
                        ScriptParamEnv.getDefaultParam()?.getFullId() ?: return
                    val fullSystemParamId =
                        ScriptParamEnv.getDefaultSysParam()?.getFullId() ?: return
                    val cmd = when (pair.first) {
                        0 -> {
                            CmdSet.createAction2(fullParamId, "")
                        }

                        1 -> {
                            CmdSet.createAction1(fullParamId, ScriptSystemParam.CLIPBOARD)
                        }

                        2 -> {
                            CmdSet.createAction3(fullParamId, "", fullSystemParamId)
                        }

                        3 -> {
                            CmdSet.createAction4(fullParamId, "")
                        }

                        else -> {
                            null
                        }
                    }
                    cmd ?: return
                    onSelectedConfirm(cmd)
                }

                override fun onCancel() {
                }

            }).show()
    }


    fun startInsertReadViewText(
        listener: OnInsertListener? = null
    ) {
        UIHandlerUtils.getInstance().postDelayed({
            ScriptRecordManager.setRecordClickViewType(ScriptRecordManager.RecordClickViewType.READ_VIEW_TEXT)
            startHandleInsert(
                ScriptRecordViewManager.ViewState.default()
                    .ofTrue(ScriptRecordViewManager.RecordViewType.CLICK_VIEW)
                    .ofTrue(ScriptRecordViewManager.RecordViewType.TOUCHABLE)
                    .ofTrue(ScriptRecordViewManager.RecordViewType.PREVIEW_DRAW)
                    .ofFalse(ScriptRecordViewManager.RecordViewType.MENU), listener
            )
        }, 500)

    }

    fun startInsertClickView(
        listener: OnInsertListener? = null
    ) {
        UIHandlerUtils.getInstance().postDelayed({
            ScriptRecordManager.setRecordClickViewType(ScriptRecordManager.RecordClickViewType.CLICK_VIEW)
            startHandleInsert(
                ScriptRecordViewManager.ViewState.default()
                    .ofTrue(ScriptRecordViewManager.RecordViewType.CLICK_VIEW)
                    .ofTrue(ScriptRecordViewManager.RecordViewType.TOUCHABLE)
                    .ofTrue(ScriptRecordViewManager.RecordViewType.PREVIEW_DRAW)
                    .ofFalse(ScriptRecordViewManager.RecordViewType.MENU), listener
            )
        }, 500)

    }


    fun startInsertInput(
        listener: OnInsertListener? = null
    ) {
        UIHandlerUtils.getInstance().postDelayed({
            ScriptRecordManager.setRecordClickViewType(ScriptRecordManager.RecordClickViewType.INPUT_VIEW)
            startHandleInsert(
                ScriptRecordViewManager.ViewState.default()
                    .ofTrue(ScriptRecordViewManager.RecordViewType.CLICK_VIEW)
                    .ofTrue(ScriptRecordViewManager.RecordViewType.TOUCHABLE)
                    .ofTrue(ScriptRecordViewManager.RecordViewType.PREVIEW_DRAW)
                    .ofFalse(ScriptRecordViewManager.RecordViewType.MENU), listener
            )
        }, 500)
    }

    fun startInsertBatchClick(
        listener: OnInsertListener? = null
    ) {
        startHandleInsert(
            ScriptRecordViewManager.ViewState.default()
                .ofTrue(ScriptRecordViewManager.RecordViewType.BATCH_CLICK)
                .ofTrue(ScriptRecordViewManager.RecordViewType.TOUCHABLE)
                .ofTrue(ScriptRecordViewManager.RecordViewType.PREVIEW_DRAW)
                .ofFalse(ScriptRecordViewManager.RecordViewType.MENU), listener
        )
    }

    /**
     * 开始设置限制区域
     */
    fun startSetRectLayout(cmd: ScriptCommand, callback: () -> Unit) {
        if (ScriptManager.checkAccessibility()) return
        BaseScriptDialog.saveStateAndHidden()
        ScriptRecordManager.clearViewState()
        ScriptRecordManager.saveViewState()
        ScriptRecordManager.showRecordView()
        ScriptRecordManager.updateRecordView(
            ScriptRecordViewManager.ViewState.default()
                .ofTrue(ScriptRecordViewManager.RecordViewType.LAYOUT_SIZE)
                .ofTrue(ScriptRecordViewManager.RecordViewType.TOUCHABLE)
        )
        ScriptRecordManager.getRecordView()?.post {
            ScriptRecordManager.getRecordView()?.setRect(cmd.limitRect)
        }
        ScriptRecordManager.setRecordResultListener { action, data ->
            if (data != null) {
                cmd.limitRect = data as RectF
            }
            ScriptRecordManager.restoreViewState()
            ScriptRecordManager.setRecordResultListener(null)
            BaseScriptDialog.restoreState()
            callback.invoke()
        }
    }

    private fun startHandleInsert(
        state: ScriptRecordViewManager.ViewState,
        listener: OnInsertListener? = null
    ) {
        if (ScriptManager.checkAccessibility()) return
        ScriptRecordHelper.instance.reset()
        ScriptInterpreter.getDefault().stopExecute()
        registerInsertListener(object : OnInsertListener {


            override fun onPickImage(image: String?) {
                listener?.onPickImage(image)
                ScriptRecordManager.hiddenRecordView()
                ScriptMenuManager.hiddenMenuView()
                ScriptInterpreter.getDefault().stopExecute()
            }

            override fun onInsertCommand(cmdInsert: ScriptCommand?) {
                listener?.onInsertCommand(cmdInsert)
                ScriptRecordManager.hiddenRecordView()
                ScriptMenuManager.hiddenMenuView()
                ScriptInterpreter.getDefault().stopExecute()
            }

            override fun onInsertDismiss() {
                listener?.onInsertDismiss()
                ScriptRecordManager.hiddenRecordView()
                ScriptMenuManager.hiddenMenuView()
                ScriptInterpreter.getDefault().stopExecute()
            }
        })
        ScriptMenuManager.switchMenuMode(ScriptControlView.MenuMode.INSERT_SINGLE_MENU)
        ScriptRecordManager.updateRecordView(
            state
        )
        ScriptManager.updateViewLayout()
    }


    private fun requestPermission(listener: OnInsertListener?, onRequestSuccess: () -> Unit) {
        val activity = ScriptProvider.getViewContext()
        if (ScriptScreenShotService.instance == null) {
            ActivityRequestPermissionCapture.checkOrRequestPermission(activity, true, {
                UIHandlerUtils.getInstance().postDelayed({
                    onRequestSuccess.invoke()
                }, 300)
            }, {
                listener?.onInsertDismiss()
                CommonToast.show(com.hive.i8n.R.string.sc_permission_snap_screen_failure)
            })
        } else {
            UIHandlerUtils.getInstance().postDelayed({
                onRequestSuccess.invoke()
            }, 300)
        }
    }


    interface OnInsertListener {

        fun onHandleInsertCommand(cmdInsert: ScriptCommand?) {
            cmdInsert?.startDelay = ScriptConst.Cmd_Delay_Default
            cmdInsert?.endDelay = ScriptConst.Cmd_Delay_Default
            onInsertCommand(cmdInsert)
        }

        fun onPickImage(image: String?) {}
        fun onInsertCommand(cmdInsert: ScriptCommand?) {}

        fun onInsertDismiss()
    }

    private var sOnInsertListener: OnInsertListener? = null

    fun registerInsertListener(listener: OnInsertListener) {
        sOnInsertListener = listener
    }

    fun notifyInsertCommand(cmdInsert: ScriptCommand?) {
        sOnInsertListener?.onHandleInsertCommand(cmdInsert)
        sOnInsertListener = null
    }

    fun notifyInsertImage(path: String?) {
        sOnInsertListener?.onPickImage(path)
        sOnInsertListener = null
    }

    /**
     * 步骤取消/关闭的统一入口。有插入监听时恢复工作流编辑界面，无监听时恢复录制菜单。
     */
    fun notifyInsertDismiss() {
        if (sOnInsertListener != null) {
            sOnInsertListener?.onInsertDismiss()
            sOnInsertListener = null
        } else {
            ScriptRecordManager.restoreRecordMenuState()
            ScriptMenuManager.restoreMenuState()
        }
    }

    fun cleanInsertListener() {
        sOnInsertListener = null
    }

}
