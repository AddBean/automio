// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.views.widgets

import android.graphics.RectF
import android.graphics.Shader
import android.widget.LinearLayout
import androidx.annotation.ColorInt
import androidx.core.content.withStyledAttributes
import com.hive.utils.debug.DLog
import com.hive.views.R
import com.hive.views.utils.AspectRatioConfig
import com.hive.views.utils.AspectRatioHelper
import com.hive.views.utils.RoundCornerHelper

class UIRoundCornerLinearLayout : LinearLayout {
    private var mBorderWidth = 0
    private var mBorderColor = android.graphics.Color.WHITE
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

    constructor(context: android.content.Context, attrs: android.util.AttributeSet?, defStyle: kotlin.Int) : super(
        context,
        attrs,
        defStyle
    ) {
        init(context, attrs)
    }

    constructor(context: android.content.Context, attrs: android.util.AttributeSet?) : super(context, attrs) {
        init(context, attrs)
    }

    constructor(context: android.content.Context) : super(context) {
        init(context, null)
    }

    private fun init(context: android.content.Context, attrs: android.util.AttributeSet?) {
        if (attrs != null) {
            context.withStyledAttributes(attrs, R.styleable.UIRoundCornerLinearLayout) {
                mRadius = getDimensionPixelSize(R.styleable.UIRoundCornerLinearLayout_rcv_radius, mRadius)
                mRadiusLB = getDimensionPixelSize(R.styleable.UIRoundCornerLinearLayout_rcv_radius_LeftBottom, mRadius)
                mRadiusLT = getDimensionPixelSize(R.styleable.UIRoundCornerLinearLayout_rcv_radius_LeftTop, mRadius)
                mRadiusRB = getDimensionPixelSize(R.styleable.UIRoundCornerLinearLayout_rcv_radius_RightBottom, mRadius)
                mRadiusRT = getDimensionPixelSize(R.styleable.UIRoundCornerLinearLayout_rcv_radius_RightTop, mRadius)
                mBorderWidth = getDimensionPixelSize(R.styleable.UIRoundCornerLinearLayout_rcv_borderWidth, mBorderWidth)
                mBorderColor = getColor(R.styleable.UIRoundCornerLinearLayout_rcv_borderColor, mBorderColor)
                mBorderGradientStartColor = getColor(
                    R.styleable.UIRoundCornerLinearLayout_rcv_borderGradientStartColor,
                    RoundCornerHelper.COLOR_UNSET,
                )
                mBorderGradientEndColor = getColor(
                    R.styleable.UIRoundCornerLinearLayout_rcv_borderGradientEndColor,
                    RoundCornerHelper.COLOR_UNSET,
                )
                
                val aspectRatioStr = getString(R.styleable.UIRoundCornerLinearLayout_rcv_aspectRatio)
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

    override fun draw(canvas: android.graphics.Canvas) {
        mRoundCornerHelper!!.preDraw(canvas)
        try {
            super.draw(canvas)
        } catch (e: java.lang.Exception) {
            DLog.e("UIRoundCornerLinearLayout", "Exception", e)
        }
        mRoundCornerHelper!!.postDraw(canvas)
    }

    fun setBorderWidth(borderWidth: Int): UIRoundCornerLinearLayout {
        mBorderWidth = borderWidth
        reConfigRoundCorner()
        return this
    }

    fun setBorderColor(borderColor: Int): UIRoundCornerLinearLayout {
        mBorderColor = borderColor
        reConfigRoundCorner()
        return this
    }

    fun setRadiusLB(radiusLB: Int): UIRoundCornerLinearLayout {
        mRadiusLB = radiusLB
        reConfigRoundCorner()
        return this
    }

    fun setRadiusLT(radiusLT: Int): UIRoundCornerLinearLayout {
        mRadiusLT = radiusLT
        reConfigRoundCorner()
        return this
    }

    fun setRadiusRB(radiusRB: Int): UIRoundCornerLinearLayout {
        mRadiusRB = radiusRB
        reConfigRoundCorner()
        return this
    }

    fun setRadiusRT(radiusRT: Int): UIRoundCornerLinearLayout {
        mRadiusRT = radiusRT
        reConfigRoundCorner()
        return this
    }

    fun setRadius(radius: Int): UIRoundCornerLinearLayout {
        mRadius = radius
        mRadiusRB = mRadius
        mRadiusRT = mRadiusRB
        mRadiusLB = mRadiusRT
        mRadiusLT = mRadiusLB
        reConfigRoundCorner()
        return this
    }

    fun setBorderShader(shader: Shader?): UIRoundCornerLinearLayout {
        mStrokeShader = shader
        reConfigRoundCorner()
        return this
    }

    fun setBorderVerticalGradient(
        @ColorInt startColor: Int,
        @ColorInt endColor: Int,
    ): UIRoundCornerLinearLayout {
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
            kotlin.floatArrayOf(mRadiusLT.toFloat(), mRadiusRT.toFloat(), mRadiusRB.toFloat(), mRadiusLB.toFloat()),
            mBorderWidth,
            mBorderColor,
            mStrokeShader,
            gradientStart,
            gradientEnd,
        )
        invalidate()
    }
}
