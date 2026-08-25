// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.widgets

import android.content.Context
import android.view.View
import com.hive.script.driver.ScriptEventHelper
import com.hive.utils.GlobalApp
import com.hive.utils.utils.ViewUtils
import com.hive.views.widgets.AbsWindowFloatView

abstract class ScriptMaxMinDialog(context: Context?) : BaseScriptDialog(context) {

    private var miniView: AbsWindowFloatView? = null

    override fun initView(view: View?) {
        super.initView(view)
        post {
            miniView = getMiniEditView()
            ViewUtils.measureView(miniView)
            miniView?.setOnClickListener {
                recoverView()
            }
        }
    }

    abstract fun getMiniEditView(): AbsWindowFloatView

    protected fun miniView(view: View) {
        ensureSingleMiniView()
        if (miniView is IScriptMiniView) {
            (miniView as IScriptMiniView).setViewSnapshot(view)
        }
        miniView?.addToWindow()
        currentMiniViewList.add(miniView!!)
        miniView?.post {
            hidden()
        }
        if (GlobalApp.isAppInForeground()) {
            ScriptEventHelper.get().performActionHome()
        }
        onMiniView()
    }

    private fun ensureSingleMiniView() {
        currentMiniViewList.forEach {
            it.removeToWindow()
        }
    }

    open fun onMiniView() {

    }


    open fun onMaxView() {

    }

    private fun recoverView() {
        miniView?.removeToWindow()
        currentMiniViewList.remove(miniView)
        restore()
        onConfigurationChanged(null)
        onMaxView()
    }

    interface IScriptMiniView {
        fun setViewSnapshot(view: View?)
    }

    companion object {
        private val currentMiniViewList = mutableListOf<AbsWindowFloatView>()
    }
}