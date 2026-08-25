// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.record.impl

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import com.hive.extension.visibleOrGone
import com.hive.script.R
import com.hive.script.ScriptScreenShotService
import com.hive.script.views.dialog.DialogColorPicker
import com.hive.script.views.manager.ScriptRecordManager
import com.hive.script.views.record.ScriptRecordBaseView
import com.hive.script.views.record.ScriptRecordEventHandler
import com.hive.script.views.record.ScriptRecordViewManager
import com.hive.script.views.record.handler.ScriptClickColorHandler
import com.hive.script.views.widgets.ColorPreviewCircleView
import com.hive.utils.GlobalApp
import com.hive.utils.utils.ColorUtils

/**
 *
 * @author jiadou
 * @email 172111432@qq.com
 * @date 6/29/21
 */
class ScriptRecordClickColorView(context: Context, attributeSet: AttributeSet) :
    ScriptRecordBaseView(context, attributeSet) {

    private var bitmap: Bitmap? = null

    private var rect = Rect()

    private var mCircleView = ColorPreviewCircleView(context).apply {
        this.visibility = GONE
        this@ScriptRecordClickColorView.addView(this)
    }

    private var mScriptControlView = ControlView(context).apply {
        this.visibility = GONE
    }

    override fun getCtrView(): View {
        return mScriptControlView
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        when (event?.action) {
            MotionEvent.ACTION_DOWN -> {
                mCircleView.visibility = VISIBLE
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                mCircleView.visibility = GONE
            }
        }
        mCircleView.post {
            val color = bitmap?.getPixel(event!!.x.toInt(), event!!.y.toInt()) ?: Color.WHITE
            mCircleView.translationX = (event?.x ?: 0f) - mCircleView.width / 2
            mCircleView.translationY = (event?.y ?: 0f) - mCircleView.height / 2
            mCircleView.color = color
            mScriptControlView.updateColor(color)
        }
        return true
    }

    override fun dispatchDraw(canvas: Canvas) {
        bitmap?.run {
            canvas.getClipBounds(rect)
            canvas.drawBitmap(this, 0f, 0f, null)
        }
        super.dispatchDraw(canvas)
    }

    override fun onShow() {
        super.onShow()
        floatView.visibleOrGone(false)
        mScriptControlView.visibleOrGone(false)
        ScriptRecordManager.getRecordInnerView()?.postDelayed({
            bitmap = ScriptScreenShotService.instance?.getScreenShot()
            floatView.visibleOrGone(true)
            mScriptControlView.visibleOrGone(true)
            postInvalidate()
        }, 300)
    }

    override fun onHidden() {
        super.onHidden()
        bitmap = null
    }

    override fun getLayoutName(): String {
        return GlobalApp.getString(com.hive.i8n.R.string.sc_spot_layout_name_2)
    }

    override fun getViewTypes() = mutableListOf(
        ScriptRecordViewManager.RecordViewType.CLICK_COLOR
    )

    override fun getEventHandler() = ScriptClickColorHandler(this)


    inner class ControlView(context: Context) : FrameLayout(context) {

        private var mCmdClickColor: Int = Color.RED

        private var mColorView: View? = null

        private var mBtnSubmit: Button? = null

        private var mTextColor: TextView? = null

        private val view: View = LayoutInflater.from(context)
            .inflate(R.layout.script_scale_color_control_view, this@ControlView)

        fun updateColor(color: Int) {
            mCmdClickColor = color
            mColorView?.setBackgroundColor(color)
            mTextColor?.text = ColorUtils.toHexColor(color)
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
            mColorView = view.findViewById(R.id.view_color)
            mBtnSubmit = view.findViewById(R.id.btn_submit)
            mTextColor = view.findViewById(R.id.text_color)
            mColorView?.setOnClickListener {
                DialogColorPicker(context).loadColor(mCircleView.getCurrentSelectedColor())
                    .setOnColorPickListener(object : DialogColorPicker.OnColorPickListener {
                        override fun onColorPicked(dialog: DialogColorPicker, color: Int) {
                            dialog.dismiss()
                            updateColor(color)
                        }
                    }).show()
            }
            mBtnSubmit?.setOnClickListener {
                baseEventHandler?.notifyEvent(
                    ScriptRecordEventHandler.RecordResultAction.ACTION_CLICK_COLOR,
                    mCircleView.getCurrentSelectedColor()
                )
            }
        }
    }
}