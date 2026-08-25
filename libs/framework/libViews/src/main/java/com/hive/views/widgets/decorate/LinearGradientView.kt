// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.views.widgets.decorate

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import com.hive.views.R

/**
 * 线性渐变背景
 * Created by gangzhiguo
 * on 2020/9/2
 */

class LinearGradientView @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
    defAttributeStyle: Int = 0
) : View(
    context, attributeSet, defAttributeStyle
) {

    private val orientation_topToBottom = 1
    private val orientation_bottomToTop = 2


    private val startColor: Int
    private val endColor: Int
    private val orientation: Int

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
        endColor = typeArray.getColor(
            R.styleable.LinearGradientView_android_endColor,
            Color.parseColor("#00000000")
        )

        orientation = typeArray.getInt(
            R.styleable.LinearGradientView_orientation,
            orientation_topToBottom
        )

        typeArray.recycle()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        if (orientation == orientation_topToBottom) {
            mLinearGradient = LinearGradient(
                0f, 0f, 0f, h.toFloat(),
                startColor,
                endColor,
                Shader.TileMode.CLAMP
            )
        } else {
            mLinearGradient = LinearGradient(
                0f, h.toFloat(), 0f, 0f,
                startColor,
                endColor,
                Shader.TileMode.CLAMP
            )
        }

        mPaint.shader = mLinearGradient

        postInvalidate()
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), mPaint)
    }
}