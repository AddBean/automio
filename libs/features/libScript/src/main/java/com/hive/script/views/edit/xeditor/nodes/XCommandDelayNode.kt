// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.edit.xeditor.nodes

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.Rect
import android.graphics.RectF
import android.text.TextPaint
import android.view.MotionEvent
import com.hive.script.R
import com.hive.script.base.core.ScriptParser
import com.hive.script.base.ScriptCommand
import com.hive.script.views.edit.ScriptMenuEditHelper
import com.hive.script.views.edit.xeditor.ScriptXEditorView
import com.hive.script.views.edit.xeditor.XCellModel
import com.hive.script.views.edit.xeditor.core.SCAbsLayerItemView
import com.hive.script.views.edit.xeditor.core.model.SCDrawEditRect
import com.hive.script.views.edit.xeditor.utils.XEditorHelper
import com.hive.script.views.edit.xeditor.utils.XEditorUtils
import com.hive.utils.GlobalApp
import com.hive.utils.utils.BitmapUtils
import com.hive.utils.utils.ColorUtils
import kotlin.math.PI
import kotlin.math.sin

class XCommandDelayNode(
    context: Context, val cell: XCellModel
) : SCAbsLayerItemView(context) {

    private val mTextPaint = TextPaint().apply {
        alpha = 200
        color = 0xffffffff.toInt()
        textSize = 8f * GlobalApp.DP
        isAntiAlias = true
    }

    private val mCellDisablePaint = Paint().apply {
        color = Color.BLACK
        alpha = 150
        strokeWidth = 4f
        isAntiAlias = true
    }

    private var delayText = (("%.2fS").format((cell.cmd?.startDelay ?: 0L) / 1000f))

    private var delayWidth = mTextPaint.measureText(delayText)

    private val mIconPaint = Paint().apply {
        alpha = 200
        color = 0xffffffff.toInt()
        
        val filter = PorterDuffColorFilter(
            GlobalApp.getColor(com.hive.i8n.R.color.colorIcon), PorterDuff.Mode.SRC_IN
        );
        colorFilter = filter
    }

    private val mBgPaint = Paint().apply {
        val colorSrc = ScriptParser.getXCellColor(cell.cmd)
        color = ColorUtils.blendColors(colorSrc, 0xff000000.toInt(), 0.5f)
    }

    private val point =
        XEditorUtils.findTargetConnectPoint(findNextCell(cell.nextCell) ?: XCellModel().apply {
            xIndex = cell.xIndex
            prevCell = cell
            yIndex = cell.yIndex + 1
            frame = RectF(
                xIndex * XEditorHelper.cellWidth,
                yIndex * XEditorHelper.cellHeight,
                (xIndex + 1) * XEditorHelper.cellWidth,
                (yIndex + 1) * XEditorHelper.cellHeight
            )
            val round = 16f * GlobalApp.DP
            frame.inset((frame.width() - round) / 2, (frame.height() - round) / 2)
            frame.offset(
                0f, -XEditorHelper.cellHeight / 2f
            )
        }, cell, 0)

    private val hezPadding = 3 * dp

    private val verSize = 14 * GlobalApp.DP

    private var hezSize = (delayWidth + verSize + hezPadding).toInt()

    private val desRect = RectF().apply {
        set(
            point.x - hezSize / 2f,
            point.y - verSize / 2f,
            point.x + hezSize / 2f,
            point.y + verSize / 2f
        )
    }

    private val srcRect = Rect().apply {
        set(0, 0, iconBmp.width, iconBmp.height)
    }

    private val desBmpRect = RectF().apply {
        set(
            point.x - hezSize / 2f,
            point.y - verSize / 2f,
            point.x - hezSize / 2f + verSize,
            point.y + verSize / 2f
        )
        inset(2f * GlobalApp.DP, 2f * GlobalApp.DP)
    }

    /**
     * 查找下一个cell
     */
    private fun findNextCell(nextCell: XCellModel?): XCellModel? {
        if (nextCell?.parentCell != cell.parentCell) {
            return null
        }
        return nextCell
    }

    override fun onDataRefresh(cmd: ScriptCommand?) {
        cell.cmd = cmd

        delayText = (("%.2fS").format((cell.cmd?.startDelay ?: 0L) / 1000f))

        delayWidth = mTextPaint.measureText(delayText)

        hezSize = (delayWidth + verSize + hezPadding).toInt()

        desRect.apply {
            set(
                point.x - hezSize / 2f,
                point.y - verSize / 2f,
                point.x + hezSize / 2f,
                point.y + verSize / 2f
            )
        }

        srcRect.apply {
            set(0, 0, iconBmp.width, iconBmp.height)
        }

        desBmpRect.apply {
            set(
                point.x - hezSize / 2f,
                point.y - verSize / 2f,
                point.x - hezSize / 2f + verSize,
                point.y + verSize / 2f
            )
            inset(2f * GlobalApp.DP, 2f * GlobalApp.DP)
        }
    }

    override fun onMeasure(rect: SCDrawEditRect) {
        rect.set(desRect)
        rect.inset(-10f * GlobalApp.DP, -10f * GlobalApp.DP)
    }

    override fun onDraw(canvas: Canvas) {
        if (cell.isInTouchMove) {
            canvas.translate(ScriptXEditorView.touchX, ScriptXEditorView.touchY)
        }
        if (isNormalSize()) {
            canvas.drawRoundRect(desRect, desRect.height() / 2f, desRect.height() / 2f, mBgPaint)
            canvas.drawBitmap(
                iconBmp, srcRect, desBmpRect, mIconPaint
            )
            canvas.drawText(
                delayText,
                desRect.left + verSize + hezPadding / 5f,
                desRect.centerY() + mTextPaint.textSize / 2f - 0.6f * dp,
                mTextPaint
            )

            //如果是禁用状态，绘制禁用遮罩
            if (cell.isDisabled) {
                canvas.drawRoundRect(
                    desRect,
                    desRect.height() / 2f,
                    desRect.height() / 2f,
                    mCellDisablePaint
                )
            }
        }
    }

    override fun getRenderRect(): RectF {
        return cell.frame
    }

    override fun onDrawAnim(canvas: Canvas, animPercent: Float, type: String?) {
        //动画
        var value = (1f - sin(animPercent * PI) * 0.3f).toFloat()
        if (value > 1f) {
            value = 1f
        }
        if (value < 0f) {
            value = 0f
        }
        canvas.scale(value, value, desRect.centerX(), desRect.centerY())
        onDraw(canvas)
    }

    override fun getMainCell() = cell

    override fun onClick(event: MotionEvent) {
        startAnim(200f)
        ScriptMenuEditHelper.showEditDialog(mParentView?.context!!,
            cell.cmd!!,
            true,
            { event, cmd ->
                postEvent(event)
            }) { newCmd ->
            postEvent(ScriptXEditorView.XEditorEvent.REFRESH_DATA, newCmd)
            requestParentInvalidate()
        }
    }

    companion object {

        private val iconBmp = BitmapUtils.drawableToBitmap(R.drawable.sc_icon_delay)


        fun create(
            context: Context, cell: XCellModel
        ): XCommandDelayNode {
            return XCommandDelayNode(context, cell)
        }
    }
}