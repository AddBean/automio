// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.widgets;

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import com.hive.views.R

class ScriptGradientAnimView @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
    defAttributeStyle: Int = 0
) : View(
    context, attributeSet, defAttributeStyle
) {


    private val matrix = Matrix()
    private val startColor: Int
    private val centerColor: Int
    private val endColor: Int
    private var animEnable = false
    private val rectF = RectF(0f, 0f, 0f, 0f)

    private val mPaint: Paint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    private var mLinearGradient: LinearGradient? = null

    init {
        val typeArray = context.obtainStyledAttributes(
            attributeSet,
            R.styleable.LinearGradientView,
            defAttributeStyle,
            0
        )

        startColor = typeArray.getColor(
            R.styleable.LinearGradientView_android_startColor,
            Color.parseColor("#80000000")
        )
        centerColor = typeArray.getColor(
            R.styleable.LinearGradientView_android_centerColor,
            Color.parseColor("#00000000")
        )

        endColor = typeArray.getColor(
            R.styleable.LinearGradientView_android_endColor,
            Color.parseColor("#00000000")
        )

        typeArray.recycle()
    }

    fun startAnim() {
        animEnable = true
        postInvalidate()
    }

    fun stopAnim() {
        animEnable = false
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        mLinearGradient = LinearGradient(
            0f, 0f, w.toFloat(), 0f,
            intArrayOf(
                startColor, centerColor,
                endColor
            ), null,
            Shader.TileMode.CLAMP
        )

        mPaint.shader = mLinearGradient

        postInvalidate()
    }


    override fun onDraw(canvas: Canvas) {
        if (animEnable) {
            rectF.set(0f, 0f, width.toFloat(), height.toFloat())
            canvas.drawRoundRect(rectF, height.toFloat() / 2, height.toFloat() / 2, mPaint)
            matrix.postRotate(5f, width / 2f, height / 2f)
            mLinearGradient?.setLocalMatrix(matrix)
            postDelayed({ postInvalidate() }, 20)
        }
    }
}