// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.menu

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.graphics.Point
import android.graphics.Rect
import android.graphics.RectF
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import com.blankj.utilcode.util.VibrateUtils
import com.hive.extension.removeSelf
import com.hive.extension.visibleOrGone
import com.hive.script.R
import com.hive.script.ScriptProvider
import com.hive.script.base.ScriptCommand
import com.hive.script.base.ScriptConst
import com.hive.script.base.ScriptRecordHelper
import com.hive.script.base.core.ScriptInterpreter
import com.hive.script.base.core.ScriptInterpreterObserver
import com.hive.script.base.core.ScriptThreadManager
import com.hive.script.driver.ScriptEventHelper
import com.hive.script.setting.ScriptSetting
import com.hive.net.engineer.EngineerConfig
import com.hive.script.utils.ScriptCommonUtils
import com.hive.script.utils.ScriptHelper
import com.hive.script.views.dialog.DialogPlayStop
import com.hive.script.views.edit.DialogScriptEdit
import com.hive.script.views.event.ScriptMenuEvent
import com.hive.script.views.logger.ScriptLoggerView
import com.hive.script.views.manager.ScriptInsertManager
import com.hive.script.views.manager.ScriptManager
import com.hive.script.views.manager.ScriptMenuManager
import com.hive.script.views.manager.ScriptRecordManager
import com.hive.script.views.record.ScriptRecordViewManager
import com.hive.utils.debug.DLog
import com.hive.views.widgets.AbsWindowFloatView
import org.greenrobot.eventbus.EventBus

/**
 *
 * @author jiadou
 * @date 6/9/21
 */
class ScriptControlView(context: Context) : AbsWindowFloatView(context, null),
    ScriptInterpreterObserver.InterpreterExecuteObserver,
    ScriptInterpreterObserver.CommandExecuteObserver {

    var onRecordEventListener: ScriptRecordManager.OnRecordEventListener? = null

    private var logger_view: ScriptLoggerView? = null
    private var menu_main: ScriptControlMainView? = null
    private var menu_play: ScriptControlPlayView? = null
    private var menu_record: ScriptControlRecordView? = null
    private var menu_screen_unlock: ScriptControlUnlockView? = null

    enum class MenuMode {
        MAIN_MENU, RECORD_MENU, PLAYING_MENU, EDIT_MENU, RECORD_SCREEN_UNLOCK_MENU, INSERT_SINGLE_MENU
    }

    private var playBeginTime = 0L

    override var PADING_DISTANT = 0

    private var isShouldHiddenMenu = false

    private var mCurMenuMode: MenuMode = MenuMode.MAIN_MENU

    override var PADDING_BOTTOM = 0

    private var view = LayoutInflater.from(context).inflate(R.layout.script_control_menu_view, this)

    private var disableStopDialogOnce = false

    private fun isEngineerMode() = EngineerConfig.read().engineerOn

    private fun setLoggerVisibility(visible: Boolean) {
        logger_view?.visibleOrGone(isEngineerMode() && visible)
    }

    init {
        logger_view = findViewById(R.id.logger_view)
        if (!isEngineerMode()) logger_view?.visibility = View.GONE
        menu_main = findViewById(R.id.menu_main)
        menu_play = findViewById(R.id.menu_play)
        menu_record = findViewById(R.id.menu_record)
        menu_screen_unlock = findViewById(R.id.menu_screen_unlock)
        switchControlMode(mCurMenuMode)
        updateCurrentStatus()
        initEvent()
        initObserver()
    }

    private fun initEvent() {
        menu_main?.parentControl = this
        menu_play?.parentControl = this
        menu_record?.parentControl = this
        menu_record?.clickStopListener = {
            if (onRecordEventListener?.onRecordFinished(ScriptRecordHelper.instance.rootCommand) != true) {
                ScriptRecordManager.stopRecord()
                ScriptThreadManager.stopAll()
            }
            onRecordEventListener = null
            updateCurrentStatus()
        }
        menu_record?.menuStateListener = { isOpen ->
            setLoggerVisibility(!isOpen)
        }
        menu_record?.clickPlayListener = { isPause ->
            if (isPause) {
                ScriptRecordManager.pauseRecord()
            } else {
                ScriptRecordManager.startRecord(isResume = true)
            }
            updateCurrentStatus()
        }
        menu_main?.clickEditListener = {
            ScriptRecordHelper.instance.rootCommand.run {
                switchControlMode(MenuMode.EDIT_MENU)
                DialogScriptEdit.create(scriptMate)?.loadRoot(this)
                    ?.setFromSource(ScriptConst.From.FROM_SCRIPT_MENU_MAIN)
                    ?.show()
            }
        }
    }

    private fun initObserver() {
        ScriptInterpreterObserver.registerInterpreterObserver(this)
        ScriptInterpreterObserver.registerCommandObserver(this)
    }


    fun startPlaybackProgress() {
        menu_play?.startPlaybackProgress()
    }

    fun stopPlaybackProgress() {
        menu_play?.stopPlaybackProgress()
    }

    fun pausePlaybackProgress() {
        menu_play?.pausePlaybackProgress()
    }

    private var savedStateMenuMode: MenuMode? = null

    fun hasSavedStateMode(): Boolean {
        return savedStateMenuMode != null
    }

    fun saveMode() {
        savedStateMenuMode = mCurMenuMode
    }

    fun restoreMode() {
        savedStateMenuMode ?: return
        switchControlMode(savedStateMenuMode!!)
        savedStateMenuMode = null
    }

    fun switchControlMode(menuMode: MenuMode) {
        if (ScriptManager.checkAccessibility()) return
        mCurMenuMode = menuMode
        when (mCurMenuMode) {
            MenuMode.MAIN_MENU -> {
                setLoggerVisibility(false)
                menu_main?.visibility = View.VISIBLE
                menu_record?.visibility = View.GONE
                menu_play?.visibility = View.GONE
                menu_screen_unlock?.visibility = View.GONE
                menu_record?.stopRecord()
            }

            MenuMode.RECORD_MENU -> {
                setLoggerVisibility(!menu_record!!.expend)
                menu_main?.visibility = View.GONE
                menu_record?.visibility = View.VISIBLE
                menu_play?.visibility = View.GONE
                menu_screen_unlock?.visibility = View.GONE
                menu_record?.startRecord()
                ScriptRecordManager.updateRecordView(
                    ScriptRecordViewManager.ViewState.default()
                        .ofTrue(ScriptRecordViewManager.RecordViewType.TOUCHABLE)
                        .ofTrue(ScriptRecordViewManager.RecordViewType.PREVIEW_DRAW)
                )
            }

            MenuMode.RECORD_SCREEN_UNLOCK_MENU -> {
                setLoggerVisibility(false)
                menu_main?.visibility = View.GONE
                menu_record?.visibility = View.GONE
                menu_play?.visibility = View.GONE
                menu_screen_unlock?.visibility = View.VISIBLE
                menu_record?.startRecord()
                ScriptRecordManager.updateRecordView(
                    ScriptRecordViewManager.ViewState.default()
                        .ofTrue(ScriptRecordViewManager.RecordViewType.TOUCHABLE)
                        .ofTrue(ScriptRecordViewManager.RecordViewType.PREVIEW_DRAW)
                )
            }

            MenuMode.INSERT_SINGLE_MENU -> {
                setLoggerVisibility(false)
                menu_main?.visibility = View.GONE
                menu_record?.visibility = View.GONE
                menu_play?.visibility = View.GONE
                menu_screen_unlock?.visibility = View.GONE
                menu_record?.startRecord()
                ScriptRecordManager.updateRecordView(
                    ScriptRecordViewManager.ViewState.default()
                        .ofTrue(ScriptRecordViewManager.RecordViewType.TOUCHABLE)
                        .ofTrue(ScriptRecordViewManager.RecordViewType.PREVIEW_DRAW)
                )
            }

            MenuMode.PLAYING_MENU -> {
                setLoggerVisibility(true)
                menu_main?.visibility = View.GONE
                menu_record?.visibility = View.GONE
                menu_play?.visibleOrGone(ScriptSetting.script_setting_running_menu_on)
                menu_screen_unlock?.visibility = View.GONE
                menu_record?.stopRecord()
            }

            MenuMode.EDIT_MENU -> {
                setLoggerVisibility(false)
                menu_record?.stopRecord()
            }

        }

        if (mCurMenuMode != MenuMode.PLAYING_MENU) {
            ScriptManager.ensurePlayStop()
        }

        post {
            logger_view?.setCurrentMode(mCurMenuMode)
            updateCurrentStatus()
            EventBus.getDefault().post(ScriptMenuEvent())
        }
    }

    fun updateCurrentStatus() {
        menu_main?.updateCurrentStatus()
        menu_play?.updateCurrentStatus()
        backToEdge()
    }

    override fun onEdgeAnimationEnd() {
        super.onEdgeAnimationEnd()
        menu_main?.updateCurrentStatus()
    }

    override fun onCommandExecuteBefore(cmd: ScriptCommand) {
        //如果是播放模式，且不需要显示菜单
        if (mCurMenuMode == MenuMode.PLAYING_MENU
            && ScriptMenuManager.isMenuViewVisible()
            && !ScriptSetting.script_setting_running_menu_on
        ) {
            post {
                ScriptMenuManager.hiddenMenuView()
            }
        } else {
            isShouldHiddenMenu = checkIfNeedHiddenMenu(cmd)
            if (isShouldHiddenMenu) {
                ScriptMenuManager.saveMenuState()
                ScriptHelper.blockUntilViewReady(this) {
                    ScriptMenuManager.hiddenMenuView()
                }
                ScriptThreadManager.delay(200)
            }
        }
    }

    override fun onCommandExecuteWait(cmd: ScriptCommand, delay: Long) {
        if (isShouldHiddenMenu) {
            this.post {
                ScriptMenuManager.restoreMenuState()
            }
        }
    }

    override fun onCommandExecuteAfter(cmd: ScriptCommand) {
        post {
            ScriptInsertManager.notifyInsertCommand(cmd)
        }
    }

    /**
     * 检查是否需要隐藏菜单,根据当menu的大小和位置，是否和cmd的有效范围有交集
     */
    private fun checkIfNeedHiddenMenu(cmd: ScriptCommand): Boolean {
        if (isShown.not()) return false
        val locs = intArrayOf(0, 0)
        getLocationOnScreen(locs)
        val rect = Rect(locs[0], locs[1], locs[0] + measuredWidth, locs[1] + measuredHeight)
        val menuInScreen = ScriptCommonUtils.convertToNormalization(RectF(rect))
        val cmdInScreen = cmd.getNormalizedActiveArea()
        return cmdInScreen != null && cmdInScreen.intersect(menuInScreen)
    }


    override fun onInterpreterStart(cmd: ScriptCommand) {
        //仅播放模式
        if (!ScriptInterpreter.getDefault().isRecording()) {
            playBeginTime = System.currentTimeMillis()
            ScriptRecordManager.getRecordView()?.run {
                ScriptHelper.blockUntilViewReady(this) {
                    ScriptRecordManager.updateRecordView(
                        ScriptRecordViewManager.ViewState.default()
                            .ofTrue(ScriptRecordViewManager.RecordViewType.TOUCHABLE)
                            .ofFalse(ScriptRecordViewManager.RecordViewType.MENU)
                    )
                    startPlaybackProgress()
                    switchControlMode(MenuMode.PLAYING_MENU)
                }
            }
        }
    }

    override fun onInterpreterEnd(cmd: ScriptCommand) {
        DLog.d("ScriptControlView", "onInterpreterEnd() isRecording=${ScriptInterpreter.getDefault().isRecording()}, cmd=${cmd::class.simpleName}")
        //仅播放模式
        if (!ScriptInterpreter.getDefault().isRecording()) {
            ScriptRecordManager.getRecordView()?.run {
                ScriptHelper.blockUntilViewReady(this) {
                    DLog.d("ScriptControlView", "onInterpreterEnd() -> stopPlay()")
                    stopPlaybackProgress()
                    if (hasSavedStateMode()) {
                        restoreMode()
                    } else {
                        switchControlMode(MenuMode.MAIN_MENU)
                    }
                    ScriptManager.stopPlay()
                    ScriptRecordManager.hiddenRecordView()
                    VibrateUtils.vibrate(100L)
                    if (ScriptSetting.script_setting_running_tips_switch) {
                        if (!disableStopDialogOnce) {
                            disableStopDialogOnce = false
                            DialogPlayStop(context)
                                .loadCmd(cmd, System.currentTimeMillis() - playBeginTime).show()
                        }
                    }
                    if (ScriptSetting.script_setting_lock_switch) {
                        ScriptEventHelper.get().performActionLockScreen()
                    }
                }
            }
        }
    }

    fun disableStopDialogOnce() {
        disableStopDialogOnce = true
    }

    fun resetStopDialogOnce() {
        disableStopDialogOnce = false
    }

    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)
        backToEdge()
    }

    override fun backToEdge() {
        mViewHeight = measuredHeight
        mViewWidth = measuredWidth
        super.backToEdge()
    }

    override fun backToEdge(toLeft: Boolean) {
        post {
            mViewHeight = measuredHeight
            mViewWidth = measuredWidth
            super.backToEdge(toLeft)
        }
    }

    override fun getStartPosition(pw: Int, ph: Int) = Point(pw - mViewWidth, ph / 2 - mViewHeight / 2)

    fun stopRecord() {
        menu_record?.pauseRecord()
    }

    fun startRecord() {
        menu_record?.startRecord()
    }

    fun pauseRecord() {
        menu_record?.pauseRecord()
    }

    fun getLoggerView() = logger_view

    fun release() {
        removeSelf()
        instance = null
    }


    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP || event.action == MotionEvent.ACTION_CANCEL) {
            menu_main?.startAutoHidden()
        } else {
            menu_main?.cancelAutoHidden()
        }
        return super.dispatchTouchEvent(event)
    }

    companion object {


        @SuppressLint("StaticFieldLeak")
        private var instance: ScriptControlView? = null

        fun get(): ScriptControlView? {
            return instance
        }

        fun create(): ScriptControlView {
            instance = ScriptControlView(ScriptProvider.getViewContext())
            return instance!!
        }
    }

}
