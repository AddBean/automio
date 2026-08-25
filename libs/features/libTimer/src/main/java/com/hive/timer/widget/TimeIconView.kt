// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.timer.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.EmbossMaskFilter
import android.graphics.Paint
import android.graphics.Point
import android.util.AttributeSet
import android.view.View
import com.hive.timer.R
import com.hive.utils.utils.DensityUtil
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Created by AddBean on 2016/3/18.
 */
class TimeIconView : View {
    private var colorDay = Color.BLACK
    private var colorNight = Color.WHITE
    private val Hour_Accuracy = (12 * 60).toDouble()//精确到分钟
    private val Min_Accuracy = 60.0 //精确到分钟
    private var minus = 0
    private var hours = 0
    private var animSpeed = 1
    private var mMin = 0
    private var mHour = 0
    private var animEnable = false
    private var calendar: Calendar? = null
    private var isDaytime = true
    private val dp = DensityUtil.dip2px(1f)
    private val paintBg = Paint()
    private val paintHour = Paint()
    private val paintMinute = Paint()
    private var mCount = 0
    private var timeWidth = 2f*dp

    constructor(context: Context?) : super(context) {
        initAttrs(null)
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        initAttrs(attrs)
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        initAttrs(attrs)
    }

    private fun initAttrs(attrs: AttributeSet?) {
        attrs?.run {
            val a = context.obtainStyledAttributes(
                attrs,
                R.styleable.TimeIconView
            )
            val count = a.indexCount
            for (i in 0 until count) {
                when (val attr = a.getIndex(i)) {
                    R.styleable.TimeIconView_timeIconDayColor -> {
                        colorDay = a.getColor(attr, Color.BLACK)
                    }

                    R.styleable.TimeIconView_timeIconNightColor -> {
                        colorNight = a.getColor(attr, Color.BLACK)
                    }

                    R.styleable.TimeIconView_timeIconSpeed -> {
                        animSpeed = a.getInt(attr, 1)
                    }

                    R.styleable.TimeIconView_timeIconLineWidth -> {
                        timeWidth = a.getDimension(attr, 2.4f * dp)
                    }
                }

            }
            a.recycle()
        }
    }

    override fun onDraw(canvas: Canvas) {
        drawBg(canvas)
        drawHour(canvas, mHour, mMin)
        drawMinute(canvas, mMin)
        super.onDraw(canvas)
    }


    private fun drawBg(canvas: Canvas) {
        paintBg.isAntiAlias = true
        paintBg.color = colorDay
        val lineWidth = timeWidth
        paintBg.strokeWidth = lineWidth.toFloat()
        paintBg.style = if (isDaytime) Paint.Style.STROKE else Paint.Style.FILL
        val radius = measuredWidth.coerceAtMost(measuredHeight) / 2 - lineWidth
        canvas.drawCircle(
            (measuredWidth / 2).toFloat(),
            (measuredHeight / 2).toFloat(),
            radius.toFloat(),
            paintBg
        )
        paintBg.style = Paint.Style.STROKE
        val direction = floatArrayOf(1f, 1f, 1f)
        val light = 0.4f
        val specular = 6f
        val blur = 3.5f // 向mask应用一定级别的模糊
        val emboss = EmbossMaskFilter(direction, light, specular, blur)
        paintBg.maskFilter = emboss
        paintBg.color = if (isDaytime) colorDay else colorNight
        paintBg.style = Paint.Style.FILL
        canvas.drawCircle(
            (measuredWidth / 2).toFloat(),
            (measuredHeight / 2).toFloat(),
            (radius / 7).toFloat(),
            paintBg
        )
        if (animEnable) {
            if (mMin == minus) {
                animEnable = false
            } else {
                mCount++
                if (mCount > animSpeed) {
                    mCount = 0
                    calendar!!.add(Calendar.MINUTE, 1)
                    mMin = calendar!![Calendar.MINUTE]
                }
                postInvalidate()
            }
        }
    }

    private fun drawHour(canvas: Canvas, hour: Int, min: Int) {
        paintHour.isAntiAlias = true
        paintHour.strokeJoin = Paint.Join.ROUND
        paintHour.strokeCap = Paint.Cap.ROUND
        paintHour.color =
            if (isDaytime) colorDay else colorNight
        val lineWidth = timeWidth * (11 / 12f)
        paintHour.strokeWidth = lineWidth.toFloat()
        paintHour.style =
            if (isDaytime) Paint.Style.STROKE else Paint.Style.FILL
        val radius = measuredWidth.coerceAtMost(measuredHeight) / 2 - lineWidth
        val circlePoint = Point(measuredWidth / 2, measuredHeight / 2)
        val startPoint = Point(measuredWidth / 2, (measuredHeight / 2 - radius / 8).toInt())
        val endPoint = Point(measuredWidth / 2, (radius / 2).toInt())
        val radius1 = abs(startPoint.y - circlePoint.y)
        val radius2 = abs(endPoint.y - circlePoint.y)
        val time = (hour * 60 + min).toDouble()
        startPoint.x =
            (radius1 * cos(time * 2 * Math.PI / Hour_Accuracy - Math.PI / 2)).toInt() + circlePoint.x
        startPoint.y =
            (radius1 * sin(time * 2 * Math.PI / Hour_Accuracy - Math.PI / 2)).toInt() + circlePoint.y
        endPoint.x =
            (radius2 * cos(time * 2 * Math.PI / Hour_Accuracy - Math.PI / 2)).toInt() + circlePoint.x
        endPoint.y =
            (radius2 * sin(time * 2 * Math.PI / Hour_Accuracy - Math.PI / 2)).toInt() + circlePoint.y
        canvas.drawLine(
            startPoint.x.toFloat(),
            startPoint.y.toFloat(),
            endPoint.x.toFloat(),
            endPoint.y.toFloat(),
            paintHour
        )
    }

    private fun drawMinute(canvas: Canvas, min: Int) {
        paintMinute.isAntiAlias = true
        paintMinute.strokeJoin = Paint.Join.ROUND
        paintMinute.strokeCap = Paint.Cap.ROUND
        paintMinute.color = if (isDaytime) colorDay else colorNight
        val lineWidth = timeWidth * (10 / 12f)
        paintMinute.strokeWidth = lineWidth.toFloat()
        paintMinute.style = Paint.Style.STROKE
        val radius = measuredWidth.coerceAtMost(measuredHeight) / 2 - lineWidth
        val circlePoint = Point(measuredWidth / 2, measuredHeight / 2)
        val startPoint = Point(measuredWidth / 2, (measuredHeight / 2 - radius / 8).toInt())
        val endPoint = Point(measuredWidth / 2, (radius / 3).toInt())
        val radius1 = abs(startPoint.y - circlePoint.y)
        val radius2 = abs(endPoint.y - circlePoint.y)
        val time = min.toDouble()
        startPoint.x =
            (radius1 * cos(time * 2 * Math.PI / Min_Accuracy - Math.PI / 2)).toInt() + circlePoint.x
        startPoint.y =
            (radius1 * sin(time * 2 * Math.PI / Min_Accuracy - Math.PI / 2)).toInt() + circlePoint.y
        endPoint.x =
            (radius2 * cos(time * 2 * Math.PI / Min_Accuracy - Math.PI / 2)).toInt() + circlePoint.x
        endPoint.y =
            (radius2 * sin(time * 2 * Math.PI / Min_Accuracy - Math.PI / 2)).toInt() + circlePoint.y
        canvas.drawLine(
            startPoint.x.toFloat(),
            startPoint.y.toFloat(),
            endPoint.x.toFloat(),
            endPoint.y.toFloat(),
            paintMinute
        )
    }

    fun setTime(date: Date, animEnable: Boolean) {
        this.animEnable = animEnable
        calendar = Calendar.getInstance()
        calendar?.time = date
        hours = calendar!!.get(Calendar.HOUR) //12小时进制；
        minus = calendar!!.get(Calendar.MINUTE)
        isDaytime = isDayTime(date)
        if (!animEnable) {
            mHour = hours
            mMin = minus
            postInvalidate()
        } else {
            mMin = 0
            mHour = hours
            calendar!!.set(Calendar.MINUTE, 0)
            postInvalidate()
        }
    }

    private fun isDayTime(date: Date): Boolean {
        val sdf = SimpleDateFormat("HH")
        val hour = sdf.format(date)
        val k = hour.toInt()
        return !(k in 0..5 || k in 18..23)
    }
}