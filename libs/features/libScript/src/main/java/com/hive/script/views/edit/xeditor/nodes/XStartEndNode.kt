// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.edit.xeditor.nodes

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View
import com.hive.script.R
import com.hive.script.views.edit.ScriptMenuEditHelper
import com.hive.script.views.edit.xeditor.ScriptXEditorView
import com.hive.script.views.edit.xeditor.XCellModel
import com.hive.script.views.edit.xeditor.core.SCAbsLayerItemView
import com.hive.script.views.edit.xeditor.core.model.SCDrawEditRect
import com.hive.script.views.edit.xeditor.utils.XEditorHelper
import com.hive.utils.GlobalApp
import com.hive.views.popmenu.PopMenuManager

class XStartEndNode(context: Context, var type: NodeType, var cellBean: XCellModel) :
    XCommandNode(context, cellBean) {
    private val mCellPaint = Paint().apply {
        color = if (type == NodeType.START) {
            GlobalApp.getColor(com.hive.i8n.R.color.colorAccent)
        } else {
            GlobalApp.getColor(com.hive.i8n.R.color.colorRed)
        }
        alpha = 200
        strokeWidth = 4f
        isAntiAlias = true
    }


    private val innerCell = RectF(cell.frame).apply {
        inset(XEditorHelper.cellHevPadding, XEditorHelper.cellVerPadding)
    }
    private val mTextStartPaint = Paint().apply {
        color = GlobalApp.getColor(com.hive.i8n.R.color.textColorPrimary)
        textSize = 20f * GlobalApp.DP
        isAntiAlias = true
    }

    private val mTextEndPaint = Paint().apply {
        color = GlobalApp.getColor(com.hive.i8n.R.color.textColorPrimary)
        textSize = 20f * GlobalApp.DP
        isAntiAlias = true
    }


    private val startText = XEditorHelper.clipString(
        GlobalApp.getString(com.hive.i8n.R.string.sc_x_start_title),
        mTextStartPaint,
        innerCell.width() - 16 * GlobalApp.DP
    )

    private val endText = XEditorHelper.clipString(
        GlobalApp.getString(com.hive.i8n.R.string.sc_x_end_title),
        mTextEndPaint,
        innerCell.width() - 16 * GlobalApp.DP
    )

    private val startWidth = mTextStartPaint.measureText(startText)

    private val endWidth = mTextEndPaint.measureText(endText)


    override fun onDraw(canvas: Canvas) {
        if (isInTouchMove) {
            canvas.translate(ScriptXEditorView.touchX, ScriptXEditorView.touchY)
        }

        canvas.drawRoundRect(
            innerCell,
            innerCell.height() / 2f,
            innerCell.height() / 2f,
            mCellPaint
        )
        if (type == NodeType.START) {
            canvas.drawText(
                startText,
                cell.frame.centerX() - startWidth / 2f,
                cell.frame.centerY() + mTextStartPaint.textSize / 3,
                mTextStartPaint
            )
        } else {
            canvas.drawText(
                endText,
                cell.frame.centerX() - endWidth / 2f,
                cell.frame.centerY() + mTextEndPaint.textSize / 3,
                mTextEndPaint
            )
        }
    }

    override fun onDrawAnim(canvas: Canvas, animPercent: Float, typeAnim: String?) {
        onDraw(canvas)
    }

    override fun getRenderRect(): RectF {
        return cell.frame
    }

    override fun getMainCell() = cellBean


    enum class NodeType {
        START, END
    }

    companion object {
        fun createStartNode(context: Context, node: XCellModel): XStartEndNode {
            return XStartEndNode(context, NodeType.START, node)
        }

        fun createEndNode(context: Context, node: XCellModel): XStartEndNode {
            return XStartEndNode(context, NodeType.END, node)
        }
    }
}