// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.views.utils

import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.graphics.Shader
import androidx.annotation.ColorInt

/**
 * 圆角裁剪和描边工具类
 * author: machao.mc
 */
class RoundCornerHelper {
    // 描边内部区域
    private var mInnerRect: RectF? = null
    private var mOuterRect: RectF? = null
    private var mViewRect: RectF? = null
    private var mPaint: Paint? = null

    // 描边内部区域的圆角半径
    private var mInnerRadii: FloatArray? = null
    private var mOuterRadii: FloatArray? = null

    private var mCornerShadeLT: Path? = null
    private var mCornerShadeLB: Path? = null
    private var mCornerShadeRT: Path? = null
    private var mCornerShadeRB: Path? = null

    private var mStrokePaint: Paint? = null
    private var mStrokePath: Path? = null
    private var mStrokeWidth: Int = 0

    fun init() {
        mInnerRadii = FloatArray(4)
        mOuterRadii = FloatArray(4)
        mOuterRect = RectF()
        mInnerRect = RectF()
        mViewRect = RectF()
        mStrokePath = Path()
        mCornerShadeLT = Path()
        mCornerShadeLB = Path()
        mCornerShadeRT = Path()
        mCornerShadeRB = Path()

        mPaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
            xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT)
        }

        mStrokePaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
        }
    }

    /**
     * @param outerRect 圆角矩形的rect，以描边外边缘为基准
     * @param viewRect View自身的rect
     * @param radii 四角的圆角值，顺序:{左上、右上、右下、左下}
     * @param strokeWidth 描边宽度
     * @param strokeColor 描边颜色
     * @param strokeShader 描边渐变Shader（可选）
     * @param strokeGradientStartColor 垂直渐变起始色（可选，顶端）
     * @param strokeGradientEndColor 垂直渐变结束色（可选，底部）
     */
    fun config(
        outerRect: RectF,
        viewRect: RectF,
        radii: FloatArray,
        strokeWidth: Int,
        @ColorInt strokeColor: Int,
        strokeShader: Shader? = null,
        @ColorInt strokeGradientStartColor: Int? = null,
        @ColorInt strokeGradientEndColor: Int? = null,
    ) {
        require(radii.size == 4) { "radii must has 4 element" }

        mViewRect?.set(viewRect)
        // 描边不能大于短边一半
        mStrokeWidth = maxOf(
            minOf(strokeWidth.toFloat(), minOf(outerRect.width(), outerRect.height()) / 2.0f).toInt(),
            0,
        )
        mOuterRect?.set(outerRect)
        mInnerRect?.set(
            outerRect.left + mStrokeWidth,
            outerRect.top + mStrokeWidth,
            outerRect.right - mStrokeWidth,
            outerRect.bottom - mStrokeWidth,
        )

        mOuterRadii?.let { outerRadii ->
            outerRadii[0] = radii[0]
            outerRadii[1] = radii[1]
            outerRadii[2] = radii[2]
            outerRadii[3] = radii[3]
        }

        // 描边内边缘圆角半径不能小于0，也不能大于内部区域的短边一半
        mInnerRect?.let { innerRect ->
            mInnerRadii?.let { innerRadii ->
                innerRadii[0] = minOf(
                    minOf(innerRect.width(), innerRect.height()) / 2.0f,
                    maxOf(radii[0] - mStrokeWidth, 0f),
                )
                innerRadii[1] = minOf(
                    minOf(innerRect.width(), innerRect.height()) / 2.0f,
                    maxOf(radii[1] - mStrokeWidth, 0f),
                )
                innerRadii[2] = minOf(
                    minOf(innerRect.width(), innerRect.height()) / 2.0f,
                    maxOf(radii[2] - mStrokeWidth, 0f),
                )
                innerRadii[3] = minOf(
                    minOf(innerRect.width(), innerRect.height()) / 2.0f,
                    maxOf(radii[3] - mStrokeWidth, 0f),
                )
            }
        }

        // 左上角
        mCornerShadeLT?.let { cornerShadeLT ->
            mInnerRect?.let { innerRect ->
                mInnerRadii?.let { innerRadii ->
                    cornerShadeLT.reset()
                    cornerShadeLT.moveTo(0f, 0f)
                    cornerShadeLT.lineTo(0f, innerRect.top + innerRadii[0])
                    cornerShadeLT.lineTo(innerRect.left, innerRect.top + innerRadii[0])
                    cornerShadeLT.arcTo(
                        RectF(
                            innerRect.left,
                            innerRect.top,
                            innerRect.left + 2 * innerRadii[0],
                            innerRect.top + 2 * innerRadii[0],
                        ),
                        -180f,
                        90f,
                    )
                    cornerShadeLT.lineTo(innerRect.right - innerRadii[1], innerRect.top)
                    cornerShadeLT.lineTo(innerRect.right - innerRadii[1], 0f)
                    cornerShadeLT.close()
                }
            }
        }

        // 右上角
        mCornerShadeRT?.let { cornerShadeRT ->
            mInnerRect?.let { innerRect ->
                mInnerRadii?.let { innerRadii ->
                    mViewRect?.let { viewRect ->
                        cornerShadeRT.reset()
                        cornerShadeRT.moveTo(viewRect.right, 0f)
                        cornerShadeRT.lineTo(innerRect.right - innerRadii[1], 0f)
                        cornerShadeRT.lineTo(innerRect.right - innerRadii[1], innerRect.top)
                        cornerShadeRT.arcTo(
                            RectF(
                                innerRect.right - 2 * innerRadii[1],
                                innerRect.top,
                                innerRect.right,
                                innerRect.top + 2 * innerRadii[1],
                            ),
                            -90f,
                            90f,
                        )
                        cornerShadeRT.lineTo(innerRect.right, innerRect.bottom - innerRadii[2])
                        cornerShadeRT.lineTo(viewRect.right, innerRect.bottom - innerRadii[2])
                        cornerShadeRT.close()
                    }
                }
            }
        }

        // 右下角
        mCornerShadeRB?.let { cornerShadeRB ->
            mInnerRect?.let { innerRect ->
                mInnerRadii?.let { innerRadii ->
                    mViewRect?.let { viewRect ->
                        cornerShadeRB.reset()
                        cornerShadeRB.moveTo(viewRect.right, viewRect.bottom)
                        cornerShadeRB.lineTo(viewRect.right, innerRect.bottom - innerRadii[2])
                        cornerShadeRB.lineTo(innerRect.right, innerRect.bottom - innerRadii[2])
                        cornerShadeRB.arcTo(
                            RectF(
                                innerRect.right - 2 * innerRadii[2],
                                innerRect.bottom - 2 * innerRadii[2],
                                innerRect.right,
                                innerRect.bottom,
                            ),
                            0f,
                            90f,
                        )
                        cornerShadeRB.lineTo(innerRect.left + innerRadii[3], innerRect.bottom)
                        cornerShadeRB.lineTo(innerRect.left + innerRadii[3], viewRect.bottom)
                        cornerShadeRB.close()
                    }
                }
            }
        }

        // 左下角
        mCornerShadeLB?.let { cornerShadeLB ->
            mInnerRect?.let { innerRect ->
                mInnerRadii?.let { innerRadii ->
                    mViewRect?.let { viewRect ->
                        cornerShadeLB.reset()
                        cornerShadeLB.moveTo(0f, viewRect.bottom)
                        cornerShadeLB.lineTo(innerRect.left + innerRadii[3], viewRect.bottom)
                        cornerShadeLB.lineTo(innerRect.left + innerRadii[3], innerRect.bottom)
                        cornerShadeLB.arcTo(
                            RectF(
                                innerRect.left,
                                innerRect.bottom - 2 * innerRadii[3],
                                innerRect.left + 2 * innerRadii[3],
                                innerRect.bottom,
                            ),
                            90f,
                            90f,
                        )
                        cornerShadeLB.lineTo(innerRect.left, innerRect.top + innerRadii[0])
                        cornerShadeLB.lineTo(0f, innerRect.top + innerRadii[0])
                        cornerShadeLB.close()
                    }
                }
            }
        }

        mStrokePaint?.let { strokePaint ->
            val resolvedShader = buildVerticalStrokeShader(strokeShader, strokeGradientStartColor, strokeGradientEndColor)
            if (resolvedShader != null) {
                strokePaint.shader = resolvedShader
            } else {
                strokePaint.shader = null
                strokePaint.color = strokeColor
            }
            strokePaint.strokeWidth = mStrokeWidth.toFloat()
        }

        mStrokePath?.let { strokePath ->
            mInnerRect?.let { innerRect ->
                mOuterRect?.let { outerRect ->
                    mInnerRadii?.let { innerRadii ->
                        mOuterRadii?.let { outerRadii ->
                            strokePath.reset()
                            strokePath.addRoundRect(
                                innerRect,
                                floatArrayOf(
                                    innerRadii[0],
                                    innerRadii[0],
                                    innerRadii[1],
                                    innerRadii[1],
                                    innerRadii[2],
                                    innerRadii[2],
                                    innerRadii[3],
                                    innerRadii[3],
                                ),
                                Path.Direction.CCW,
                            )
                            strokePath.addRoundRect(
                                outerRect,
                                floatArrayOf(
                                    outerRadii[0],
                                    outerRadii[0],
                                    outerRadii[1],
                                    outerRadii[1],
                                    outerRadii[2],
                                    outerRadii[2],
                                    outerRadii[3],
                                    outerRadii[3],
                                ),
                                Path.Direction.CW,
                            )
                            strokePath.close()
                        }
                    }
                }
            }
        }
    }

    fun preDraw(canvas: Canvas) {
        canvas.saveLayer(null, null, Canvas.ALL_SAVE_FLAG)
    }

    fun postDraw(canvas: Canvas) {
        // round corner
        mPaint?.let { paint ->
            mCornerShadeLT?.let { canvas.drawPath(it, paint) }
            mCornerShadeRT?.let { canvas.drawPath(it, paint) }
            mCornerShadeRB?.let { canvas.drawPath(it, paint) }
            mCornerShadeLB?.let { canvas.drawPath(it, paint) }
            // 裁剪两遍 省的圆角比较小的时候会有杂色
            mCornerShadeLT?.let { canvas.drawPath(it, paint) }
            mCornerShadeRT?.let { canvas.drawPath(it, paint) }
            mCornerShadeRB?.let { canvas.drawPath(it, paint) }
            mCornerShadeLB?.let { canvas.drawPath(it, paint) }
        }
        canvas.restore()

        // stroke
        if (mStrokeWidth > 0) {
            mStrokePath?.let { strokePath ->
                mStrokePaint?.let { strokePaint ->
                    canvas.drawPath(strokePath, strokePaint)
                }
            }
        }
    }

    private fun buildVerticalStrokeShader(
        strokeShader: Shader?,
        @ColorInt strokeGradientStartColor: Int?,
        @ColorInt strokeGradientEndColor: Int?,
    ): Shader? {
        if (strokeShader != null) {
            return strokeShader
        }
        if (strokeGradientStartColor == null || strokeGradientEndColor == null) {
            return null
        }
        val viewRect = mViewRect ?: return null
        if (viewRect.height() <= 0f) {
            return null
        }
        return LinearGradient(
            viewRect.left,
            viewRect.top,
            viewRect.left,
            viewRect.bottom,
            strokeGradientStartColor,
            strokeGradientEndColor,
            Shader.TileMode.CLAMP,
        )
    }

    companion object {
        const val COLOR_UNSET = Int.MIN_VALUE
    }
}
