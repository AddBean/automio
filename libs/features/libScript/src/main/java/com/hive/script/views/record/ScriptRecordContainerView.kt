// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.record

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.graphics.RectF
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import com.hive.base.BaseLayout
import com.hive.extension.removeSelf
import com.hive.extension.visibleOrGone
import com.hive.script.R
import com.hive.script.ScriptProvider
import com.hive.script.base.ScriptCommand
import com.hive.script.base.core.ScriptInterpreterObserver
import com.hive.script.utils.ScriptCoordinateAdapter
import com.hive.script.utils.ScriptHelper
import com.hive.script.views.record.impl.ScriptRecordSetSizeView
import com.hive.script.views.record.impl.ScriptRecordView

/**
 *
 * @author jiadou
 * @email 172111432@qq.com
 * @date 6/9/21
 */
@Suppress("UNCHECKED_CAST")
class ScriptRecordContainerView(context: Context?) : BaseLayout(context),
    ScriptInterpreterObserver.InterpreterExecuteObserver,
    ScriptInterpreterObserver.CommandExecuteObserver {
    private var viewManager = ScriptRecordViewManager(this)

    private var layout_size: ScriptRecordSetSizeView? = null
    private var layout_wrapper: View? = null
    private var record_view: ScriptRecordView? = null

    override fun initView(view: View?) {
        ScriptInterpreterObserver.registerCommandObserver(this)
        layout_size = findViewById(R.id.layout_size)
        layout_wrapper = findViewById(R.id.layout_wrapper)
        record_view = findViewById(R.id.record_view)
        post {
            viewManager.registerAllRecordView()
        }
    }

    fun setViewState(viewState: ScriptRecordViewManager.ViewState) {
        viewManager.setViewState(viewState)
    }

    fun setRect(rect: RectF) {
        layout_size?.setNormalizedRect(rect)
    }


    fun getRecordView(): ScriptRecordView = record_view!!

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        ScriptInterpreterObserver.registerInterpreterObserver(this)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        ScriptInterpreterObserver.unRegisterInterpreterObserver(this)
    }

    override fun onCommandExecuteBefore(cmd: ScriptCommand) {
    }

    override fun onCommandExecuteAfter(cmd: ScriptCommand) {
    }

    override fun onInterpreterStart(cmd: ScriptCommand) {
        ScriptHelper.blockUntilViewReady(layout_wrapper!!) {
            layout_wrapper?.visibility = GONE
        }
    }

    override fun onInterpreterEnd(cmd: ScriptCommand) {
        ScriptHelper.blockUntilViewReady(layout_wrapper!!) {
            layout_wrapper?.visibility = VISIBLE
        }
    }

    fun getRealMarginTop(): Int? {
        if (this.parent != null) {
            val p = intArrayOf(0, 0)
            this.getLocationOnScreen(p)
            val top = -(this.layoutParams as WindowManager.LayoutParams?)!!.y
            if (p[1] == 0) return top
            return p[1] + top
        }
        return null
    }

    fun getRealMarginLeft(): Int? {
        if (this.parent != null) {
            val p = intArrayOf(0, 0)
            this.getLocationOnScreen(p)
            val left = -(this.layoutParams as WindowManager.LayoutParams?)!!.x
            if (p[0] == 0) return left
            return p[0] + left
        }
        return null;
    }

    private var recordViewVisibleState = false
    fun saveRecordViewState() {
        recordViewVisibleState = record_view?.visibility == VISIBLE
    }

    fun restoreRecordViewState() {
        record_view?.visibleOrGone(recordViewVisibleState)
    }

    override fun getLayoutId() = R.layout.script_main_view
    fun release() {
        removeSelf()
        instance = null
    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        private var instance: ScriptRecordContainerView? = null
        var currentTouchable = true
        fun get(): ScriptRecordContainerView? {
            return instance
        }

        fun create(): ScriptRecordContainerView {
            instance = ScriptRecordContainerView(ScriptProvider.getViewContext())
            return instance!!
        }

        fun generateLayoutParams(touchable: Boolean = currentTouchable) =
            WindowManager.LayoutParams().also { lp ->
                currentTouchable = touchable
                lp.width = ScriptCoordinateAdapter.getScreenWidthByOrientation()
                lp.height = ScriptCoordinateAdapter.getScreenHeightByOrientation()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                    lp.type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
                } else {
                    lp.type = WindowManager.LayoutParams.TYPE_TOAST
                }
                lp.flags = if (touchable) {
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                } else {
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                }
                lp.gravity = Gravity.TOP or Gravity.START
                val top = get()?.getRealMarginTop()
                val left = get()?.getRealMarginLeft()
                top?.run {
                    lp.y = -this
                }
                left?.run {
                    lp.x = -this
                }
                lp.format = PixelFormat.RGBA_8888
            }
    }
}