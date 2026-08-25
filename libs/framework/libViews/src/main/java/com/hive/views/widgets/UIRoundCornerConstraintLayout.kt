// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.views.widgets

import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.view.View
import androidx.annotation.ColorInt
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.withStyledAttributes
import androidx.core.view.ViewCompat
import com.hive.views.R
import com.hive.views.utils.AspectRatioConfig
import com.hive.views.utils.AspectRatioHelper
import com.hive.views.utils.RoundCornerHelper

/**
 * 支持圆角、边框和外侧阴影的 ConstraintLayout
 * 
 * @author machao.mc
 */
open class UIRoundCornerConstraintLayout : ConstraintLayout {
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

    // 阴影相关
    private var mShadowColor: Int = 0x80000000.toInt()
    private var mShadowBlur: Float = 0f
    private var mShadowRadius: Float = 0f
    private val mShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private var mTopGradient: LinearGradient? = null
    private var mBottomGradient: LinearGradient? = null
    private var mLeftGradient: LinearGradient? = null
    private var mRightGradient: LinearGradient? = null
    private var mTopLeftGradient: RadialGradient? = null
    private var mTopRightGradient: RadialGradient? = null
    private var mBottomLeftGradient: RadialGradient? = null
    private var mBottomRightGradient: RadialGradient? = null

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
            context.withStyledAttributes(attrs, R.styleable.UIRoundCornerConstraintLayout) {
                mRadius = getDimensionPixelSize(R.styleable.UIRoundCornerConstraintLayout_rcv_radius, mRadius)
                mRadiusLB =
                    getDimensionPixelSize(R.styleable.UIRoundCornerConstraintLayout_rcv_radius_LeftBottom, mRadius)
                mRadiusLT = getDimensionPixelSize(R.styleable.UIRoundCornerConstraintLayout_rcv_radius_LeftTop, mRadius)
                mRadiusRB =
                    getDimensionPixelSize(R.styleable.UIRoundCornerConstraintLayout_rcv_radius_RightBottom, mRadius)
                mRadiusRT = getDimensionPixelSize(R.styleable.UIRoundCornerConstraintLayout_rcv_radius_RightTop, mRadius)
                mBorderWidth =
                    getDimensionPixelSize(R.styleable.UIRoundCornerConstraintLayout_rcv_borderWidth, mBorderWidth)
                mBorderColor = getColor(R.styleable.UIRoundCornerConstraintLayout_rcv_borderColor, mBorderColor)
                mBorderGradientStartColor = getColor(
                    R.styleable.UIRoundCornerConstraintLayout_rcv_borderGradientStartColor,
                    RoundCornerHelper.COLOR_UNSET,
                )
                mBorderGradientEndColor = getColor(
                    R.styleable.UIRoundCornerConstraintLayout_rcv_borderGradientEndColor,
                    RoundCornerHelper.COLOR_UNSET,
                )
                
                // 读取阴影属性
                mShadowColor = getColor(R.styleable.UIRoundCornerConstraintLayout_rcv_shadowColor, mShadowColor)
                mShadowBlur = getDimension(R.styleable.UIRoundCornerConstraintLayout_rcv_shadowBlur, 0f)
                mShadowRadius = mShadowBlur
                
                val aspectRatioStr = getString(R.styleable.UIRoundCornerConstraintLayout_rcv_aspectRatio)
                aspectRatioConfig = AspectRatioHelper.parseAspectRatio(aspectRatioStr)
            }
        }
        mRoundCornerHelper = RoundCornerHelper()
        mRoundCornerHelper!!.init()
        setWillNotDraw(false)
        
        // 设置阴影 padding
        updateShadowPadding()
        
//        // 启用软件渲染以支持阴影绘制
//        if (mShadowBlur > 0) {
////            ViewCompat.setLayerType(this, View.LAYER_TYPE_SOFTWARE, null)
//        }
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
        updateShadowGradients()
        reConfigRoundCorner()
    }

    override fun draw(canvas: Canvas) {
        // 先绘制外侧阴影
        drawOuterShadow(canvas)
        
        mRoundCornerHelper!!.preDraw(canvas)
        super.draw(canvas)
        mRoundCornerHelper!!.postDraw(canvas)
    }

    fun setBorderWidth(borderWidth: Int): UIRoundCornerConstraintLayout {
        mBorderWidth = borderWidth
        reConfigRoundCorner()
        return this
    }

    fun setBorderColor(borderColor: Int): UIRoundCornerConstraintLayout {
        mBorderColor = borderColor
        reConfigRoundCorner()
        return this
    }

    fun setRadiusLB(radiusLB: Int): UIRoundCornerConstraintLayout {
        mRadiusLB = radiusLB
        updateShadowGradients()
        reConfigRoundCorner()
        return this
    }

    fun setRadiusLT(radiusLT: Int): UIRoundCornerConstraintLayout {
        mRadiusLT = radiusLT
        updateShadowGradients()
        reConfigRoundCorner()
        return this
    }

    fun setRadiusRB(radiusRB: Int): UIRoundCornerConstraintLayout {
        mRadiusRB = radiusRB
        updateShadowGradients()
        reConfigRoundCorner()
        return this
    }

    fun setRadiusRT(radiusRT: Int): UIRoundCornerConstraintLayout {
        mRadiusRT = radiusRT
        updateShadowGradients()
        reConfigRoundCorner()
        return this
    }

    fun setRadius(radius: Int): UIRoundCornerConstraintLayout {
        mRadius = radius
        mRadiusRB = mRadius
        mRadiusRT = mRadiusRB
        mRadiusLB = mRadiusRT
        mRadiusLT = mRadiusLB
        updateShadowGradients()
        reConfigRoundCorner()
        return this
    }

    fun setBorderShader(shader: Shader?): UIRoundCornerConstraintLayout {
        mStrokeShader = shader
        reConfigRoundCorner()
        return this
    }

    fun setBorderVerticalGradient(
        @ColorInt startColor: Int,
        @ColorInt endColor: Int,
    ): UIRoundCornerConstraintLayout {
        mStrokeShader = null
        mBorderGradientStartColor = startColor
        mBorderGradientEndColor = endColor
        reConfigRoundCorner()
        return this
    }

    /**
     * 设置阴影颜色
     */
    fun setShadowColor(color: Int): UIRoundCornerConstraintLayout {
        if (mShadowColor != color) {
            mShadowColor = color
            updateShadowGradients()
            invalidate()
        }
        return this
    }

    /**
     * 设置阴影模糊半径
     */
    fun setShadowBlur(blur: Float): UIRoundCornerConstraintLayout {
        if (mShadowBlur != blur) {
            mShadowBlur = blur
            mShadowRadius = blur
            if (blur > 0) {
                ViewCompat.setLayerType(this, View.LAYER_TYPE_SOFTWARE, null)
            }
            updateShadowPadding()
            updateShadowGradients()
            invalidate()
        }
        return this
    }

    private fun updateShadowPadding() {
        val padding = if (mShadowRadius > 0) mShadowRadius.toInt() else return
        setPadding(padding, padding, padding, padding)
    }

    private fun updateShadowGradients() {
        if (width <= 0 || height <= 0 || mShadowRadius <= 0) {
            return
        }

        val padding = mShadowRadius.toInt()
        val contentLeft = padding.toFloat()
        val contentTop = padding.toFloat()
        val contentRight = (width - padding).toFloat()
        val contentBottom = (height - padding).toFloat()

        val shadowColorWithAlpha = mShadowColor
        val transparentColor = mShadowColor and 0x00FFFFFF

        // 顶部渐变
        mTopGradient = LinearGradient(
            contentLeft, contentTop,
            contentLeft, 0f,
            intArrayOf(shadowColorWithAlpha, transparentColor),
            null,
            Shader.TileMode.CLAMP
        )

        // 底部渐变
        mBottomGradient = LinearGradient(
            contentLeft, contentBottom,
            contentLeft, height.toFloat(),
            intArrayOf(shadowColorWithAlpha, transparentColor),
            null,
            Shader.TileMode.CLAMP
        )

        // 左侧渐变
        mLeftGradient = LinearGradient(
            contentLeft, contentTop,
            0f, contentTop,
            intArrayOf(shadowColorWithAlpha, transparentColor),
            null,
            Shader.TileMode.CLAMP
        )

        // 右侧渐变
        mRightGradient = LinearGradient(
            contentRight, contentTop,
            width.toFloat(), contentTop,
            intArrayOf(shadowColorWithAlpha, transparentColor),
            null,
            Shader.TileMode.CLAMP
        )

        // 四个角使用圆环渐变（从圆角边缘到外侧）
        // 左上角：圆心在 (contentLeft + radiusLT, contentTop + radiusLT)
        if (mRadiusLT > 0) {
            val radiusLTOuter = mRadiusLT.toFloat() + mShadowRadius
            mTopLeftGradient = RadialGradient(
                contentLeft + mRadiusLT.toFloat(),
                contentTop + mRadiusLT.toFloat(),
                radiusLTOuter,
                intArrayOf(shadowColorWithAlpha, shadowColorWithAlpha, transparentColor),
                floatArrayOf(0f, mRadiusLT.toFloat() / radiusLTOuter, 1f),
                Shader.TileMode.CLAMP
            )
        } else {
            mTopLeftGradient = RadialGradient(
                contentLeft, contentTop,
                mShadowRadius,
                intArrayOf(shadowColorWithAlpha, transparentColor),
                null,
                Shader.TileMode.CLAMP
            )
        }

        // 右上角：圆心在 (contentRight - radiusRT, contentTop + radiusRT)
        if (mRadiusRT > 0) {
            val radiusRTOuter = mRadiusRT.toFloat() + mShadowRadius
            mTopRightGradient = RadialGradient(
                contentRight - mRadiusRT.toFloat(),
                contentTop + mRadiusRT.toFloat(),
                radiusRTOuter,
                intArrayOf(shadowColorWithAlpha, shadowColorWithAlpha, transparentColor),
                floatArrayOf(0f, mRadiusRT.toFloat() / radiusRTOuter, 1f),
                Shader.TileMode.CLAMP
            )
        } else {
            mTopRightGradient = RadialGradient(
                contentRight, contentTop,
                mShadowRadius,
                intArrayOf(shadowColorWithAlpha, transparentColor),
                null,
                Shader.TileMode.CLAMP
            )
        }

        // 左下角：圆心在 (contentLeft + radiusLB, contentBottom - radiusLB)
        if (mRadiusLB > 0) {
            val radiusLBOuter = mRadiusLB.toFloat() + mShadowRadius
            mBottomLeftGradient = RadialGradient(
                contentLeft + mRadiusLB.toFloat(),
                contentBottom - mRadiusLB.toFloat(),
                radiusLBOuter,
                intArrayOf(shadowColorWithAlpha, shadowColorWithAlpha, transparentColor),
                floatArrayOf(0f, mRadiusLB.toFloat() / radiusLBOuter, 1f),
                Shader.TileMode.CLAMP
            )
        } else {
            mBottomLeftGradient = RadialGradient(
                contentLeft, contentBottom,
                mShadowRadius,
                intArrayOf(shadowColorWithAlpha, transparentColor),
                null,
                Shader.TileMode.CLAMP
            )
        }

        // 右下角：圆心在 (contentRight - radiusRB, contentBottom - radiusRB)
        if (mRadiusRB > 0) {
            val radiusRBOuter = mRadiusRB.toFloat() + mShadowRadius
            mBottomRightGradient = RadialGradient(
                contentRight - mRadiusRB.toFloat(),
                contentBottom - mRadiusRB.toFloat(),
                radiusRBOuter,
                intArrayOf(shadowColorWithAlpha, shadowColorWithAlpha, transparentColor),
                floatArrayOf(0f, mRadiusRB.toFloat() / radiusRBOuter, 1f),
                Shader.TileMode.CLAMP
            )
        } else {
            mBottomRightGradient = RadialGradient(
                contentRight, contentBottom,
                mShadowRadius,
                intArrayOf(shadowColorWithAlpha, transparentColor),
                null,
                Shader.TileMode.CLAMP
            )
        }
    }

    private fun drawOuterShadow(canvas: Canvas) {
        if (mShadowRadius <= 0) {
            return
        }

        val padding = mShadowRadius.toInt()
        val contentLeft = padding.toFloat()
        val contentTop = padding.toFloat()
        val contentRight = (width - padding).toFloat()
        val contentBottom = (height - padding).toFloat()

        // 如果有圆角，使用圆角阴影绘制
        if (mRadiusLT > 0 || mRadiusRT > 0 || mRadiusRB > 0 || mRadiusLB > 0) {
            drawRoundedShadow(canvas, contentLeft, contentTop, contentRight, contentBottom)
        } else {
            drawRectShadow(canvas, contentLeft, contentTop, contentRight, contentBottom)
        }

        mShadowPaint.shader = null
    }

    /**
     * 绘制矩形阴影
     */
    private fun drawRectShadow(
        canvas: Canvas,
        contentLeft: Float,
        contentTop: Float,
        contentRight: Float,
        contentBottom: Float
    ) {
        // 四个角阴影
        mTopLeftGradient?.let {
            mShadowPaint.shader = it
            canvas.drawRect(0f, 0f, contentLeft, contentTop, mShadowPaint)
        }

        mTopRightGradient?.let {
            mShadowPaint.shader = it
            canvas.drawRect(contentRight, 0f, width.toFloat(), contentTop, mShadowPaint)
        }

        mBottomLeftGradient?.let {
            mShadowPaint.shader = it
            canvas.drawRect(0f, contentBottom, contentLeft, height.toFloat(), mShadowPaint)
        }

        mBottomRightGradient?.let {
            mShadowPaint.shader = it
            canvas.drawRect(contentRight, contentBottom, width.toFloat(), height.toFloat(), mShadowPaint)
        }

        // 四边阴影
        mTopGradient?.let {
            mShadowPaint.shader = it
            canvas.drawRect(contentLeft, 0f, contentRight, contentTop, mShadowPaint)
        }

        mBottomGradient?.let {
            mShadowPaint.shader = it
            canvas.drawRect(contentLeft, contentBottom, contentRight, height.toFloat(), mShadowPaint)
        }

        mLeftGradient?.let {
            mShadowPaint.shader = it
            canvas.drawRect(0f, contentTop, contentLeft, contentBottom, mShadowPaint)
        }

        mRightGradient?.let {
            mShadowPaint.shader = it
            canvas.drawRect(contentRight, contentTop, width.toFloat(), contentBottom, mShadowPaint)
        }
    }

    /**
     * 绘制圆角矩形阴影
     */
    private fun drawRoundedShadow(
        canvas: Canvas,
        contentLeft: Float,
        contentTop: Float,
        contentRight: Float,
        contentBottom: Float
    ) {
        // 创建内容区域路径用于裁剪
        val contentPath = Path()
        val contentRect = RectF(contentLeft, contentTop, contentRight, contentBottom)
        val contentRadii = floatArrayOf(
            mRadiusLT.toFloat(), mRadiusLT.toFloat(),
            mRadiusRT.toFloat(), mRadiusRT.toFloat(),
            mRadiusRB.toFloat(), mRadiusRB.toFloat(),
            mRadiusLB.toFloat(), mRadiusLB.toFloat()
        )
        contentPath.addRoundRect(contentRect, contentRadii, Path.Direction.CW)

        // 先绘制四边阴影（避开圆角区域）
        // 计算每条边的起止位置，避开圆角
        val topLeftCornerX = contentLeft + mRadiusLT.toFloat()
        val topRightCornerX = contentRight - mRadiusRT.toFloat()
        val bottomLeftCornerX = contentLeft + mRadiusLB.toFloat()
        val bottomRightCornerX = contentRight - mRadiusRB.toFloat()
        
        val leftTopCornerY = contentTop + mRadiusLT.toFloat()
        val leftBottomCornerY = contentBottom - mRadiusLB.toFloat()
        val rightTopCornerY = contentTop + mRadiusRT.toFloat()
        val rightBottomCornerY = contentBottom - mRadiusRB.toFloat()
        
        // 顶部边阴影（从左上角圆心到右上角圆心）
        mTopGradient?.let {
            mShadowPaint.shader = it
            if (topLeftCornerX < topRightCornerX) {
                canvas.drawRect(topLeftCornerX, 0f, topRightCornerX, contentTop, mShadowPaint)
            }
        }

        // 底部边阴影（从左下角圆心到右下角圆心）
        mBottomGradient?.let {
            mShadowPaint.shader = it
            if (bottomLeftCornerX < bottomRightCornerX) {
                canvas.drawRect(bottomLeftCornerX, contentBottom, bottomRightCornerX, height.toFloat(), mShadowPaint)
            }
        }

        // 左侧边阴影（从左上角圆心到左下角圆心）
        mLeftGradient?.let {
            mShadowPaint.shader = it
            if (leftTopCornerY < leftBottomCornerY) {
                canvas.drawRect(0f, leftTopCornerY, contentLeft, leftBottomCornerY, mShadowPaint)
            }
        }

        // 右侧边阴影（从右上角圆心到右下角圆心）
        mRightGradient?.let {
            mShadowPaint.shader = it
            if (rightTopCornerY < rightBottomCornerY) {
                canvas.drawRect(contentRight, rightTopCornerY, width.toFloat(), rightBottomCornerY, mShadowPaint)
            }
        }

        // 再绘制四个角的扇形渐变（覆盖在边上，确保圆滑过渡）
        // 左上角 - 绘制圆环扇形
        mTopLeftGradient?.let {
            mShadowPaint.shader = it
            if (mRadiusLT > 0) {
                val centerX = contentLeft + mRadiusLT.toFloat()
                val centerY = contentTop + mRadiusLT.toFloat()
                val outerRadius = mRadiusLT.toFloat() + mShadowRadius
                val innerRadius = mRadiusLT.toFloat()
                
                // 外部扇形
                val outerPath = Path().apply {
                    moveTo(centerX, centerY)
                    arcTo(
                        RectF(centerX - outerRadius, centerY - outerRadius,
                              centerX + outerRadius, centerY + outerRadius),
                        180f, 90f
                    )
                    lineTo(centerX, centerY)
                    close()
                }
                
                // 内部扇形（要减去的部分）
                val innerPath = Path().apply {
                    moveTo(centerX, centerY)
                    arcTo(
                        RectF(centerX - innerRadius, centerY - innerRadius,
                              centerX + innerRadius, centerY + innerRadius),
                        180f, 90f
                    )
                    lineTo(centerX, centerY)
                    close()
                }
                
                // 绘制圆环扇形
                outerPath.op(innerPath, Path.Op.DIFFERENCE)
                canvas.drawPath(outerPath, mShadowPaint)
            } else {
                canvas.drawRect(0f, 0f, contentLeft, contentTop, mShadowPaint)
            }
        }

        // 右上角 - 绘制圆环扇形
        mTopRightGradient?.let {
            mShadowPaint.shader = it
            if (mRadiusRT > 0) {
                val centerX = contentRight - mRadiusRT.toFloat()
                val centerY = contentTop + mRadiusRT.toFloat()
                val outerRadius = mRadiusRT.toFloat() + mShadowRadius
                val innerRadius = mRadiusRT.toFloat()
                
                val outerPath = Path().apply {
                    moveTo(centerX, centerY)
                    arcTo(
                        RectF(centerX - outerRadius, centerY - outerRadius,
                              centerX + outerRadius, centerY + outerRadius),
                        270f, 90f
                    )
                    lineTo(centerX, centerY)
                    close()
                }
                
                val innerPath = Path().apply {
                    moveTo(centerX, centerY)
                    arcTo(
                        RectF(centerX - innerRadius, centerY - innerRadius,
                              centerX + innerRadius, centerY + innerRadius),
                        270f, 90f
                    )
                    lineTo(centerX, centerY)
                    close()
                }
                
                outerPath.op(innerPath, Path.Op.DIFFERENCE)
                canvas.drawPath(outerPath, mShadowPaint)
            } else {
                canvas.drawRect(contentRight, 0f, width.toFloat(), contentTop, mShadowPaint)
            }
        }

        // 右下角 - 绘制圆环扇形
        mBottomRightGradient?.let {
            mShadowPaint.shader = it
            if (mRadiusRB > 0) {
                val centerX = contentRight - mRadiusRB.toFloat()
                val centerY = contentBottom - mRadiusRB.toFloat()
                val outerRadius = mRadiusRB.toFloat() + mShadowRadius
                val innerRadius = mRadiusRB.toFloat()
                
                val outerPath = Path().apply {
                    moveTo(centerX, centerY)
                    arcTo(
                        RectF(centerX - outerRadius, centerY - outerRadius,
                              centerX + outerRadius, centerY + outerRadius),
                        0f, 90f
                    )
                    lineTo(centerX, centerY)
                    close()
                }
                
                val innerPath = Path().apply {
                    moveTo(centerX, centerY)
                    arcTo(
                        RectF(centerX - innerRadius, centerY - innerRadius,
                              centerX + innerRadius, centerY + innerRadius),
                        0f, 90f
                    )
                    lineTo(centerX, centerY)
                    close()
                }
                
                outerPath.op(innerPath, Path.Op.DIFFERENCE)
                canvas.drawPath(outerPath, mShadowPaint)
            } else {
                canvas.drawRect(contentRight, contentBottom, width.toFloat(), height.toFloat(), mShadowPaint)
            }
        }

        // 左下角 - 绘制圆环扇形
        mBottomLeftGradient?.let {
            mShadowPaint.shader = it
            if (mRadiusLB > 0) {
                val centerX = contentLeft + mRadiusLB.toFloat()
                val centerY = contentBottom - mRadiusLB.toFloat()
                val outerRadius = mRadiusLB.toFloat() + mShadowRadius
                val innerRadius = mRadiusLB.toFloat()
                
                val outerPath = Path().apply {
                    moveTo(centerX, centerY)
                    arcTo(
                        RectF(centerX - outerRadius, centerY - outerRadius,
                              centerX + outerRadius, centerY + outerRadius),
                        90f, 90f
                    )
                    lineTo(centerX, centerY)
                    close()
                }
                
                val innerPath = Path().apply {
                    moveTo(centerX, centerY)
                    arcTo(
                        RectF(centerX - innerRadius, centerY - innerRadius,
                              centerX + innerRadius, centerY + innerRadius),
                        90f, 90f
                    )
                    lineTo(centerX, centerY)
                    close()
                }
                
                outerPath.op(innerPath, Path.Op.DIFFERENCE)
                canvas.drawPath(outerPath, mShadowPaint)
            } else {
                canvas.drawRect(0f, contentBottom, contentLeft, height.toFloat(), mShadowPaint)
            }
        }
    }

    private fun reConfigRoundCorner() {
        if (width <= 0 || height <= 0) {
            return
        }
        
        // 计算内容区域（排除阴影占用的 padding）
        val shadowPadding = mShadowRadius
        val contentLeft = shadowPadding
        val contentTop = shadowPadding
        val contentRight = width - shadowPadding
        val contentBottom = height - shadowPadding
        
        val gradientStart = mBorderGradientStartColor.takeIf { it != RoundCornerHelper.COLOR_UNSET }
        val gradientEnd = mBorderGradientEndColor.takeIf { it != RoundCornerHelper.COLOR_UNSET }

        mRoundCornerHelper!!.config(
            RectF(contentLeft, contentTop, contentRight, contentBottom),
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
