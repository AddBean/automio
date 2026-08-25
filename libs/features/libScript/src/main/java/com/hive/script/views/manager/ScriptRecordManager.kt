// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.manager

import android.view.View
import com.hive.script.base.ScriptCommandRoot
import com.hive.script.base.ScriptRecordHelper
import com.hive.script.base.core.ScriptInterpreter
import com.hive.script.base.core.ScriptSaver
import com.hive.script.cmd.CmdActionWakeUp
import com.hive.script.driver.ScriptEventHelper
import com.hive.script.views.menu.ScriptControlView
import com.hive.script.views.record.ScriptRecordContainerView
import com.hive.script.views.record.ScriptRecordEventHandler
import com.hive.script.views.record.ScriptRecordViewManager
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

object ScriptRecordManager {

    private var viewStateSave: ScriptRecordViewManager.ViewState? = null

    private var currentViewState = ScriptRecordViewManager.ViewState.default()

    private var recordResultListener: ((action: ScriptRecordEventHandler.RecordResultAction, data: Any?) -> Unit)? =
        null

    var clickViewType = ScriptRecordManager.RecordClickViewType.CLICK_VIEW

    var dragViewType = ScriptRecordManager.RecordDragViewType.OFFSET

    var clickImageType = ScriptRecordManager.RecordClickImageType.DEFAULT

    enum class RecordClickViewType {
        SELECT_VIEW, SELECT_EDIT_VIEW, CLICK_VIEW, INPUT_VIEW, READ_VIEW_TEXT,
    }

    enum class RecordDragViewType {
        OFFSET, DRAG
    }

    enum class RecordClickImageType {
        DEFAULT, INSERT_IMAGE
    }

    fun ensureRecordViewAdded(): Boolean {
        try {
            if (!ScriptManager.checkServerEnable()) return false
            ScriptRecordContainerView.get()?.release()
            ScriptRecordContainerView.create()
            ScriptManager.getWindowManager()?.addView(
                ScriptRecordContainerView.get(),
                ScriptRecordContainerView.generateLayoutParams(true)
            )
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    fun isPanelShowing(): Boolean {
        return ScriptMenuManager.isMenuViewVisible()
    }

    fun setRecordResultListener(listener: ((action: ScriptRecordEventHandler.RecordResultAction, data: Any?) -> Unit)?) {
        recordResultListener = listener
    }

    fun notifyRecordResultListener(
        action: ScriptRecordEventHandler.RecordResultAction,
        data: Any?
    ) {
        recordResultListener?.invoke(action, data)
        recordResultListener = null
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun updateRecordView(
        viewState: ScriptRecordViewManager.ViewState = ScriptRecordViewManager.ViewState.default()
    ) {
        if (ScriptManager.checkAccessibility()) return
        GlobalScope.launch(Dispatchers.Main) {
            currentViewState = viewState.copy()
            showRecordView()
            ScriptRecordContainerView.get()?.getRecordView()?.setViewState(viewState)
            ScriptRecordContainerView.get()?.setViewState(viewState)
            ScriptMenuManager.updateView(
                enableRecord = (viewState.isEnable(ScriptRecordViewManager.RecordViewType.MENU)
                        && viewState.isEnable(ScriptRecordViewManager.RecordViewType.TOUCHABLE)
                        && !(viewState.isEnable(ScriptRecordViewManager.RecordViewType.CLICK_VIEW)
                        || viewState.isEnable(ScriptRecordViewManager.RecordViewType.CLICK_IMAGE)
                        || viewState.isEnable(ScriptRecordViewManager.RecordViewType.LAYOUT_SIZE)
                        || viewState.isEnable(ScriptRecordViewManager.RecordViewType.BATCH_CLICK)
                        || viewState.isEnable(ScriptRecordViewManager.RecordViewType.MULTIPLE)
                        || viewState.isEnable(ScriptRecordViewManager.RecordViewType.SCALE_IN_OUT)
                        || viewState.isEnable(ScriptRecordViewManager.RecordViewType.MATCH_DRAG)
                        ))
            )
        }
    }

    fun hiddenRecordView() {
        ScriptRecordContainerView.get()?.visibility = View.GONE
    }

    fun showRecordView() {
        ScriptRecordContainerView.get()?.visibility = View.VISIBLE
    }

    fun hiddenRecordInnerView() {
        getRecordInnerView()?.visibility = View.GONE
    }

    fun showRecordInnerView() {
        getRecordInnerView()?.visibility = View.VISIBLE
    }

    fun startRecord(
        listener: OnRecordEventListener? = null,
        menuType: ScriptControlView.MenuMode? = ScriptControlView.MenuMode.RECORD_MENU,
        isResume: Boolean = false
    ) {
        if (ScriptManager.checkAccessibility()) return
        if (!isResume) {
            ScriptRecordHelper.instance.reset()
        }
        val menuView = ScriptMenuManager.getMenuView()
        menuView?.onRecordEventListener = listener
        ScriptMenuManager.switchMenuMode(menuType ?: ScriptControlView.MenuMode.RECORD_MENU)
        updateRecordView(
            ScriptRecordViewManager.ViewState.default()
                .ofTrue(ScriptRecordViewManager.RecordViewType.TOUCHABLE)
                .ofTrue(ScriptRecordViewManager.RecordViewType.PREVIEW_DRAW)
        )
        showRecordView()
        showRecordInnerView()
        ScriptMenuManager.showMenuView()
        ScriptManager.updateViewLayout()
        getRecordInnerView()?.clearTrackView()
    }


    fun stopRecord(saveName: String? = null) {
        hiddenRecordView()
        ScriptInterpreter.getDefault().stopExecute()
        val menuView = ScriptMenuManager.getMenuView()
        menuView?.switchControlMode(ScriptControlView.MenuMode.MAIN_MENU)
        //如果存储名为空，则弹出保存对话框
        if (saveName == null) {
            ScriptManager.showSaveDialog(ScriptRecordHelper.instance.rootCommand, null, null)
        } else {
            ScriptSaver.saveToLocalWithLoading(
                saveName,
                ScriptRecordHelper.instance.rootCommand,
                null
            )
        }
    }

    fun pauseRecord() {
        hiddenRecordView()
    }

    fun getViewState(): ScriptRecordViewManager.ViewState {
        return currentViewState
    }

    fun saveViewState() {
        viewStateSave = currentViewState.copy()
    }

    fun restoreViewState() {
        viewStateSave?.copy()?.let {
            updateRecordView(it)
        }
    }

    /**
     * 恢复为默认录制界面状态（仅用于「录制菜单」场景下弹窗取消后恢复菜单展示）。
     * 编辑插入场景的取消由 ScriptInsertManager.notifyInsertCancel() 走 listener 恢复。
     */
    fun restoreRecordMenuState() {
        updateRecordView(
            ScriptRecordViewManager.ViewState.default()
                .ofTrue(ScriptRecordViewManager.RecordViewType.TOUCHABLE)
                .ofTrue(ScriptRecordViewManager.RecordViewType.PREVIEW_DRAW)
        )
    }

    fun getRecordView() = ScriptRecordContainerView.get()

    fun getRecordInnerView() = ScriptRecordContainerView.get()?.getRecordView()

    fun setRecordTouchable(touchable: Boolean) {
        ScriptRecordContainerView.currentTouchable = touchable
        ScriptManager.updateViewLayout()
    }

    fun startRecordUnlock() {
        if (ScriptManager.checkServerEnable()) {
            ScriptInterpreter.getDefault().stopExecute()
            ScriptEventHelper.get().performActionLockScreen()
            startRecord(
                isResume = true, menuType = ScriptControlView.MenuMode.RECORD_SCREEN_UNLOCK_MENU
            )
            ScriptRecordHelper.instance.reset()
            ScriptManager.addAndExecuteCommand(CmdActionWakeUp.createCommand())
        }
    }

    fun setRecordDragViewType(type: RecordDragViewType) {
        dragViewType = type
    }

    fun setRecordClickViewType(type: RecordClickViewType) {
        clickViewType = type
    }

    fun setRecordClickImageType(type: RecordClickImageType) {
        clickImageType = type
    }

    fun clearViewState() {
        currentViewState =
            ScriptRecordViewManager.ViewState.default()
                .ofFalse(ScriptRecordViewManager.RecordViewType.MENU)
    }

    interface OnRecordEventListener {
        fun onRecordFinished(script: ScriptCommandRoot): Boolean
    }

}
