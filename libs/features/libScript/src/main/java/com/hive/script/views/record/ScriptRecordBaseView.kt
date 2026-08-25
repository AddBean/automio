// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.record

import android.content.Context
import android.graphics.Point
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import com.hive.extension.isVisible
import com.hive.extension.visibleOrGone
import com.hive.script.R
import com.hive.script.views.manager.ScriptMenuManager
import com.hive.script.views.manager.ScriptRecordManager
import com.hive.utils.GlobalApp
import com.hive.utils.utils.DeviceCompatHelper
import com.hive.utils.utils.ScreenUtils
import com.hive.views.widgets.AbsFloatView

/**
 *
 * @author jiadou
 * @date 7/14/21
 */
abstract class ScriptRecordBaseView(context: Context, attributeSet: AttributeSet) :
    FrameLayout(context, attributeSet), IScriptRecordView {

    private var currentViewState: ScriptRecordViewManager.ViewState? = null

    protected var baseEventHandler: ScriptRecordEventHandler? = null

    protected var floatView: FloatView

    protected val dp = GlobalApp.DP

    protected var bottomView: FrameLayout = FrameLayout(context)

    protected var controlView: View? = null

    private var iv_close: View? = null
    private var llContent: ViewGroup? = null
    private var tv_msg: TextView? = null
    private var viewLine: View? = null

    init {
        floatView = FloatView(context)
        addView(bottomView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        addView(floatView, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT))
        iv_close = findViewById(R.id.iv_close)
        llContent = findViewById(R.id.llContent)
        tv_msg = findViewById(R.id.tv_msg)
        viewLine = findViewById(R.id.viewLine)
        tv_msg?.text = getLayoutName()
        baseEventHandler = getEventHandler()
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        controlView = getCtrView()
        controlView?.let { item ->
            viewLine?.visibleOrGone(true)
            llContent?.visibleOrGone(true)
            llContent?.addView(
                item
            )
        } ?: run {
            viewLine?.visibleOrGone(false)
            llContent?.visibleOrGone(false)
        }
    }

    override fun getViewState(): ScriptRecordViewManager.ViewState {
        return currentViewState ?: ScriptRecordViewManager.ViewState.default()
    }

    override fun setViewState(state: ScriptRecordViewManager.ViewState) {
        this.currentViewState = state
        getViewTypes().firstOrNull()?.let {
            val enable = state.isEnable(it)
            if (this.isVisible() != enable) {
                if (enable) {
                    onShow()
                } else {
                    onHidden()
                }
            }
        }
    }

    open fun getLayoutName() = ""

    open fun onShow() {
        visibility = VISIBLE
        ScriptRecordManager.hiddenRecordInnerView()
        ScriptMenuManager.hiddenMenuView()
        ScriptMenuManager.updateView(enableRecord = false)
    }

    open fun onHidden() {
        visibility = GONE
        ScriptRecordManager.showRecordInnerView()
        ScriptMenuManager.showMenuView()
        ScriptMenuManager.updateView(enableRecord = true)
    }

    open fun getCtrView(): View? = null

    inner class FloatView(context: Context) : AbsFloatView(context, null) {

        private var view =
            LayoutInflater.from(context).inflate(R.layout.sc_record_base_layout_ctr, this)

        init {
            view?.findViewById<View>(R.id.iv_close)?.setOnClickListener {
                baseEventHandler?.notifyEvent(
                    ScriptRecordEventHandler.RecordResultAction.ACTION_CANCEL,
                    null
                )
            }
        }

        override fun getStartPosition(pw: Int, ph: Int): Point {
            return Point(
                ScreenUtils.getScreenWidth() / 2 - this@FloatView.measuredWidth / 2,
                if (DeviceCompatHelper.isLandscape()) {
                    4 * DP
                } else {
                    60 * DP
                }
            )
        }
    }
}