// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PathMeasure
import android.graphics.Point
import android.graphics.Rect
import android.graphics.RectF
import com.hive.script.R
import com.hive.script.extensions.arrBmp
import com.hive.utils.GlobalApp
import com.hive.utils.extends.color
import com.hive.utils.extends.dp
import kotlin.math.atan2
import kotlin.math.pow
import kotlin.math.sqrt

/**
 *
 * @author jiadou
 * @date 7/23/21
 */
object ScriptCommonDrawer {
    var DP = GlobalApp.DP

    var debugPaint = Paint().apply {
        isAntiAlias = true
        color = Color.YELLOW
        style = Paint.Style.STROKE
        strokeWidth = 2f * DP
    }

    private var desRect = Rect()

    private var srcRect = Rect()

    private var paintRect = Paint().apply {
        isAntiAlias = true
        strokeWidth = 2f * DP
    }

    private var paintBitmap = Paint().apply { isAntiAlias = true }

    private var paint1 = Paint().apply { isAntiAlias = true }

    private var paint2 = Paint().apply { isAntiAlias = true }

    private var paint3 = Paint().apply { isAntiAlias = true }

    private var paintCross =
        Paint().apply {
            isAntiAlias = true
            pathEffect = DashPathEffect(floatArrayOf(5f, 7f), 0f)
        }

    private var paintCross2 =
        Paint().apply {
            isAntiAlias = true
        }

    private var paintPath = Paint().apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeWidth = 30f * DP
        color = 0x555555
        isAntiAlias = true
    }
    private var paintLine = Paint().apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeWidth = 26f * DP
        color = 0x555555
        isAntiAlias = true
    }
    private var paintDot = Paint().apply {
        style = Paint.Style.FILL
        color = Color.RED
        isAntiAlias = true
    }

    private var tempRectF = RectF()

    private var dotPaint = Paint().apply {
        isAntiAlias = true
        color = Color.WHITE
        style = Paint.Style.FILL
        strokeWidth = 2f.dp
        pathEffect = DashPathEffect(floatArrayOf(4f, 6f), 0f)
    }

    private var dotBgPaint = Paint().apply {
        isAntiAlias = true
        color = Color.BLACK
        style = Paint.Style.FILL
        strokeWidth = 2f * DP
    }

    private var dotCrossBgPaint = Paint().apply {
        isAntiAlias = true
        color = 0x6fE9463C.toInt()
        style = Paint.Style.FILL
        strokeWidth = 1f * DP
    }


    private var dotTextPaint = Paint().apply {
        isAntiAlias = true
        color = Color.WHITE
        textSize = 14f * DP
        style = Paint.Style.FILL
    }

    private var minTextPaint = Paint().apply {
        isAntiAlias = true
        color = Color.WHITE
        textSize = 12f * DP
        style = Paint.Style.FILL
    }

    private var minBgPaint = Paint().apply {
        isAntiAlias = true
        color = 0xaf000000.toInt()
        style = Paint.Style.FILL
        strokeWidth = 2f * DP
    }

    private var dotPath = Path()


    fun drawLimitRect(
        canvas: Canvas? = null,
        rect: Rect? = null,
        alpha: Int = 255,
        color: Int = GlobalApp.getColor(com.hive.i8n.R.color.colorAccent)
    ) {
        rect?.run {
            paintRect.style = Paint.Style.STROKE
            paintRect.color = color
            paintRect.alpha = alpha
            canvas?.drawRect(rect, paintRect)
        }
    }

    fun drawBatchRect(
        canvas: Canvas? = null,
        rect: Rect? = null,
        alpha: Int = 255,
        color: Int = GlobalApp.getColor(com.hive.i8n.R.color.colorAccent)
    ) {
        rect?.run {
            paintRect.style = Paint.Style.FILL
            paintRect.color = color
            paintRect.alpha = alpha
            canvas?.drawRect(rect, paintRect)
        }
    }

    fun drawRect(
        canvas: Canvas? = null,
        rect: Rect? = null,
        alpha: Int = 255,
        color: Int = 0xffE9463C.toInt()
    ) {
        rect?.run {
            paintRect.style = Paint.Style.STROKE
            paintRect.color = color
            paintRect.alpha = alpha
            tempRectF.set(rect)
            canvas?.drawRoundRect(tempRectF, 3f.dp, 3f.dp, paintRect)
        }
    }

    fun drawCircle(
        canvas: Canvas? = null,
        x: Float = 0f,
        y: Float = 0f,
        alpha: Int = 255,
        color: Int = 0xffa3a3a3.toInt()
    ) {
        paint1.style = Paint.Style.FILL
        paint2.style = Paint.Style.FILL
        paint3.style = Paint.Style.STROKE
        paint3.strokeWidth = 8f * DP
        paint3.style = Paint.Style.STROKE
        paint1.color = 0x6f000000 or color
        paint2.color = 0x8fffffff.toInt() or color
        paint3.color = 0x4f000000 or color
        paint1.alpha = ((alpha / 255f) * 0x6f).toInt()
        paint2.alpha = ((alpha / 255f) * 0x6f).toInt()
        paint3.alpha = ((alpha / 255f) * 0x4f).toInt()
        canvas?.drawCircle(x, y, 26f * DP, paint1)
        canvas?.drawCircle(x, y, 14f * DP, paint3)
        canvas?.drawCircle(x, y, 12f * DP, paint2)
    }

    fun drawDot(
        canvas: Canvas? = null,
        x: Float = 0f,
        y: Float = 0f,
        alpha: Int = 255,
        color: Int = 0xffffffff.toInt()
    ) {
        paint1.style = Paint.Style.FILL
        paint1.alpha = alpha
        paint1.color = color
        canvas?.drawCircle(x, y, 2f * DP, paint1)
    }

    fun drawDot2(
        canvas: Canvas? = null,
        x: Float = 0f,
        y: Float = 0f,
        alpha: Int = 255,
        color: Int = 0xffffffff.toInt()
    ) {
        paint1.style = Paint.Style.FILL
        paint1.alpha = alpha
        paint1.color = color
        canvas?.drawCircle(x, y, 1f * DP, paint1)
    }

    fun drawPath(canvas: Canvas, path: Path, alpha: Int) {
        paintPath.alpha = alpha
        canvas.drawPath(path, paintPath)
    }

    /**
     * Draw Guide Line
     */
    fun drawGuideLine(
        canvas: Canvas,
        x1: Float,
        y1: Float,
        x2: Float,
        y2: Float,
        alpha: Int = 180
    ) {
        paintLine.alpha = alpha
        canvas.drawLine(x1, y1, x2, y2, paintLine)
    }

    /**
     * 绘制arr线
     */
    fun drawGuideArrLine(
        canvas: Canvas,
        fromX: Int,
        fromY: Int,
        toX: Int,
        toY: Int,
        exDegree: Int = 0
    ) {
        drawGuideArrLine(
            canvas,
            fromX,
            fromY,
            toX,
            toY,
            arrBmp,
            18 * GlobalApp.DP,
            30 * DP,
            exDegree
        )
    }

    /**
     * 绘制量尺线，用于标注距离，一头为箭头，一头为圆点，中间为虚线，虚线中间有数字
     */
    fun drawMeasureInfo(
        canvas: Canvas,
        fromX: Int,
        fromY: Int,
        toX: Int,
        toY: Int
    ) {
        val distance =
            sqrt((toX - fromX).toDouble().pow(2.0) + (toY - fromY).toDouble().pow(2.0)).toInt()
        val distanceInfo = distance.toString()

        dotPath.reset()
        dotPath.moveTo(fromX.toFloat(), fromY.toFloat())
        dotPath.lineTo(toX.toFloat(), toY.toFloat())

        canvas.save()
        canvas.drawLine(fromX.toFloat(), fromY.toFloat(), toX.toFloat(), toY.toFloat(), dotPaint)
        canvas.restore()

        val pathMeasure = PathMeasure(dotPath, false)
        val pathLength = pathMeasure.length
        val textWidth = dotTextPaint.measureText(distanceInfo)
        val textOffset = (pathLength - textWidth) / 2

        val pos = FloatArray(2)
        val tan = FloatArray(2)
        pathMeasure.getPosTan(textOffset, pos, tan)
        val angle = atan2(tan[1], tan[0]) * (180 / Math.PI).toFloat()

        canvas.save()
        canvas.rotate(angle, pos[0], pos[1])
        //绘制文字黑色背景
        tempRectF.set(
            pos[0] - 4.dp,
            (pos[1] - dotTextPaint.textSize / 2f - 1.dp),
            (pos[0] + textWidth) + 4.dp,
            pos[1] + dotTextPaint.textSize / 2f + 5.dp
        )
        canvas.drawRoundRect(tempRectF, 4f.dp, 4f.dp, dotBgPaint)
        canvas.drawText(distanceInfo, pos[0], pos[1] + dotTextPaint.textSize / 2f, dotTextPaint)
        canvas.restore()
    }

    /**
     * 绘制文字，居中绘制，且有背景
     */

    fun drawTextInfo(
        canvas: Canvas,
        text: String,
        x: Int,
        y: Int
    ) {
        val textWidth = minTextPaint.measureText(text)

        canvas.save()

        minBgPaint.alpha = 110
        minBgPaint.color = 0xafffffff.toInt()
        minBgPaint.style = Paint.Style.STROKE
        //绘制圆形背景
        canvas.drawCircle(
            x.toFloat(),
            y.toFloat(),
            textWidth / 2 + 12.dp,
            minBgPaint
        )

        minBgPaint.alpha = 100
        minBgPaint.color = 0xaf000000.toInt()
        minBgPaint.style = Paint.Style.FILL

        val r = textWidth / 2 + 9.dp
        //绘制圆形背景
        canvas.drawCircle(
            x.toFloat(),
            y.toFloat(),
            r,
            minBgPaint
        )

        //绘制Circle内部十字
        canvas.drawLine(
            x.toFloat(),
            y.toFloat() - r,
            x.toFloat(),
            y.toFloat() + r,
            dotCrossBgPaint
        )

        canvas.drawLine(
            x.toFloat() - r,
            y.toFloat(),
            x.toFloat() + r,
            y.toFloat(),
            dotCrossBgPaint
        )

        canvas.drawText(
            text,
            x.toFloat() - textWidth / 2,
            y + minTextPaint.textSize / 4f,
            minTextPaint
        )
        canvas.restore()
    }

    /**
     * 绘制arr线
     */
    private fun drawGuideArrLine(
        canvas: Canvas,
        fromX: Int,
        fromY: Int,
        toX: Int,
        toY: Int,
        arrBmp: Bitmap?,
        bmpSize: Int,
        itemMargin: Int,
        exDegree: Int = 0
    ) {
        if (arrBmp == null) return
        desRect.set(0, 0, bmpSize, bmpSize)
        srcRect.set(0, 0, arrBmp.width, arrBmp.height)
        val degree = 180 - Math.toDegrees(atan2((toX - fromX).toDouble(), (toY - fromY).toDouble()))
            .toFloat() + exDegree
        val disAl = (toY - fromY).toDouble().pow(2.0) + (toX - fromX).toDouble().pow(2.0)
        val disAl2 = sqrt(disAl)
        val rateX = (toX - fromX) / disAl2
        val rateY = (toY - fromY) / disAl2
        var curDis = 0.0
        val xStep = itemMargin * rateX.toFloat()
        val yStep = itemMargin * rateY.toFloat()
        var x = fromX + xStep
        var y = fromY + yStep
        canvas.save()
        while (curDis < disAl) {
            desRect.left = (x - bmpSize).toInt()
            desRect.right = (x + bmpSize).toInt()
            desRect.top = (y - bmpSize).toInt()
            desRect.bottom = (y + bmpSize).toInt()
            canvas.save()
            canvas.rotate(degree, desRect.centerX().toFloat(), desRect.centerY().toFloat())
            canvas.drawBitmap(arrBmp, srcRect, desRect, paintBitmap)
            canvas.restore()
            x += xStep
            y += yStep
            curDis = (y - fromY).toDouble().pow(2.0) + (x - fromX).toDouble().pow(2.0)
        }
        canvas.restore()
    }

    /**
     * 控制点
     */
    fun drawTouchCrossDot(
        canvas: Canvas,
        x: Int,
        y: Int,
        alpha: Int = 255,
        color: Int = 0xffa3a3a3.toInt()
    ) {
        paint1.style = Paint.Style.FILL
        paint2.style = Paint.Style.FILL
        paint3.style = Paint.Style.STROKE
        paint3.strokeWidth = 10f * DP
        paint3.style = Paint.Style.STROKE
        paint1.color = 0x6f000000 or color
        paint2.color = 0xafffffff.toInt() or color
        paint3.color = 0x5f000000 or color
        paint1.alpha = ((alpha / 255f) * 0x6f).toInt()
        paint2.alpha = ((alpha / 255f) * 0x6f).toInt()
        paint3.alpha = ((alpha / 255f) * 0x4f).toInt()
//        canvas.drawCircle(x.toFloat(), y.toFloat(), 26f * DP, paint1)
        canvas.drawCircle(x.toFloat(), y.toFloat(), 14f * DP, paint3)
        canvas.drawCircle(x.toFloat(), y.toFloat(), 12f * DP, paint2)

        //画个canvas十字，上下左右点分别在canvas边缘，共4条线，交接点在circle的上下左右边缘，circle的中心点在xy，半径为14dp
        var r = 20f * DP
        paintCross.strokeWidth = 1f * DP
        paintCross.color = com.hive.i8n.R.color.colorAccent.color()
        canvas.drawLine(x.toFloat(), y.toFloat() - r, x.toFloat(), 0f, paintCross)
        canvas.drawLine(
            x.toFloat(),
            y.toFloat() + r,
            x.toFloat(),
            canvas.height.toFloat(),
            paintCross
        )
        canvas.drawLine(x.toFloat() - r, y.toFloat(), 0f, y.toFloat(), paintCross)
        canvas.drawLine(
            x.toFloat() + r,
            y.toFloat(),
            canvas.width.toFloat(),
            y.toFloat(),
            paintCross
        )

        //画个小十字在xy，大小为4dp
        r = 5f * DP
        paintCross2.strokeWidth = 1f * DP
        paintCross2.color = com.hive.i8n.R.color.colorAccent.color()
        canvas.drawLine(x.toFloat(), y.toFloat() - r, x.toFloat(), y.toFloat() + r, paintCross2)
        canvas.drawLine(x.toFloat() - r, y.toFloat(), x.toFloat() + r, y.toFloat(), paintCross2)
    }

    /**
     * 控制点
     */
    fun drawTouchDot(
        canvas: Canvas,
        x: Int,
        y: Int,
        alpha: Int = 255,
        color: Int = 0xffa3a3a3.toInt()
    ) {
        paint1.style = Paint.Style.FILL
        paint2.style = Paint.Style.FILL
        paint3.style = Paint.Style.STROKE
        paint3.strokeWidth = 10f * DP
        paint3.style = Paint.Style.STROKE
        paint1.color = 0x6f000000 or color
        paint2.color = 0xafffffff.toInt() or color
        paint3.color = 0x5f000000 or color
        paint1.alpha = ((alpha / 255f) * 0x6f).toInt()
        paint2.alpha = ((alpha / 255f) * 0x6f).toInt()
        paint3.alpha = ((alpha / 255f) * 0x4f).toInt()
//        canvas.drawCircle(x.toFloat(), y.toFloat(), 26f * DP, paint1)
        canvas.drawCircle(x.toFloat(), y.toFloat(), 14f * DP, paint3)
        canvas.drawCircle(x.toFloat(), y.toFloat(), 12f * DP, paint2)
    }


    /**
     * 控制点
     */
    fun drawTouchPreviewDot(
        canvas: Canvas,
        x: Int,
        y: Int,
        alpha: Int = 255,
        color: Int = 0xffa3a3a3.toInt()
    ) {
        paint2.style = Paint.Style.FILL_AND_STROKE
        paint2.color = 0xaf0498FF.toInt() or color
        paint2.alpha = alpha
        paint2.strokeWidth = 1f * DP
        canvas.drawCircle(x.toFloat(), y.toFloat(), 4f * DP, paint2)
    }

    fun drawClickDot(canvas: Canvas, point: Point) {
        canvas.drawCircle(point.x.toFloat(), point.y.toFloat(), 2f * DP, paintDot)
    }

}