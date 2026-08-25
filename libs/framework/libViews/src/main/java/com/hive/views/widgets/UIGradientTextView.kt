// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.views.widgets

import android.content.Context
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Shader
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import com.hive.views.R

class UIGradientTextView : AppCompatTextView {
    private var startColor = 0
    private var centerColor: Int? = null
    private var centerPosition: Float = 0.5f
    private var endColor = 0

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle) {
        init(context, attrs)
    }

    private fun init(context: Context, attrs: AttributeSet?) {
        val ta = context.obtainStyledAttributes(attrs, R.styleable.UIGradientTextView)
        startColor = ta.getColor(R.styleable.UIGradientTextView_startColor, Color.RED)
        endColor = ta.getColor(R.styleable.UIGradientTextView_endColor, Color.BLUE)
        
        if (ta.hasValue(R.styleable.UIGradientTextView_centerColor)) {
            centerColor = ta.getColor(R.styleable.UIGradientTextView_centerColor, Color.TRANSPARENT)
        }
        centerPosition = ta.getFloat(R.styleable.UIGradientTextView_centerPosition, 0.5f)
        
        ta.recycle()
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        if (changed) {
            val shader = if (centerColor != null) {
                // 三色渐变
                LinearGradient(
                    0f,
                    0f,
                    width.toFloat(),
                    height.toFloat(),
                    intArrayOf(startColor, centerColor!!, endColor),
                    floatArrayOf(0f, centerPosition.coerceIn(0f, 1f), 1f),
                    Shader.TileMode.CLAMP
                )
            } else {
                // 双色渐变
                LinearGradient(
                    0f,
                    0f,
                    width.toFloat(),
                    height.toFloat(),
                    startColor,
                    endColor,
                    Shader.TileMode.CLAMP
                )
            }
            paint.setShader(shader)
        }
    }
}
