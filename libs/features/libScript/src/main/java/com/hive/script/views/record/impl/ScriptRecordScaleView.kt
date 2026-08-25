// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.record.impl

import android.content.Context
import android.content.res.Configuration
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import com.hive.script.R
import com.hive.script.base.ScriptConst
import com.hive.script.cmd.CmdPinchZoom
import com.hive.script.views.record.ScriptRecordBaseView
import com.hive.script.views.record.ScriptRecordEventHandler
import com.hive.script.views.record.ScriptRecordViewManager
import com.hive.script.views.record.handler.ScriptScaleHandler
import com.hive.script.views.widgets.ScriptScaleInOutView
import com.hive.utils.GlobalApp
import com.hive.utils.extends.color
import com.hive.utils.utils.ScreenUtils
import com.hive.views.widgets.SelectorTabView

/**
 *
 * @author jiadou
 * @date 7/16/21
 */
class ScriptRecordScaleView(context: Context, attributeSet: AttributeSet) :
    ScriptRecordBaseView(context, attributeSet) {

    private var mScriptScaleView = ScriptScaleInOutView(context, null).apply {
        bottomView.addView(
            this,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        )
    }

    private var ctlView = ControlView(context)

    override fun getCtrView(): View {
        return ctlView
    }

    override fun dispatchDraw(canvas: Canvas) {
        canvas.drawColor(com.hive.i8n.R.color.recordOptBg.color())
        super.dispatchDraw(canvas)
    }

    override fun onShow() {
        super.onShow()
        val sw = ScreenUtils.getScreenWidth()
        val sh = ScreenUtils.getScreenHeight()
        val cx = sw / 2
        val cy = sh / 2
        val x1 = cx - sw / 3
        val x2 = cx + sw / 3
        val y1 = cy
        val y2 = cy
        mScriptScaleView.loadCmd(
            CmdPinchZoom.createCommand(
                CmdPinchZoom.ACTION_SCALE_OUT,
                ScriptConst.Cmd_Click_Scale_Default,
                x1,
                y1,
                x2,
                y2,
                cx,
                cy
            )
        )
    }

    override fun getLayoutName(): String {
        return GlobalApp.getString(com.hive.i8n.R.string.sc_spot_layout_name_1)
    }

    override fun getViewTypes() = mutableListOf(
        ScriptRecordViewManager.RecordViewType.SCALE_IN_OUT
    )

    override fun getEventHandler() = ScriptScaleHandler(this)

    inner class ControlView(context: Context) : FrameLayout(context) {

        private var mTabSelector: SelectorTabView? = null

        private var mBtnSubmit: Button? = null

        val view = LayoutInflater.from(context)
            .inflate(R.layout.script_scale_inout_control_view, this@ControlView)

        fun loadCommand(cmd: CmdPinchZoom) {
            mTabSelector?.setValue(cmd.action)
        }

        override fun onConfigurationChanged(newConfig: Configuration?) {
            super.onConfigurationChanged(newConfig)
            post { adjustPosition() }

        }


        override fun onAttachedToWindow() {
            super.onAttachedToWindow()
            post { adjustPosition() }
        }

        private fun adjustPosition() {
            mTabSelector = view.findViewById(R.id.tab_selector)
            mBtnSubmit = view.findViewById(R.id.btn_submit)
            mBtnSubmit?.setOnClickListener {
                baseEventHandler?.notifyEvent(ScriptRecordEventHandler.RecordResultAction.ACTION_SCALE, mScriptScaleView.mCmdScale)
            }
            mTabSelector?.onTabSelectedChangedListener =
                object : SelectorTabView.OnTabSelectedChangedListener {
                    override fun onSelectedChanged(p: Pair<String?, String?>?) {
                        p?.second?.run {
                            mScriptScaleView.mCmdScale?.action = this
                            mScriptScaleView.invalidate()
                        }
                    }
                }
            mScriptScaleView.onCommandReadyListener =
                object : ScriptScaleInOutView.OnCommandReadyListener {
                    override fun onCommandReady(cmd: CmdPinchZoom) {
                        ctlView.loadCommand(cmd)
                    }
                }
        }
    }
}