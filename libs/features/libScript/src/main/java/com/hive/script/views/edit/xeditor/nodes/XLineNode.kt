// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.edit.xeditor.nodes

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Point
import android.graphics.PointF
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.Rect
import android.graphics.RectF
import android.text.TextPaint
import android.text.TextUtils
import android.view.MotionEvent
import com.hive.script.R
import com.hive.script.views.edit.ScriptMenuEditHelper
import com.hive.script.views.edit.xeditor.ScriptXEditorView
import com.hive.script.views.edit.xeditor.XCellModel
import com.hive.script.views.edit.xeditor.core.SCAbsLayerItemView
import com.hive.script.views.edit.xeditor.core.model.SCDrawEditRect
import com.hive.script.views.edit.xeditor.utils.XEditorHelper
import com.hive.script.views.edit.xeditor.utils.XEditorUtils
import com.hive.utils.GlobalApp
import com.hive.utils.utils.BitmapUtils


class XLineNode private constructor(
    context: Context, val lineType: LineType, var startCell: XCellModel, var endCell: XCellModel
) : SCAbsLayerItemView(context) {

    var supportAdd: Boolean = true

    var supportStartArr: Boolean = false

    var supportEndArr: Boolean = true

    var shouldMarginStartByArrIcon = false

    var shouldMarginEndByArrIcon = true

    var shouldMarginStartByAddIcon = false

    var shouldMarginEndByAddIcon = false

    var shouldHalf = false

    var labelText: String? = null

    var isDotted = false

    private val labelRectF = RectF()

    private val textPoint = PointF()

    private val arrowIconSize = XEditorHelper.arrIconSize

    var startPoint = Point()

    var endPoint = Point()

    var startPos: Int = 0

    var endPos: Int = 0

    private val mLinePaint = Paint().apply {
        color = 0x6fffffff
        strokeWidth = 1f * GlobalApp.DP
        style = Paint.Style.STROKE
        isAntiAlias = true
    }

    private val mArrPaint = Paint().apply {
        color = 0x6fffffff.toInt()
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val mBackPaint = Paint().apply {
        color = GlobalApp.getColor(com.hive.i8n.R.color.colorPrimary)
        isAntiAlias = true
    }

    private val mTextPaint = TextPaint().apply {
        color = 0x6fffffff.toInt()
        textSize = 10f * GlobalApp.DP
        isAntiAlias = true
    }

    private val mTextBgPaint = TextPaint().apply {
        color = 0xff000000.toInt()
        textSize = 10f * GlobalApp.DP
        isAntiAlias = true
    }

    private val mBackPaintSelected = Paint().apply {
        color = GlobalApp.getColor(com.hive.i8n.R.color.colorGreen)
    }

    private val mIconPaint = Paint().apply {
        alpha = 120
        isAntiAlias = true
    }

    private val mIconPaintSelected = Paint().apply {
        val filter = PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);
        colorFilter = filter
        alpha = 255
        isAntiAlias = true
    }

    private var addSize = XEditorHelper.addIconSize

    private var addSizeSelected = XEditorHelper.addIconSelectedSize

    private var addDesRect = RectF()

    private var addDesRectSelected = RectF()

    private var addSrcRect = Rect().apply {
        set(0, 0, addIconBmp.width, addIconBmp.height)
    }

    private var endArrowPath = XEditorUtils.getEndArrPath(endCell, endPos, arrowIconSize.toInt())

    private var startArrowPath =
        XEditorUtils.getEndArrPath(startCell, startPos, arrowIconSize.toInt())

    private var linePath = Path()


    init {
        updatePosition()
        updateLine()
    }

    private fun updatePosition() {
        updateEndPosition()
        updateStartPosition()
    }

    private fun updateEndPosition() {
        endPos = XEditorUtils.findPositionInCell(
            startCell, endCell
        )
    }

    private fun updateStartPosition() {
        startPos = XEditorUtils.findPositionInCell(
            endCell, startCell
        )
    }

    fun updateLine() {
        if (isDotted) {
            mLinePaint.pathEffect = XEditorUtils.getDottedPathEffect()
        } else {
            mLinePaint.pathEffect = null
        }
        startPoint = XEditorUtils.findTargetConnectPoint(
            endCell, startCell, startPos
        )
        endPoint = XEditorUtils.findTargetConnectPoint(
            startCell, endCell, endPos
        )
        val sp = XEditorUtils.findTargetConnectPoint(
            endCell, startCell, startPos, getStartMargin()
        )

        val ep = XEditorUtils.findTargetConnectPoint(
            startCell, endCell, endPos, getEndMargin()
        )
        addDesRect.set(
            (sp.x + ep.x) / 2 - addSize / 2f,
            (sp.y + ep.y) / 2 - addSize / 2f,
            (sp.x + ep.x) / 2 + addSize / 2f,
            (sp.y + ep.y) / 2 + addSize / 2f
        )
        addDesRectSelected.set(
            (sp.x + ep.x) / 2 - addSizeSelected / 2f,
            (sp.y + ep.y) / 2 - addSizeSelected / 2f,
            (sp.x + ep.x) / 2 + addSizeSelected / 2f,
            (sp.y + ep.y) / 2 + addSizeSelected / 2f
        )
        endArrowPath = XEditorUtils.getEndArrPath(endCell, endPos, arrowIconSize.toInt())
        startArrowPath = XEditorUtils.getEndArrPath(startCell, startPos, arrowIconSize.toInt())
        if (!TextUtils.isEmpty(labelText)) {
            val labelRect = Rect()
            mTextPaint.getTextBounds(labelText, 0, labelText!!.length, labelRect)
            val fontMetrics: Paint.FontMetrics = mTextPaint.fontMetrics
            val insert = -3 * GlobalApp.DP
            val fontMargin = 0.7f * GlobalApp.DP
            //如果是垂直线
            if (sp.y != ep.y) {
                val paddingVer = getEndMargin() - addDesRect.height() + 1f * GlobalApp.DP
                labelRect.offsetTo(
                    (addDesRect.centerX() - labelRect.width() / 2).toInt(),
                    (addDesRect.centerY() + addDesRect.width() / 2 + mTextPaint.textSize + paddingVer + 1f * GlobalApp.DP).toInt()
                )
                textPoint.x = labelRect.left.toFloat()
                textPoint.y = labelRect.bottom.toFloat() - fontMetrics.descent + fontMargin

            } else {    //如果是水平线
                labelRect.offsetTo(
                    (addDesRect.centerX() - labelRect.width() / 2f).toInt(),
                    (addDesRect.centerY() - mTextPaint.textSize - 6 * GlobalApp.DP - 1.6f * GlobalApp.DP).toInt()
                )
                textPoint.x = labelRect.left.toFloat()
                textPoint.y = labelRect.bottom.toFloat() - fontMetrics.descent + fontMargin
            }
            labelRectF.set(labelRect)
            labelRectF.inset(insert.toFloat(), insert.toFloat())
        }

        if (startCell.xIndex == endCell.xIndex || startCell.yIndex == endCell.yIndex) {
            if (shouldHalf) {
                linePath = XEditorUtils.getLinePath(
                    Point(sp.x, sp.y), Point(sp.x + (ep.x - sp.x) / 2, sp.y + (ep.y - sp.y) / 2)
                )
            } else {
                linePath = XEditorUtils.getLinePath(Point(sp.x, sp.y), Point(ep.x, ep.y))
            }

        } else {//如果不是同一行或者同一列，就画三段线，分为竖横竖三段连接，其中一段是水平或者垂直的，水平线位于中间
            val mid1 = Point(sp.x, ((sp.y - (sp.y - ep.y) / 2f).toInt()))
            val mid2 = Point(ep.x, ((sp.y - (sp.y - ep.y) / 2f).toInt()))
            linePath = XEditorUtils.getLinePath(
                Point(sp.x, sp.y), Point(ep.x, ep.y), mid1, mid2, startPos, endPos
            )
        }
        requestMeasure()
    }

    fun isGroupLine() = lineType == LineType.GROUP_TO_CMD || lineType == LineType.GROUP_EMPTY_TO_ADD

    private fun getStartMargin(): Int {
        val margin1 = if (shouldMarginStartByArrIcon) {
            arrowIconSize.toInt()
        } else {
            0
        }
        val margin2 = if (shouldMarginStartByAddIcon) {
            addSize.toInt()
        } else {
            0
        }
        return margin1 + margin2
    }

    private fun getEndMargin(): Int {
        val margin1 = if (shouldMarginEndByArrIcon) {
            arrowIconSize.toInt()
        } else {
            0
        }
        val margin2 = if (shouldMarginEndByAddIcon) {
            addSize.toInt()
        } else {
            0
        }
        return margin1 + margin2
    }


    override fun onDraw(canvas: Canvas) {
        if (startCell.isInTouchMove || endCell.isInTouchMove) {
            canvas.translate(ScriptXEditorView.touchX, ScriptXEditorView.touchY)
            //如果只有一个cell在移动，就不画线
            if (!startCell.isInTouchMove || !endCell.isInTouchMove) {
                return
            }
        }
        canvas.drawPath(
            linePath, mLinePaint
        )
        if (isNormalSize()) {
            if (supportAdd) {
                if (isInTouchSelected) {
                    canvas.drawOval(addDesRectSelected, mBackPaintSelected)
                    canvas.drawBitmap(
                        addIconBmp, addSrcRect, addDesRectSelected, mIconPaintSelected
                    )
                } else {
                    canvas.drawOval(addDesRect, mBackPaint)
                    canvas.drawBitmap(
                        addIconBmp, addSrcRect, addDesRect, mIconPaint
                    )
                }
            }
            //绘制文字
            if (!isInTouchSelected && !TextUtils.isEmpty(labelText)) {
                canvas.drawRoundRect(labelRectF, 3f * GlobalApp.DP, 3f * GlobalApp.DP, mTextBgPaint)
                canvas.drawText(
                    labelText!!, textPoint.x, textPoint.y, mTextPaint
                )
            }
        }
        if (supportStartArr) {
            canvas.drawPath(startArrowPath, mArrPaint)
        }
        if (supportEndArr) {
            canvas.drawPath(endArrowPath, mArrPaint)
        }

    }

    override fun onClick(event: MotionEvent) {
        super.onClick(event)
        if (LineType.GROUP_EMPTY_TO_ADD == lineType) {
            postEvent(ScriptMenuEditHelper.ClickType.INSERT_RECORD_INNER)
        } else if (LineType.GROUP_TO_CMD == lineType) {//插在第一个
            postEvent(ScriptMenuEditHelper.ClickType.INSERT_RECORD_BEFORE)
        } else {
            postEvent(ScriptMenuEditHelper.ClickType.INSERT_RECORD)
        }
    }

    override fun getMainCell() = startCell


    override fun onMeasure(rect: SCDrawEditRect) {
        if (supportAdd) {
            rect.set(addDesRect)
            rect.inset(-10f * GlobalApp.DP, -10f * GlobalApp.DP)
        }
    }

    private val renderRect = RectF()

    //startCell和endCell的frame的并集
    override fun getRenderRect(): RectF {
        val startCellFrame = startCell.frame
        val endCellFrame = endCell.frame
        val left = Math.min(startCellFrame.left, endCellFrame.left)
        val top = Math.min(startCellFrame.top, endCellFrame.top)
        val right = Math.max(startCellFrame.right, endCellFrame.right)
        val bottom = Math.max(startCellFrame.bottom, endCellFrame.bottom)
        renderRect.set(left, top, right, bottom)
        return renderRect
    }

    companion object {
        private val addIconBmp = BitmapUtils.drawableToBitmap(R.drawable.sc_icon_add)

        enum class LineType {
            CMD_TO_CMD, GROUP_TO_CMD, CMD_TO_END, END_TO_CMD, SCRIPT_TO_START, SCRIPT_TO_END, GROUP_EMPTY_TO_ADD
        }

        fun createLine(
            view: ScriptXEditorView,
            lineType: LineType,
            start: XCellModel,
            end: XCellModel,
            label: String? = null,
            startConnectPos: Int? = null,
            endConnectPos: Int? = null
        ): XLineNode {
            return XLineNode(
                view.context, lineType, start, end
            ).apply {
                labelText = label
                when (lineType) {
                    //cmd到cmd连线
                    LineType.CMD_TO_CMD -> {
                        supportAdd = true
                        supportStartArr = false
                        supportEndArr = true
                        isDotted = false
                    }
                    //group到cmd连线
                    LineType.GROUP_TO_CMD -> {
                        supportAdd = true
                        supportStartArr = true
                        supportEndArr = false
                        shouldMarginStartByArrIcon = true
                        labelText = GlobalApp.getString(com.hive.i8n.R.string.cmd_line_node_label_1)
                        if (end.cmd?.hasCondition() == true) {
                            labelText = if (end.cmd?.isConditionReverse() == true) {
                                GlobalApp.getString(com.hive.i8n.R.string.cmd_line_node_label_no)
                            } else {
                                GlobalApp.getString(com.hive.i8n.R.string.cmd_line_node_label_yes)
                            }
                        }
                    }
                    //end到cmd连线
                    LineType.END_TO_CMD -> {
                        supportAdd = true
                        supportStartArr = false
                        supportEndArr = true
                        isDotted = true
                        supportAdd = false
                    }
                    //cmd到end连线
                    LineType.CMD_TO_END -> {
                        supportAdd = false
                        supportStartArr = false
                        supportEndArr = false
                        isDotted = true
                        shouldMarginEndByArrIcon = true
                        shouldMarginEndByAddIcon = true
                        labelText = GlobalApp.getString(com.hive.i8n.R.string.cmd_line_node_label_2)
                    }
                    //start连线
                    LineType.SCRIPT_TO_START -> {
                        supportAdd = false
                        supportStartArr = false
                        supportEndArr = true
                    }
                    //end连线
                    LineType.SCRIPT_TO_END -> {
                        supportAdd = true
                        supportStartArr = false
                        supportEndArr = true
                    }
                    //group的子命令为空时的连线
                    LineType.GROUP_EMPTY_TO_ADD -> {
                        supportAdd = true
                        supportStartArr = false
                        supportEndArr = false
                        shouldHalf = true
                        labelText = GlobalApp.getString(com.hive.i8n.R.string.cmd_line_node_label_1)
                        endCell = XEditorUtils.findEmptyAroundCell(view, start.copy())
                    }
                }
                updateStartPosition()
                updateEndPosition()
                if (startConnectPos != null) {
                    this.startPos = startConnectPos
                }
//                if (endConnectPos != null) {
//                    this.endPos = endConnectPos
//                }
                if (!XEditorHelper.editMode) {
                    supportAdd = false
                }
                updateLine()
            }
        }
    }
}