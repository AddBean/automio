// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.record.impl

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.text.TextPaint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.hive.script.R
import com.hive.script.driver.ScriptEventHelper
import com.hive.script.views.manager.ScriptRecordManager
import com.hive.script.views.record.ScriptRecordBaseView
import com.hive.script.views.record.ScriptRecordEventHandler
import com.hive.script.views.record.ScriptRecordViewManager
import com.hive.script.views.record.handler.ScriptClickViewHandler
import com.hive.utils.GlobalApp
import com.hive.views.widgets.CommonToast

/**
 *
 * @author jiadou
 * @date 7/6/21
 */
class ScriptRecordClickViewView(context: Context, attributeSet: AttributeSet) :
    ScriptRecordBaseView(context, attributeSet) {

    private var mNodeList: MutableList<AccessibilityNodeInfo>? = null

    private var mTempRect = Rect()

    private var mNodeTextPaint = TextPaint().apply {
        isAntiAlias = true
        color = Color.BLUE
    }

    private var mNodePaint = Paint().apply {
        isAntiAlias = true
        color = Color.RED
        strokeWidth = 2f * dp
        style = Paint.Style.STROKE
    }

    private var mNodeBgPaint = Paint().apply {
        isAntiAlias = true
        color = Color.BLUE
        alpha = 20
        style = Paint.Style.FILL
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(e: MotionEvent?): Boolean {
        when (e?.action) {
            MotionEvent.ACTION_DOWN -> {
                val rect = Rect()
                val targetNodes = mutableListOf<AccessibilityNodeInfo>()
                mNodeList?.forEach {
                    it.getBoundsInScreen(rect)
                    if (rect.contains(e.x.toInt(), e.y.toInt())) {
                        targetNodes.add(it)
                    }
                }

                val node = targetNodes.minByOrNull {
                    it.getBoundsInScreen(rect)
                    rect.width() * rect.height()
                }
                targetNodes.sortBy {
                    it.getBoundsInScreen(rect)
                    rect.width() * rect.height()
                }
                node?.run {
                    baseEventHandler?.notifyEvent(
                        ScriptRecordEventHandler.RecordResultAction.ACTION_CLICK_VIEW,
                        this to targetNodes
                    )
                }
            }
        }

        return true
    }

    override fun dispatchDraw(canvas: Canvas) {
        drawBackground(canvas)
        canvas.run {
            drawNodeInfo(canvas)
        }
        super.dispatchDraw(canvas)
    }

    private fun drawBackground(canvas: Canvas?) {
        canvas?.drawARGB(180, 0, 0, 0)
    }

    private fun drawNodeInfo(canvas: Canvas) {
        mNodeList?.let { node ->

            node.forEach {
                it.getBoundsInScreen(mTempRect)
                canvas.drawRect(mTempRect, mNodePaint)
                canvas.drawRect(mTempRect, mNodeBgPaint)
//                mNodeTextPaint.textSize = rect.width() / 12f
//                var curY = 0
//                if (!TextUtils.isEmpty(it.text)) {
//                    canvas.drawText(it.text.toString(), rect.left.toFloat(), curY + rect.top.toFloat() + mNodeTextPaint.textSize, mNodeTextPaint)
//                    curY = (curY + mNodeTextPaint.textSize + dp).toInt()
//                }
//                if (!TextUtils.isEmpty(it.viewIdResourceName)) {
//                    canvas.drawText(it.viewIdResourceName.toString(), rect.left.toFloat(), curY + rect.top.toFloat() + mNodeTextPaint.textSize, mNodeTextPaint)
//                    curY = (curY + mNodeTextPaint.textSize + dp).toInt()
//                }
//                if (!TextUtils.isEmpty(it.contentDescription)) {
//                    canvas.drawText(it.contentDescription.toString(), rect.left.toFloat(), curY + rect.top.toFloat() + mNodeTextPaint.textSize, mNodeTextPaint)
//                }
            }
        }
    }


    override fun getViewTypes() = mutableListOf(
        ScriptRecordViewManager.RecordViewType.CLICK_VIEW
    )

    override fun getEventHandler() = ScriptClickViewHandler(this)

    override fun onShow() {
        super.onShow()
        mNodeList =
            ScriptEventHelper.get().getCurrentClickNodeList(ScriptRecordManager.clickViewType)
        if (mNodeList.isNullOrEmpty()) {
            CommonToast.getInstance().showToast(com.hive.i8n.R.string.sc_record_click_none_view)
            baseEventHandler?.notifyEvent(
                ScriptRecordEventHandler.RecordResultAction.ACTION_CLICK_VIEW,
                null
            )
        }
    }

    override fun onHidden() {
        mNodeList = null
        super.onHidden()
    }

    override fun getLayoutName(): String {
        return GlobalApp.getString(com.hive.i8n.R.string.sc_spot_layout_name_4)
    }

}