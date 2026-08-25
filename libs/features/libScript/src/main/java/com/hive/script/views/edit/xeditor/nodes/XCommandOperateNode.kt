// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.edit.xeditor.nodes

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Point
import android.graphics.Rect
import android.graphics.RectF
import android.view.MotionEvent
import com.hive.script.R
import com.hive.script.views.edit.xeditor.ScriptXEditorView
import com.hive.script.views.edit.xeditor.XCellModel
import com.hive.script.views.edit.xeditor.XCellOperateModel
import com.hive.script.views.edit.xeditor.core.SCAbsLayerItemView
import com.hive.script.views.edit.xeditor.core.model.SCDrawEditRect
import com.hive.utils.GlobalApp
import com.hive.utils.utils.BitmapUtils

class XCommandOperateNode(
    context: Context,
    val cell: XCellModel,
    var operate: XCellOperateModel?,
    point: Point
) : SCAbsLayerItemView(context) {

    private val mIconPaint = Paint().apply {
        alpha = 200
        color = GlobalApp.getColor(com.hive.i8n.R.color.black)
    }

    private val point = Point(point.x, point.y)

    private val foldSize = 18 * GlobalApp.DP

    private val foldDesRect = RectF().apply {
        set(
            point.x - foldSize / 2f,
            point.y - foldSize / 2f,
            point.x + foldSize / 2f,
            point.y + foldSize / 2f
        )
    }

    private val foldSrcRect = Rect().apply {
        set(0, 0, foldIconBmp.width, foldIconBmp.height)
    }

    private val foldDesBmpRect = RectF(foldDesRect).apply {
        inset(5f * GlobalApp.DP, 5f * GlobalApp.DP)
    }

    override fun onMeasure(rect: SCDrawEditRect) {
        rect.set(foldDesRect)
        rect.inset(-10f * GlobalApp.DP, -10f * GlobalApp.DP)
    }

    override fun onDraw(canvas: Canvas) {
        if (cell.isInTouchMove) {
            canvas.translate(ScriptXEditorView.touchX, ScriptXEditorView.touchY)
        }
        if (isNormalSize()) {
            if (operate?.isFold == true) {
                canvas.drawOval(foldDesRect, mIconPaint)
                canvas.drawBitmap(
                    foldIconBmp, foldSrcRect, foldDesBmpRect, mIconPaint
                )
            } else {
                canvas.drawOval(foldDesRect, mIconPaint)
                canvas.drawBitmap(
                    unfoldIconBmp, foldSrcRect, foldDesBmpRect, mIconPaint
                )
            }
        }
    }

    override fun getMainCell() = cell

    override fun getRenderRect(): RectF {
        return cell.frame
    }

    override fun onClick(event: MotionEvent) {
        operate?.isFold = !operate?.isFold!!
        postEvent(ScriptXEditorView.XEditorEvent.OPERATE_CHANGE)
    }

    companion object {

        private val foldIconBmp = BitmapUtils.drawableToBitmap(R.drawable.sc_icon_fold)

        private val unfoldIconBmp = BitmapUtils.drawableToBitmap(R.drawable.sc_icon_unfold)

        /**
         * @param point 折叠按钮在 group 上的放置点，建议用 [XEditorUtils.getOperateNodeConnectPoint] 计算以与连接线对齐
         */
        fun create(
            context: Context,
            cell: XCellModel,
            operate: XCellOperateModel?,
            point: Point
        ): XCommandOperateNode {
            return XCommandOperateNode(context, cell, operate, point)
        }
    }
}