// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.views.widgets

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.annotation.ColorInt
import androidx.core.content.withStyledAttributes
import com.hive.views.R
import com.hive.views.utils.AspectRatioConfig
import com.hive.views.utils.AspectRatioHelper
import com.hive.views.utils.RoundCornerHelper

open class UIRoundCornerFrameLayout : FrameLayout {
    private var mBorderWidth = 0
    private var mBorderColor = Color.WHITE
    private var mBorderGradientStartColor = RoundCornerHelper.COLOR_UNSET
    private var mBorderGradientEndColor = RoundCornerHelper.COLOR_UNSET
    private var mRadiusLB = 0
    private var mRadiusLT = 0
    private var mRadiusRB = 0
    private var mRadiusRT = 0
    private var mRadius = 0
    private var mRoundCornerHelper: RoundCornerHelper? = null

    private var mStrokeShader: Shader? = null

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
            context.withStyledAttributes(attrs, R.styleable.UIRoundCornerFrameLayout) {
                mRadius = getDimensionPixelSize(R.styleable.UIRoundCornerFrameLayout_rcv_radius, mRadius)
                mRadiusLB = getDimensionPixelSize(R.styleable.UIRoundCornerFrameLayout_rcv_radius_LeftBottom, mRadius)
                mRadiusLT = getDimensionPixelSize(R.styleable.UIRoundCornerFrameLayout_rcv_radius_LeftTop, mRadius)
                mRadiusRB = getDimensionPixelSize(R.styleable.UIRoundCornerFrameLayout_rcv_radius_RightBottom, mRadius)
                mRadiusRT = getDimensionPixelSize(R.styleable.UIRoundCornerFrameLayout_rcv_radius_RightTop, mRadius)
                mBorderWidth = getDimensionPixelSize(R.styleable.UIRoundCornerFrameLayout_rcv_borderWidth, mBorderWidth)
                mBorderColor = getColor(R.styleable.UIRoundCornerFrameLayout_rcv_borderColor, mBorderColor)
                mBorderGradientStartColor = getColor(
                    R.styleable.UIRoundCornerFrameLayout_rcv_borderGradientStartColor,
                    RoundCornerHelper.COLOR_UNSET,
                )
                mBorderGradientEndColor = getColor(
                    R.styleable.UIRoundCornerFrameLayout_rcv_borderGradientEndColor,
                    RoundCornerHelper.COLOR_UNSET,
                )
                
                val aspectRatioStr = getString(R.styleable.UIRoundCornerFrameLayout_rcv_aspectRatio)
                aspectRatioConfig = AspectRatioHelper.parseAspectRatio(aspectRatioStr)
            }
        }
        mRoundCornerHelper = RoundCornerHelper()
        mRoundCornerHelper!!.init()
        setWillNotDraw(false)
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

    override fun draw(canvas: Canvas) {
        mRoundCornerHelper!!.preDraw(canvas)
        super.draw(canvas)
        mRoundCornerHelper!!.postDraw(canvas)
    }

    fun setBorderWidth(borderWidth: Int): UIRoundCornerFrameLayout {
        mBorderWidth = borderWidth
        reConfigRoundCorner()
        return this
    }

    fun setBorderColor(borderColor: Int): UIRoundCornerFrameLayout {
        mBorderColor = borderColor
        reConfigRoundCorner()
        return this
    }

    fun setRadiusLB(radiusLB: Int): UIRoundCornerFrameLayout {
        mRadiusLB = radiusLB
        reConfigRoundCorner()
        return this
    }

    fun setRadiusLT(radiusLT: Int): UIRoundCornerFrameLayout {
        mRadiusLT = radiusLT
        reConfigRoundCorner()
        return this
    }

    fun setRadiusRB(radiusRB: Int): UIRoundCornerFrameLayout {
        mRadiusRB = radiusRB
        reConfigRoundCorner()
        return this
    }

    fun setRadiusRT(radiusRT: Int): UIRoundCornerFrameLayout {
        mRadiusRT = radiusRT
        reConfigRoundCorner()
        return this
    }

    fun setRadius(radius: Int): UIRoundCornerFrameLayout {
        mRadius = radius
        mRadiusRB = mRadius
        mRadiusRT = mRadiusRB
        mRadiusLB = mRadiusRT
        mRadiusLT = mRadiusLB
        reConfigRoundCorner()
        return this
    }

    fun setBorderShader(shader: Shader?): UIRoundCornerFrameLayout {
        mStrokeShader = shader
        reConfigRoundCorner()
        return this
    }

    fun setBorderVerticalGradient(
        @ColorInt startColor: Int,
        @ColorInt endColor: Int,
    ): UIRoundCornerFrameLayout {
        mStrokeShader = null
        mBorderGradientStartColor = startColor
        mBorderGradientEndColor = endColor
        reConfigRoundCorner()
        return this
    }

    private fun reConfigRoundCorner() {
        val gradientStart = mBorderGradientStartColor.takeIf { it != RoundCornerHelper.COLOR_UNSET }
        val gradientEnd = mBorderGradientEndColor.takeIf { it != RoundCornerHelper.COLOR_UNSET }
        mRoundCornerHelper!!.config(
            RectF(0f, 0f, width.toFloat(), height.toFloat()),
            RectF(0f, 0f, width.toFloat(), height.toFloat()),
            floatArrayOf(mRadiusLT.toFloat(), mRadiusRT.toFloat(), mRadiusRB.toFloat(), mRadiusLB.toFloat()),
            mBorderWidth,
            mBorderColor,
            mStrokeShader,
            gradientStart,
            gradientEnd,
        )
        invalidate()
    }
}
