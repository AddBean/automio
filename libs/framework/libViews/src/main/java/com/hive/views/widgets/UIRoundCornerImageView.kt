// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.views.widgets

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import androidx.annotation.ColorInt
import androidx.core.content.withStyledAttributes
import com.hive.views.R
import com.hive.views.utils.AspectRatioConfig
import com.hive.views.utils.AspectRatioHelper
import com.hive.views.utils.RoundCornerHelper

@SuppressLint("AppCompatCustomView")
class UIRoundCornerImageView : AppCompatImageView {
    private var borderWidth: Int = 0
    private var borderColor: Int = Color.WHITE
    private var isCircle: Boolean = false
    private var radiusLb: Int = 0
    private var radiusLt: Int = 0
    private var radiusRb: Int = 0
    private var radiusRt: Int = 0
    private var radius: Int = 0
    private var roundCornerHelper: RoundCornerHelper? = null
    private var strokeShader: Shader? = null
    private var borderGradientStartColor: Int = RoundCornerHelper.COLOR_UNSET
    private var borderGradientEndColor: Int = RoundCornerHelper.COLOR_UNSET

    private var aspectRatioConfig: AspectRatioConfig? = null

    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle) {
        init(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context, attrs)
    }

    constructor(context: Context) : super(context) {
        init(context, null)
    }

    private fun init(context: Context, attrs: AttributeSet?) {
        if (attrs != null) {
            context.withStyledAttributes(attrs, R.styleable.UIRoundCornerImageView) {
                isCircle = getBoolean(R.styleable.UIRoundCornerImageView_rcv_isCircle, isCircle)
                radius = getDimensionPixelSize(R.styleable.UIRoundCornerImageView_rcv_radius, radius)
                radiusLb = getDimensionPixelSize(R.styleable.UIRoundCornerImageView_rcv_radius_LeftBottom, radius)
                radiusLt = getDimensionPixelSize(R.styleable.UIRoundCornerImageView_rcv_radius_LeftTop, radius)
                radiusRb = getDimensionPixelSize(R.styleable.UIRoundCornerImageView_rcv_radius_RightBottom, radius)
                radiusRt = getDimensionPixelSize(R.styleable.UIRoundCornerImageView_rcv_radius_RightTop, radius)
                borderWidth = getDimensionPixelSize(R.styleable.UIRoundCornerImageView_rcv_borderWidth, borderWidth)
                borderColor = getColor(R.styleable.UIRoundCornerImageView_rcv_borderColor, borderColor)
                borderGradientStartColor = getColor(
                    R.styleable.UIRoundCornerImageView_rcv_borderGradientStartColor,
                    RoundCornerHelper.COLOR_UNSET,
                )
                borderGradientEndColor = getColor(
                    R.styleable.UIRoundCornerImageView_rcv_borderGradientEndColor,
                    RoundCornerHelper.COLOR_UNSET,
                )
                
                val aspectRatioStr = getString(R.styleable.UIRoundCornerImageView_rcv_aspectRatio)
                aspectRatioConfig = AspectRatioHelper.parseAspectRatio(aspectRatioStr)
            }
        }
        roundCornerHelper = RoundCornerHelper()
        roundCornerHelper?.init()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val (widthSpec, heightSpec) = AspectRatioHelper.getAdjustedMeasureSpecs(
            aspectRatioConfig,
            widthMeasureSpec,
            heightMeasureSpec,
        )
        super.onMeasure(widthSpec, heightSpec)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        reConfigRoundCorner()
    }

    override fun setPadding(left: Int, top: Int, right: Int, bottom: Int) {
        super.setPadding(left, top, right, bottom)
        reConfigRoundCorner()
    }

    override fun draw(canvas: Canvas) {
        roundCornerHelper?.preDraw(canvas)
        super.draw(canvas)
        roundCornerHelper?.postDraw(canvas)
    }

    fun setBorderWidth(borderWidth: Int): UIRoundCornerImageView {
        this.borderWidth = borderWidth
        reConfigRoundCorner()
        return this
    }

    fun setBorderColor(borderColor: Int): UIRoundCornerImageView {
        this.borderColor = borderColor
        reConfigRoundCorner()
        return this
    }

    fun setCircle(circle: Boolean): UIRoundCornerImageView {
        isCircle = circle
        reConfigRoundCorner()
        return this
    }

    fun setRadiusLb(radiusLb: Int): UIRoundCornerImageView {
        this.radiusLb = radiusLb
        reConfigRoundCorner()
        return this
    }

    fun setRadiusLt(radiusLt: Int): UIRoundCornerImageView {
        this.radiusLt = radiusLt
        reConfigRoundCorner()
        return this
    }

    fun setRadiusRb(radiusRb: Int): UIRoundCornerImageView {
        this.radiusRb = radiusRb
        reConfigRoundCorner()
        return this
    }

    fun setRadiusRt(radiusRt: Int): UIRoundCornerImageView {
        this.radiusRt = radiusRt
        reConfigRoundCorner()
        return this
    }

    fun setRadius(radius: Int): UIRoundCornerImageView {
        radiusLt = radius
        radiusLb = radius
        radiusRt = radius
        radiusRb = radius
        this.radius = radius
        reConfigRoundCorner()
        return this
    }

    fun setBorderShader(shader: Shader?): UIRoundCornerImageView {
        strokeShader = shader
        reConfigRoundCorner()
        return this
    }

    fun setBorderVerticalGradient(
        @ColorInt startColor: Int,
        @ColorInt endColor: Int,
    ): UIRoundCornerImageView {
        strokeShader = null
        borderGradientStartColor = startColor
        borderGradientEndColor = endColor
        reConfigRoundCorner()
        return this
    }

    private fun reConfigRoundCorner() {
        if (isCircle) {
            val radius = minOf(
                width - paddingLeft - paddingRight,
                height - paddingTop - paddingBottom,
            ) / 2
            radiusLt = radius
            radiusLb = radius
            radiusRt = radius
            radiusRb = radius
        }

        val gradientStart = borderGradientStartColor.takeIf { it != RoundCornerHelper.COLOR_UNSET }
        val gradientEnd = borderGradientEndColor.takeIf { it != RoundCornerHelper.COLOR_UNSET }

        roundCornerHelper?.config(
            RectF(
                paddingLeft.toFloat(),
                paddingTop.toFloat(),
                (width - paddingRight).toFloat(),
                (height - paddingBottom).toFloat(),
            ),
            RectF(0f, 0f, width.toFloat(), height.toFloat()),
            floatArrayOf(radiusLt.toFloat(), radiusRt.toFloat(), radiusRb.toFloat(), radiusLb.toFloat()),
            borderWidth,
            borderColor,
            strokeShader,
            gradientStart,
            gradientEnd,
        )
        invalidate()
    }
}
