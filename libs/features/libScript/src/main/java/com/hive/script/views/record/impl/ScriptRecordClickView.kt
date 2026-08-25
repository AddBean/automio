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
import com.hive.script.views.record.ScriptRecordBaseView
import com.hive.script.views.record.ScriptRecordEventHandler
import com.hive.script.views.record.ScriptRecordViewManager
import com.hive.script.views.record.handler.ScriptClickHandler
import com.hive.script.views.widgets.ScriptClickView
import com.hive.utils.GlobalApp
import com.hive.utils.extends.color

/**
 *
 * @author jiadou
 * @date 7/16/21
 */
class ScriptRecordClickView(context: Context, attributeSet: AttributeSet) :
    ScriptRecordBaseView(context, attributeSet) {

    private var clickView = ScriptClickView(context, null).apply {
        bottomView.addView(
            this,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        )
    }

    override fun dispatchDraw(canvas: Canvas) {
        canvas.drawColor(com.hive.i8n.R.color.recordOptBg.color())
        super.dispatchDraw(canvas)
    }

    override fun onShow() {
        super.onShow()
        clickView.updateData()
    }

    override fun getLayoutName() = GlobalApp.getString(com.hive.i8n.R.string.sc_click_view_name)

    override fun getViewTypes() = mutableListOf(
        ScriptRecordViewManager.RecordViewType.FAST_CLICK
    )

    override fun getEventHandler() = ScriptClickHandler(this)

    private var ctlView = ControlView(context)

    override fun getCtrView(): View {
        return ctlView
    }


    inner class ControlView(context: Context) : FrameLayout(context) {

        private var btnSubmit: Button? = null

        private val view = LayoutInflater.from(context)
            .inflate(R.layout.script_drag_control_view, this@ControlView)

        override fun onConfigurationChanged(newConfig: Configuration?) {
            super.onConfigurationChanged(newConfig)
            post {
                adjustPosition()
            }
        }

        override fun onAttachedToWindow() {
            super.onAttachedToWindow()
            post { adjustPosition() }
        }

        private fun adjustPosition() {
            btnSubmit = view.findViewById(R.id.btn_submit)
            btnSubmit?.setOnClickListener {
                baseEventHandler?.notifyEvent(
                    ScriptRecordEventHandler.RecordResultAction.ACTION_DRAG,
                    clickView.getFinalPoint()
                )
            }
        }
    }
}