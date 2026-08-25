// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.widgets

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Point
import android.graphics.Rect
import android.graphics.RectF
import android.text.Spannable
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.style.ClickableSpan
import android.text.style.ReplacementSpan
import android.view.MotionEvent
import android.view.View
import com.hive.script.R
import com.hive.script.base.params.ScriptParamEnv
import com.hive.script.utils.ScriptCommandHelper
import com.hive.utils.extends.color
import com.hive.utils.extends.dp
import com.hive.utils.extends.dpf

object ScriptSpanHelper {

    fun getSpans(
        content: String, spans: List<ClickSpan>?, onClicked: (ClickSpan, View) -> Unit
    ): SpannableString {
        val spanString = SpannableString(content)
        spans?.forEach {span->
            val start = span.spanStart
            val end = span.spanEnd
            spanString.setSpan(
                span, start, end, Spannable.SPAN_INCLUSIVE_EXCLUSIVE
            )
            spanString.setSpan(object : ClickableSpan() {
                override fun onClick(view: View) {
                    onClicked.invoke(span, view)
                }

                override fun updateDrawState(ds: TextPaint) {
                    ds.isUnderlineText = false
                    ds.linkColor = Color.TRANSPARENT
                    ds.bgColor = Color.TRANSPARENT
                }
            }, start, end, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
        }

        return spanString
    }

    fun onTouchEvent(touchPoint: Point, event: MotionEvent?) {
        if (event?.action == MotionEvent.ACTION_DOWN) {
            touchPoint.x = event.x.toInt()
            touchPoint.y = event.y.toInt()
        }
    }

    /**
     * 解析text
     */
    fun parseSpanText(text: String, onHandleCallback: () -> Unit): ParamsSpan {
        val spans = mutableListOf<ClickSpan>()
        val regex = ScriptCommandHelper.paramRegex
        val matchResults = regex.findAll(text)

        matchResults.forEach {
            val match = it.value
            val group = it.groupValues[1]
            val paramId = ScriptCommandHelper.parseParamsId(match)
            val param = ScriptParamEnv.getParam(paramId)
            val span = ClickSpan(
                spanBgColor = param?.getColor() ?: com.hive.i8n.R.color.colorSpan.color()
            )
            span.spanText = ScriptCommandHelper.paramFormat.format(group)
            span.rawValue = match
            span.spanTextColor = com.hive.i8n.R.color.textColorPrimary.color()
            span.onHandleCallback = {
                onHandleCallback.invoke()
            }
            span.spanStart = it.range.first
            span.spanEnd = it.range.last + 1
            spans.add(span)

        }
        return ParamsSpan(text, spans)
    }


    open class RadiusBackgroundSpan(
        private val position: Int,
        private val textColor: Int,
        private val bgColor: Int,
        private val bgRadius: Int,
        private val bmp: Bitmap? = null,
    ) : ReplacementSpan() {
        private var spanSize = 0

        override fun getSize(
            textPaint: Paint, text: CharSequence, start: Int, end: Int, fm: Paint.FontMetricsInt?
        ): Int {

            val copyText = text.toString()
            //默认大小
            val spanText =
                ScriptCommandHelper.parseParamsName(copyText.subSequence(start, end).toString())
            spanSize = textPaint.measureText(
                spanText, 0, spanText.length
            ).toInt()
            spanSize += if (bmp != null) {
                (2.2 * textPaint.textSize).toInt()
            } else {
                textPaint.textSize.toInt()
            }
            return spanSize
        }


        override fun draw(
            canvas: Canvas,
            src: CharSequence,
            start: Int,
            end: Int,
            x: Float,
            top: Int,
            y: Int,
            bottom: Int,
            paintOld: Paint
        ) {
            canvas.save()
            val paint: Paint = Paint().apply {
                isAntiAlias = true
                color = bgColor
            }
            paint.textSize = paintOld.textSize
            val text = ScriptCommandHelper.parseParamsName(src.subSequence(start, end).toString())
            val textSize = if (bmp != null) paint.textSize else paint.textSize * 0.93f
            paint.textSize = textSize
            val oval = RectF(
                x,
                y + paint.ascent() - textSize / 10f,
                x + spanSize,
                y + paint.descent() + textSize / 10f
            )
            oval.inset(2.dpf(), 0f)
            paint.color = bgColor //设置背景颜色
            paint.isAntiAlias = true // 设置画笔的锯齿效果
            //设置文字背景矩形，x为span其实左上角相对整个TextView的x值，y为span左上角相对整个View的y值。paint.ascent()获得文字上边缘，paint.descent()获得文字下边缘
            canvas.drawRoundRect(oval, bgRadius.toFloat(), bgRadius.toFloat(), paint)
            paint.color = textColor //恢复画笔的文字颜色
            var textX = 0f
            var bmpX = 0f
            if (position == 0) {
                textX = (1.2 * paint.textSize).toInt() + x + bgRadius
                bmpX = x + 3.dp()
            } else {
                textX = x + bgRadius + 3.dp()
                bmpX = x + spanSize - 1.6f * paint.textSize
            }

            if (bmp != null) {
                canvas.drawText(text, 0, text.length, textX, y.toFloat(), paint)
                canvas.drawBitmap(
                    bmp, Rect(0, 0, bmp.width, bmp.height), Rect(
                        bmpX.toInt(),
                        (y + paint.ascent()).toInt(),
                        (bmpX + paint.descent() - paint.ascent()).toInt(),
                        (y + paint.descent()).toInt()
                    ).apply {
                        inset(1.dp(), 1.dp())
                    }, paint
                )
            } else {
                val textW = paint.measureText(text, 0, text.length)
                canvas.drawText(
                    text, 0, text.length, x + (spanSize - textW) / 2, y.toFloat(), paint
                )
            }
            canvas.restore()
        }
    }

    class ParamsSpan(
        var content: String? = null, var spans: List<ClickSpan>? = null
    )

    data class ClickSpan(
        var rawValue: String? = null,
        var spanText: String? = null,
        var spanStart: Int = 0,
        var spanEnd: Int = 0,
        var spanIcon: Bitmap? = null,
        var spanPosition: Int = 1,//0左 1右
        var spanType: ClickSpanType = ClickSpanType.ParamSelector,
        var spanBgColor: Int = com.hive.i8n.R.color.colorSpan.color(),
        var spanTextColor: Int = com.hive.i8n.R.color.textColorPrimary.color(),
        var spanExtra: Any? = null,
        var onHandleCallback: ((ClickSpan) -> Unit)? = null
    ) : RadiusBackgroundSpan(
        spanPosition,
        spanTextColor,
        spanBgColor,
        4.dp(),
        spanIcon,
    )

    enum class ClickSpanType {
        Input, Selector, ImageSelector, ColorSelector, ParamSelector, PermissionMultiSelector
    }
}