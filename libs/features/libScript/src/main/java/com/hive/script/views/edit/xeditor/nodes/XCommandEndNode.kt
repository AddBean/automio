// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.edit.xeditor.nodes

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.Rect
import android.graphics.RectF
import android.view.MotionEvent
import com.hive.script.R
import com.hive.script.views.edit.ScriptMenuEditHelper
import com.hive.script.views.edit.xeditor.ScriptXEditorView
import com.hive.script.views.edit.xeditor.XCellModel
import com.hive.script.views.edit.xeditor.core.SCAbsLayerItemView
import com.hive.script.views.edit.xeditor.core.model.SCDrawEditRect
import com.hive.script.views.edit.xeditor.utils.XEditorHelper
import com.hive.utils.GlobalApp
import com.hive.utils.utils.BitmapUtils

class XCommandEndNode(context: Context, var cell: XCellModel) :
    SCAbsLayerItemView(context) {

    private val mCellPaint = Paint().apply {
        color = GlobalApp.getColor(com.hive.i8n.R.color.colorPrimary)
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val mCellPaintSelected = Paint().apply {
        color = GlobalApp.getColor(com.hive.i8n.R.color.colorGreen)
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val mIconPaintSelected = Paint().apply {
        val filter =
            PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);
        colorFilter = filter;
        alpha = 155
        isAntiAlias = true
    }

    var drawCell = XCellModel().apply {
        xIndex = cell.xIndex
        prevCell = cell
        yIndex = cell.yIndex + 1
        frame = RectF(
            xIndex * XEditorHelper.cellWidth,
            yIndex * XEditorHelper.cellHeight,
            (xIndex + 1) * XEditorHelper.cellWidth,
            (yIndex + 1) * XEditorHelper.cellHeight
        )
        frame.inset(
            (frame.width() - XEditorHelper.addIconSize) / 2,
            (frame.height() - XEditorHelper.addIconSize) / 2
        )
        frame.offset(
            0f,
            -XEditorHelper.cellHeight / 2f
        )
    }

    private val addSrcRect = Rect().apply {
        set(0, 0, addIconBmp.width, addIconBmp.height)
    }

    override fun onDraw(canvas: Canvas) {
        //移动时不画
        if (cell.isInTouchMove) {
            canvas.translate(ScriptXEditorView.touchX, ScriptXEditorView.touchY)
            return
        }

        if (isNormalSize()) {
            if (isInTouchSelected) {
                mCellPaint.alpha = 255
                drawCell.frame.offset(0f, 1.5f * dp)
                canvas.drawOval(drawCell.frame, mCellPaintSelected)
                drawCell.frame.offset(0f, -1.5f * dp)
                mCellPaint.alpha = 0x6f
                canvas.drawBitmap(
                    addIconBmp, addSrcRect, drawCell.frame, mIconPaintSelected
                )
            } else {
                mCellPaint.alpha = 255
                canvas.drawOval(drawCell.frame, mCellPaint)
                mCellPaint.alpha = 0x6f
                canvas.drawBitmap(
                    addIconBmp, addSrcRect, drawCell.frame, mCellPaint
                )
            }
        }
    }

    override fun getRenderRect(): RectF {
        return cell.frame
    }

    override fun getMainCell() = cell

    override fun onClick(event: MotionEvent) {
        super.onClick(event)
        postEvent(ScriptMenuEditHelper.ClickType.INSERT_RECORD)
    }

    override fun onMeasure(rect: SCDrawEditRect) {
        rect.lb = PointF(drawCell.frame.left, drawCell.frame.bottom)
        rect.lt = PointF(drawCell.frame.left, drawCell.frame.top)
        rect.rb = PointF(drawCell.frame.right, drawCell.frame.bottom)
        rect.rt = PointF(drawCell.frame.right, drawCell.frame.top)
    }

    companion object {

        private val addIconBmp = BitmapUtils.drawableToBitmap(R.drawable.sc_icon_add)

        fun createNode(context: Context, node: XCellModel): XCommandEndNode {
            return XCommandEndNode(context, node)
        }
    }
}