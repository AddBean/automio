// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

//package com.hive.script.views.widgets
//
//import android.content.Context
//import android.graphics.Bitmap
//import android.graphics.Canvas
//import android.graphics.Color
//import android.graphics.Paint
//import android.graphics.Point
//import android.graphics.Rect
//import android.graphics.RectF
//import android.text.Spannable
//import android.text.SpannableString
//import android.text.Spanned
//import android.text.TextPaint
//import android.text.style.ClickableSpan
//import android.text.style.ReplacementSpan
//import android.util.AttributeSet
//import android.view.MotionEvent
//import android.view.View
//import com.hive.script.R
//import com.hive.script.utils.ScriptCommandHelper
//import com.hive.utils.GlobalApp
//import com.hive.utils.extends.dp
//import com.hive.views.widgets.TextDrawableView
//
//open class SCSpanTextView(context: Context?, attrs: AttributeSet?) :
//    TextDrawableView(context, attrs) {
//
//    var touchPoint = Point()
//
//    fun setSpans(
//        content: String,
//        spanTexts: List<String>,
//        spanTextColors: List<Int>,
//        spanIcons: List<Bitmap?>?,
//        onClick: (Int, String) -> Unit
//    ) {
//        val spannableString = SpannableString(content)
//        val backgroundColor = GlobalApp.getColor(com.hive.i8n.R.color.colorAccent)
//        var index = 0
//        spanTexts.forEach { key ->
//            val start = content.indexOf(key)
//            val color = spanTextColors.getOrNull(spanTexts.indexOf(key)) ?: Color.WHITE
//            val end = start + key.length
//            spannableString.setSpan(
//                RadiusBackgroundSpan(
//                    color,
//                    backgroundColor,
//                    4.dp(),
//                    spanIcons?.getOrNull(spanTexts.indexOf(key))
//                ),
//                start,
//                end,
//                Spannable.SPAN_INCLUSIVE_EXCLUSIVE
//            )
//            val currentIndex = index
//            spannableString.setSpan(object : ClickableSpan() {
//                override fun onClick(view: View) {
//                    onClick.invoke(currentIndex, key)
//                }
//
//                override fun updateDrawState(ds: TextPaint) {
//                    ds.isUnderlineText = false
//                    ds.linkColor = Color.TRANSPARENT
//                    ds.bgColor = Color.TRANSPARENT
//                }
//            }, start, end, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
//            index++
//        }
//        this.text = spannableString
//    }
//
//    override fun onTouchEvent(event: MotionEvent?): Boolean {
//        if (event?.action == MotionEvent.ACTION_DOWN) {
//            touchPoint.x = event.x.toInt()
//            touchPoint.y = event.y.toInt()
//        }
//        return super.onTouchEvent(event)
//    }
//
//    class RadiusBackgroundSpan(
//        private val textColor: Int,
//        private val backgroundColor: Int,
//        private val backgroundRadius: Int,
//        private val bitmap: Bitmap? = null,
//    ) : ReplacementSpan() {
//        private var spanWidth = 0
//
//        override fun getSize(
//            paint: Paint,
//            text: CharSequence,
//            start: Int,
//            end: Int,
//            fm: Paint.FontMetricsInt?
//        ): Int {
//            val spanText = text.subSequence(start, end)
//            val spanTextName = ScriptCommandHelper.parseParamsName(spanText.toString())
//            spanWidth = (paint.measureText(
//                spanTextName,
//                0,
//                spanTextName.length
//            ) + 14.dp() + paint.textSize).toInt()
//            return spanWidth
//        }
//
//        override fun draw(
//            canvas: Canvas,
//            text: CharSequence,
//            start: Int,
//            end: Int,
//            x: Float,
//            top: Int,
//            y: Int,
//            bottom: Int,
//            paint: Paint
//        ) {
//            val spanText =
//                ScriptCommandHelper.parseParamsName(text.subSequence(start, end).toString())
//            val originalFakeBoldText = paint.isFakeBoldText
//            val originalColor = paint.color
//            val marginHorizontal = 2.dp()
//            val rect = RectF(x, y + paint.ascent(), x + spanWidth, y + paint.descent())
//            rect.inset(marginHorizontal.toFloat(), 0f)
//            paint.color = backgroundColor
//            paint.isAntiAlias = true
//            canvas.drawRoundRect(
//                rect,
//                backgroundRadius.toFloat(),
//                backgroundRadius.toFloat(),
//                paint
//            )
//            paint.color = textColor
//            canvas.drawText(
//                spanText,
//                0,
//                spanText.length,
//                x + backgroundRadius + marginHorizontal,
//                y.toFloat(),
//                paint
//            )
//            bitmap?.let {
//                paint.color = Color.WHITE
//                Rect(
//                    (x + spanWidth - paint.textSize - 7.dp()).toInt(),
//                    (y + paint.ascent()).toInt(),
//                    (x + spanWidth - paint.textSize - 7.dp() + paint.descent() - paint.ascent()).toInt(),
//                    (y + paint.descent()).toInt()
//                ).apply {
//                    inset(1.dp(), 1.dp())
//                    canvas.drawBitmap(it, Rect(0, 0, it.width, it.height), this, paint)
//                }
//            }
//            paint.color = originalColor
//            paint.isFakeBoldText = originalFakeBoldText
//        }
//    }
//}