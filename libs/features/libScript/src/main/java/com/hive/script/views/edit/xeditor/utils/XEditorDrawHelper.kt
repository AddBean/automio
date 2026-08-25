// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.edit.xeditor.utils

import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Point
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import com.hive.script.R
import com.hive.utils.GlobalApp
import com.hive.utils.extends.dpf
import kotlin.math.floor
import androidx.core.graphics.withClip

object XEditorDrawHelper {

    private const val MIN_SCALE_THRESHOLD = 0.5f

    private val dotGridPaint = Paint().apply {
        isAntiAlias = true
        color = 0x2fffffff
        style = Paint.Style.FILL
        strokeWidth = 2.dpf
    }

    private val deleteSize = 90f * GlobalApp.DP

    private val deleteOffset = 16f * GlobalApp.DP

    var deleteArea =
        RectF(
            -deleteSize - deleteOffset,
            -deleteSize - deleteOffset,
            deleteSize - deleteOffset,
            deleteSize - deleteOffset
        )

    //在deleteArea的右下角中间
    private val deleteIconRect =
        RectF(10f * GlobalApp.DP, 13f * GlobalApp.DP, 30f * GlobalApp.DP, 33f * GlobalApp.DP)

    private val deleteIcon =
        BitmapFactory.decodeResource(GlobalApp.getResources(), R.drawable.sc_edit_delete)

    private val deleteIconSrcRect = Rect(0, 0, deleteIcon.width, deleteIcon.height)

    private val deletePaintIn = Paint().apply {
        isAntiAlias = true
        alpha = 0xff
        shader = LinearGradient(
            0f, 0f, deleteSize, deleteSize,
            0x88FF0000.toInt(), 0x00FF0000, Shader.TileMode.CLAMP
        )
    }

    private val deletePaintOut = Paint().apply {
        isAntiAlias = true
        shader = LinearGradient(
            0f, 0f, deleteSize, deleteSize,
            0x88FF0000.toInt(), 0xFFFF0000.toInt(), Shader.TileMode.CLAMP
        )
    }

    private val selectionPositionPaint = Paint().apply {
        color = GlobalApp.getColor(com.hive.i8n.R.color.colorGreen)
        alpha = 160
        strokeWidth = 2f * GlobalApp.DP
        isAntiAlias = true
        style = Paint.Style.FILL_AND_STROKE

    }

    fun drawDeleteArea(canvas: Canvas?, deleteMode: Boolean) {
        canvas ?: return
        if (deleteMode) {

            canvas.drawCircle(
                deleteArea.centerX(),
                deleteArea.centerY(),
                deleteArea.width() / 2,
                deletePaintIn
            )
            canvas.drawBitmap(deleteIcon, deleteIconSrcRect, deleteIconRect, deletePaintIn)
        } else {
            deletePaintOut.alpha = 0xaa
            canvas.drawCircle(
                deleteArea.centerX(),
                deleteArea.centerY(),
                deleteArea.width() / 2,
                deletePaintOut
            )
            deletePaintOut.alpha = 0x55
            canvas.drawBitmap(deleteIcon, deleteIconSrcRect, deleteIconRect, deletePaintOut)
        }
    }

    fun drawSelectedPosition(canvas: Canvas?, position: Point) {
        val itemCellWidth = XEditorHelper.cellWidth
        val itemCellHeight = XEditorHelper.cellHeight
        val left = position.x * itemCellWidth
        val top = position.y * itemCellHeight
        val right = left + itemCellWidth
        val bottom = top + itemCellHeight
        canvas?.drawRoundRect(
            RectF(left, top, right, bottom).apply {
                inset(XEditorHelper.cellHevPadding / 2f, XEditorHelper.cellVerPadding / 2f)
            },
            20f * GlobalApp.DP,
            20f * GlobalApp.DP,
            selectionPositionPaint
        )
    }

    private val dotBgBiosX = -0.dpf
    private val dotBgBiosY = -3.dpf

    /**
     * 绘制随缩放显示的点阵背景，用于编辑器定位参考。缩放低于 [MIN_SCALE_THRESHOLD] 时不绘制以优化性能。
     * 仅对屏幕可见区域 [rect] 做 clip 并在此范围内遍历绘制，减少绘制调用与像素填充。
     */
    fun drawDotGridBackground(canvas: Canvas?, rect: RectF, scale: Float) {
        if (canvas == null || scale < MIN_SCALE_THRESHOLD) return
        val stepX = XEditorHelper.cellHeight
        val stepY = XEditorHelper.cellHeight
        val startX = (floor(rect.left / stepX) * stepX) + dotBgBiosX
        val startY = (floor(rect.top / stepY) * stepY) + dotBgBiosY
        canvas.withClip(rect) {
            var x = startX
            while (x <= rect.right) {
                var y = startY
                while (y <= rect.bottom) {
                    drawPoint(x, y, dotGridPaint)
                    y += stepY
                }
                x += stepX
            }
        }
    }
}