// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.manager

import android.view.View
import com.hive.extension.visibleOrGone
import com.hive.script.setting.ScriptSetting
import com.hive.script.utils.ScriptHelper
import com.hive.script.views.menu.ScriptControlView
import com.hive.script.views.record.ScriptRecordContainerView

object ScriptMenuManager {

    private var viewStateMenuVisible = View.GONE

    fun ensureMenuViewAdded(): Boolean {
        if (!ScriptManager.checkServerEnable()) return false

        ScriptControlView.get()?.release()
        ScriptControlView.create()
        ScriptControlView.get()?.addToWindow()
        return true
    }

    fun updateView(enableRecord: Boolean = true) {
        ScriptRecordContainerView.get()?.getRecordView()?.run {
            ScriptHelper.runInMain {
                ScriptRecordContainerView.get()?.getRecordView()
                    ?.visibleOrGone(enableRecord || ScriptSetting.script_setting_show_tracks)
            }
        }
    }

    fun hiddenMenuView() {
        ScriptControlView.get()?.visibility = View.GONE
    }

    fun showMenuView() {
        ScriptControlView.get()?.visibility = View.VISIBLE
    }

    fun isMenuViewVisible(): Boolean {
        return ScriptControlView.get()?.visibility == View.VISIBLE
    }


    fun saveMenuState() {
        viewStateMenuVisible = ScriptControlView.get()?.visibility ?: View.GONE
    }

    fun restoreMenuState() {
        ScriptControlView.get()?.visibility = viewStateMenuVisible
    }

    fun disableStopDialogOnce() {
        getMenuView()?.disableStopDialogOnce()
    }

    fun resetStopDialogOnce() {
        getMenuView()?.resetStopDialogOnce()
    }

    fun switchMenuMode(menuMode: ScriptControlView.MenuMode) {
        val menuView = getMenuView()
        menuView?.switchControlMode(menuMode)
    }

    fun getMenuView(): ScriptControlView? {
        if (ScriptControlView.get() == null) {
            ensureMenuViewAdded()
        }
        return ScriptControlView.get()
    }

    fun getLoggerView() = ScriptControlView.get()?.getLoggerView()
}